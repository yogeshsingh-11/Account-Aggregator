package com.company.aa.dto.digio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DigioCreateConsentResponse(
        @JsonProperty("consent_handle") String consentHandle,
        @JsonProperty("url") String url,           // link/deeplink to present to the customer
        @JsonProperty("status") String status
) {
}
