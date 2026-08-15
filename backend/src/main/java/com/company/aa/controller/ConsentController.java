package com.company.aa.controller;

import com.company.aa.dto.ConsentResponse;
import com.company.aa.dto.ConsentStatusResponse;
import com.company.aa.dto.CreateConsentRequest;
import com.company.aa.entity.ConsentRecord;
import com.company.aa.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Consent lifecycle: create -> present to user -> status polling.
 * These endpoints are called directly by the Angular UI, so they are intentionally NOT under
 * /internal/ (the internal API key must never be shipped to a browser). In production, replace
 * this open path with proper end-user auth (session/JWT tied to a logged-in customer) so a
 * caller can only create/view consents for themselves — see SecurityConfig for where to wire
 * that in. The separate GET /internal/consents endpoint below remains for FIU-service-to-service
 * lookups and stays API-key gated.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Consent", description = "Account Aggregator consent lifecycle")
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @Operation(summary = "Create a new consent request and register it with the AA (Digio)")
    @PostMapping("/consents")
    public ResponseEntity<ConsentResponse> createConsent(
            @Valid @RequestBody CreateConsentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Requested-By", required = false) String requestedBy) {
        ConsentResponse response = consentService.createConsent(request, idempotencyKey, requestedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get current status of a consent (polls Digio and syncs local state)")
    @GetMapping("/consents/{consentRecordId}/status")
    public ResponseEntity<ConsentStatusResponse> getStatus(@PathVariable UUID consentRecordId) {
        return ResponseEntity.ok(consentService.refreshConsentStatus(consentRecordId));
    }

    @Operation(summary = "List consent records for a customer (internal use)")
    @GetMapping("/internal/consents")
    public ResponseEntity<List<ConsentRecord>> listByCustomer(@RequestParam String customerRef) {
        return ResponseEntity.ok(consentService.findByCustomerRef(customerRef));
    }
}
