package com.company.aa.dto.digio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Inbound async notification from Digio — consent status changes and FI data readiness.
 * event_type examples: CONSENT_STATUS_UPDATE, FI_DATA_READY
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DigioWebhookNotification(
        @JsonProperty("event_type") String eventType,
        @JsonProperty("consent_handle") String consentHandle,
        @JsonProperty("consent_id") String consentId,
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("status") String status,
        @JsonProperty("timestamp") String timestamp
) {
}
