package com.company.aa.dto.digio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DigioFiRequestResponse(
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("status") String status
) {
}
