package com.company.aa.controller;

import com.company.aa.config.DigioProperties;
import com.company.aa.dto.digio.DigioWebhookNotification;
import com.company.aa.service.AuditService;
import com.company.aa.service.ConsentService;
import com.company.aa.service.FiDataFetchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Receives Digio's asynchronous notifications: consent status changes and FI-data-ready events.
 * Signature verification (X-Digio-Signature, HMAC-SHA256 over the raw body) protects this
 * endpoint since it's intentionally unauthenticated at the network layer (Digio calls it directly).
 * Confirm the exact header name / signing scheme against Digio's webhook documentation and adjust
 * verifySignature() accordingly — if Digio doesn't sign sandbox webhooks, this degrades to a
 * best-effort check (still logs + audits every call either way).
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Inbound async notifications from Digio")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private static final String SIGNATURE_HEADER = "X-Digio-Signature";

    private final ConsentService consentService;
    private final FiDataFetchService fiDataFetchService;
    private final AuditService auditService;
    private final DigioProperties digioProperties;

    public WebhookController(ConsentService consentService,
                              FiDataFetchService fiDataFetchService,
                              AuditService auditService,
                              DigioProperties digioProperties) {
        this.consentService = consentService;
        this.fiDataFetchService = fiDataFetchService;
        this.auditService = auditService;
        this.digioProperties = digioProperties;
    }

    @Operation(summary = "Digio webhook receiver for consent status + FI data readiness events")
    @PostMapping("/digio")
    public ResponseEntity<Void> handleDigioWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature) {

        if (!verifySignature(rawBody, signature)) {
            log.warn("Rejected Digio webhook with invalid signature");
            auditService.record("WEBHOOK_REJECTED", "WEBHOOK", null, "DIGIO", "Invalid signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        DigioWebhookNotification notification = parse(rawBody);
        auditService.record("WEBHOOK_RECEIVED", "WEBHOOK", notification.consentHandle(), "DIGIO",
                "event_type=" + notification.eventType());

        switch (notification.eventType() == null ? "" : notification.eventType()) {
            case "CONSENT_STATUS_UPDATE" -> consentService.applyWebhookStatusUpdate(
                    notification.consentHandle(), notification.consentId(),
                    notification.status(), null);
            case "FI_DATA_READY" -> fiDataFetchService.markReady(notification.sessionId());
            default -> log.info("Unhandled Digio webhook event_type={}", notification.eventType());
        }

        return ResponseEntity.ok().build();
    }

    private DigioWebhookNotification parse(String rawBody) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .findAndRegisterModules()
                    .readValue(rawBody, DigioWebhookNotification.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed webhook payload", e);
        }
    }

    private boolean verifySignature(String rawBody, String signature) {
        String secret = digioProperties.webhookSecret();
        if (secret == null || secret.isBlank()) {
            // No secret configured (e.g. early sandbox testing) — accept but this MUST be
            // configured before go-live. Logged loudly so it isn't missed in review.
            log.warn("DIGIO_WEBHOOK_SECRET not configured — webhook signature is NOT being verified");
            return true;
        }
        if (signature == null || signature.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);
            return computedHex.equalsIgnoreCase(signature.trim());
        } catch (Exception e) {
            log.error("Signature verification error", e);
            return false;
        }
    }
}
