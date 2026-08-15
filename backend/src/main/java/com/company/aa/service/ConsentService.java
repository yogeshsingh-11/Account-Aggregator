package com.company.aa.service;

import com.company.aa.config.DigioProperties;
import com.company.aa.dto.ConsentResponse;
import com.company.aa.dto.ConsentStatusResponse;
import com.company.aa.dto.CreateConsentRequest;
import com.company.aa.dto.digio.DigioConsentStatusResponse;
import com.company.aa.dto.digio.DigioCreateConsentRequest;
import com.company.aa.dto.digio.DigioCreateConsentResponse;
import com.company.aa.entity.ConsentRecord;
import com.company.aa.entity.ConsentStatus;
import com.company.aa.exception.InvalidConsentStateException;
import com.company.aa.exception.ResourceNotFoundException;
import com.company.aa.repository.ConsentRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class ConsentService {

    private static final Logger log = LoggerFactory.getLogger(ConsentService.class);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    private final ConsentRecordRepository consentRecordRepository;
    private final DigioClientService digioClientService;
    private final DigioProperties digioProperties;
    private final AuditService auditService;

    public ConsentService(ConsentRecordRepository consentRecordRepository,
                           DigioClientService digioClientService,
                           DigioProperties digioProperties,
                           AuditService auditService) {
        this.consentRecordRepository = consentRecordRepository;
        this.digioClientService = digioClientService;
        this.digioProperties = digioProperties;
        this.auditService = auditService;
    }

    /**
     * Creates a consent record locally (PENDING) and registers it with Digio.
     * Idempotent on the caller-supplied idempotencyKey: replays return the original record.
     */
    @Transactional
    public ConsentResponse createConsent(CreateConsentRequest request, String idempotencyKey, String requestedBy) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = consentRecordRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent replay for consent creation, key={}", idempotencyKey);
                return toResponse(existing.get(), null);
            }
        }

        ConsentRecord record = ConsentRecord.builder()
                .customerRef(request.customerRef())
                .templateId(digioProperties.templateId())
                .purposeCode(request.purposeCode())
                .fiTypes(String.join(",", request.fiTypes()))
                .dataRangeFrom(request.dataRangeFrom())
                .dataRangeTo(request.dataRangeTo())
                .consentExpiry(request.consentExpiry())
                .status(ConsentStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .requestedBy(requestedBy)
                .build();
        record = consentRecordRepository.save(record);

        DigioCreateConsentRequest digioRequest = DigioCreateConsentRequest.builder()
                .customerIdentifier(request.customerRef())
                .customerIdType("MOBILE")
                .templateName(digioProperties.templateId())
                .purposeCode(request.purposeCode())
                .fiTypes(request.fiTypes())
                .dataRangeFrom(ISO.format(request.dataRangeFrom()))
                .dataRangeTo(ISO.format(request.dataRangeTo()))
                .consentExpiry(request.consentExpiry() != null ? ISO.format(request.consentExpiry()) : null)
                .build();

        DigioCreateConsentResponse digioResponse = digioClientService.createConsent(digioRequest);

        record.setConsentHandle(digioResponse.consentHandle());
        if (digioResponse.status() != null) {
            record.setStatus(mapStatus(digioResponse.status()));
        }
        record = consentRecordRepository.save(record);

        auditService.record("CONSENT_CREATED", "CONSENT", record.getId().toString(), requestedBy,
                "Consent created for customerRef=" + maskRef(request.customerRef()));

        return toResponse(record, digioResponse.url());
    }

    /**
     * Polls Digio for the latest status and syncs it locally. Used both by an explicit
     * status-check endpoint and as a fallback when webhooks are delayed/missed.
     */
    @Transactional
    public ConsentStatusResponse refreshConsentStatus(UUID consentRecordId) {
        ConsentRecord record = consentRecordRepository.findById(consentRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Consent record not found: " + consentRecordId));

        if (record.getConsentHandle() == null) {
            throw new InvalidConsentStateException("Consent has no handle yet, cannot check status");
        }

        DigioConsentStatusResponse status = digioClientService.getConsentStatus(record.getConsentHandle());
        applyStatusUpdate(record, status.status(), status.consentId(), status.consentExpiry());
        return toStatusResponse(record);
    }

    @Transactional
    public void applyWebhookStatusUpdate(String consentHandle, String consentId, String status, String consentExpiry) {
        ConsentRecord record = consentRecordRepository.findByConsentHandle(consentHandle)
                .orElseThrow(() -> new ResourceNotFoundException("Consent not found for handle: " + consentHandle));
        applyStatusUpdate(record, status, consentId, consentExpiry);
    }

    private void applyStatusUpdate(ConsentRecord record, String rawStatus, String consentId, String consentExpiry) {
        ConsentStatus newStatus = mapStatus(rawStatus);
        ConsentStatus previous = record.getStatus();
        record.setStatus(newStatus);
        if (consentId != null && !consentId.isBlank()) {
            record.setConsentId(consentId);
        }
        if (consentExpiry != null && !consentExpiry.isBlank()) {
            record.setConsentExpiry(Instant.parse(consentExpiry));
        }
        consentRecordRepository.save(record);

        if (previous != newStatus) {
            auditService.record("CONSENT_STATUS_UPDATED", "CONSENT", record.getId().toString(), "SYSTEM",
                    "Status changed " + previous + " -> " + newStatus);
        }
    }

    @Transactional(readOnly = true)
    public List<ConsentRecord> findByCustomerRef(String customerRef) {
        return consentRecordRepository.findByCustomerRefOrderByCreatedAtDesc(customerRef);
    }

    @Transactional(readOnly = true)
    public ConsentRecord getOrThrow(UUID consentRecordId) {
        return consentRecordRepository.findById(consentRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Consent record not found: " + consentRecordId));
    }

    private ConsentStatus mapStatus(String digioStatus) {
        if (digioStatus == null) return ConsentStatus.PENDING;
        return switch (digioStatus.trim().toUpperCase()) {
            case "ACTIVE", "APPROVED" -> ConsentStatus.ACTIVE;
            case "REJECTED", "DENIED" -> ConsentStatus.REJECTED;
            case "EXPIRED" -> ConsentStatus.EXPIRED;
            case "REVOKED" -> ConsentStatus.REVOKED;
            case "FAILED" -> ConsentStatus.FAILED;
            default -> ConsentStatus.PENDING;
        };
    }

    private ConsentResponse toResponse(ConsentRecord record, String redirectUrl) {
        return ConsentResponse.builder()
                .consentRecordId(record.getId())
                .consentHandle(record.getConsentHandle())
                .redirectUrl(redirectUrl)
                .status(record.getStatus())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private ConsentStatusResponse toStatusResponse(ConsentRecord record) {
        return ConsentStatusResponse.builder()
                .consentRecordId(record.getId())
                .consentHandle(record.getConsentHandle())
                .consentId(record.getConsentId())
                .status(record.getStatus())
                .consentExpiry(record.getConsentExpiry())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    /** Never write raw customer identifiers into audit/log text — mask to last 4 chars. */
    private String maskRef(String ref) {
        if (ref == null || ref.length() <= 4) return "****";
        return "****" + ref.substring(ref.length() - 4);
    }
}
