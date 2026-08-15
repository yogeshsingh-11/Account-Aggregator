package com.company.aa.service;

import com.company.aa.dto.FiDataFetchResponse;
import com.company.aa.dto.NormalizedAccountView;
import com.company.aa.dto.digio.DigioFiDataPayload;
import com.company.aa.dto.digio.DigioFiRequestResponse;
import com.company.aa.entity.Account;
import com.company.aa.entity.ConsentRecord;
import com.company.aa.entity.ConsentStatus;
import com.company.aa.entity.FiDataRequest;
import com.company.aa.entity.FiRequestStatus;
import com.company.aa.exception.InvalidConsentStateException;
import com.company.aa.exception.ResourceNotFoundException;
import com.company.aa.repository.AccountRepository;
import com.company.aa.repository.FiDataRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class FiDataFetchService {

    private static final Logger log = LoggerFactory.getLogger(FiDataFetchService.class);

    private final FiDataRequestRepository fiDataRequestRepository;
    private final AccountRepository accountRepository;
    private final ConsentService consentService;
    private final DigioClientService digioClientService;
    private final DataNormalizationService normalizationService;
    private final AuditService auditService;

    public FiDataFetchService(FiDataRequestRepository fiDataRequestRepository,
                               AccountRepository accountRepository,
                               ConsentService consentService,
                               DigioClientService digioClientService,
                               DataNormalizationService normalizationService,
                               AuditService auditService) {
        this.fiDataRequestRepository = fiDataRequestRepository;
        this.accountRepository = accountRepository;
        this.consentService = consentService;
        this.digioClientService = digioClientService;
        this.normalizationService = normalizationService;
        this.auditService = auditService;
    }

    /** Initiates an FI data session with Digio for an ACTIVE consent. Idempotent on idempotencyKey. */
    @Transactional
    public FiDataFetchResponse initiateFetch(UUID consentRecordId, String idempotencyKey, String requestedBy) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = fiDataRequestRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent replay for FI data fetch, key={}", idempotencyKey);
                return toResponse(existing.get());
            }
        }

        ConsentRecord consent = consentService.getOrThrow(consentRecordId);
        if (consent.getStatus() != ConsentStatus.ACTIVE) {
            throw new InvalidConsentStateException(
                    "Consent must be ACTIVE to fetch data, current status: " + consent.getStatus());
        }

        FiDataRequest fiRequest = FiDataRequest.builder()
                .consentRecord(consent)
                .status(FiRequestStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();
        fiRequest = fiDataRequestRepository.save(fiRequest);

        DigioFiRequestResponse digioResponse = digioClientService.initiateFiDataFetch(consent.getConsentId());
        fiRequest.setSessionId(digioResponse.sessionId());
        fiRequest = fiDataRequestRepository.save(fiRequest);

        auditService.record("FI_FETCH_INITIATED", "FI_DATA_REQUEST", fiRequest.getId().toString(), requestedBy,
                "FI data session initiated: " + digioResponse.sessionId());

        return toResponse(fiRequest);
    }

    /**
     * Called once Digio signals FI_DATA_READY (via webhook, or a polling fallback job).
     * Pulls the payload, normalizes it, and marks the request DELIVERED.
     */
    @Transactional
    public List<Account> fetchAndNormalize(String sessionId) {
        FiDataRequest fiRequest = fiDataRequestRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("No FI data request for session: " + sessionId));

        try {
            DigioFiDataPayload payload = digioClientService.fetchFiData(sessionId);
            List<Account> accounts = normalizationService.normalizeAndPersist(fiRequest, payload);

            fiRequest.setStatus(FiRequestStatus.DELIVERED);
            fiRequest.setFetchedAt(Instant.now());
            fiDataRequestRepository.save(fiRequest);

            auditService.record("FI_DATA_RECEIVED", "FI_DATA_REQUEST", fiRequest.getId().toString(), "SYSTEM",
                    "Normalized " + accounts.size() + " account(s)");
            return accounts;
        } catch (Exception ex) {
            fiRequest.setStatus(FiRequestStatus.FAILED);
            fiRequest.setErrorReason(truncate(ex.getMessage(), 500));
            fiDataRequestRepository.save(fiRequest);
            auditService.record("FI_DATA_FETCH_FAILED", "FI_DATA_REQUEST", fiRequest.getId().toString(), "SYSTEM",
                    "Fetch failed: " + ex.getMessage());
            throw ex;
        }
    }

    @Transactional
    public void markReady(String sessionId) {
        FiDataRequest fiRequest = fiDataRequestRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("No FI data request for session: " + sessionId));
        fiRequest.setStatus(FiRequestStatus.READY);
        fiDataRequestRepository.save(fiRequest);
    }

    @Transactional(readOnly = true)
    public List<NormalizedAccountView> getAccountsForRequest(UUID fiDataRequestId) {
        return accountRepository.findByFiDataRequestId(fiDataRequestId).stream()
                .map(a -> NormalizedAccountView.builder()
                        .accountId(a.getId())
                        .fipId(a.getFipId())
                        .fiType(a.getFiType())
                        .maskedAccountNumber(a.getMaskedAccountNumber())
                        .accountType(a.getAccountType())
                        .currentBalance(a.getCurrentBalance())
                        .balanceAsOf(a.getBalanceAsOf())
                        .status(a.getStatus())
                        .build())
                .toList();
    }

    private FiDataFetchResponse toResponse(FiDataRequest r) {
        return FiDataFetchResponse.builder()
                .fiDataRequestId(r.getId())
                .sessionId(r.getSessionId())
                .status(r.getStatus())
                .requestedAt(r.getRequestedAt())
                .build();
    }

    private String truncate(String v, int max) {
        if (v == null) return null;
        return v.length() <= max ? v : v.substring(0, max);
    }
}
