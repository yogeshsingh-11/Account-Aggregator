package com.company.aa.dto.digio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DigioConsentStatusResponse(
        @JsonProperty("consent_handle") String consentHandle,
        @JsonProperty("consent_id") String consentId,
        @JsonProperty("status") String status,       // PENDING / ACTIVE / REJECTED / EXPIRED / REVOKED
        @JsonProperty("consent_expiry") String consentExpiry
) {
}
