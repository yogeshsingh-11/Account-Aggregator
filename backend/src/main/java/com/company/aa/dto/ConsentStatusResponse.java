package com.company.aa.dto;

import com.company.aa.entity.ConsentStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ConsentStatusResponse(
        UUID consentRecordId,
        String consentHandle,
        String consentId,
        ConsentStatus status,
        Instant consentExpiry,
        Instant updatedAt
) {
}
