package com.company.aa.dto;

import com.company.aa.entity.ConsentStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ConsentResponse(
        UUID consentRecordId,
        String consentHandle,
        String redirectUrl,       // URL/deeplink the frontend uses to present the consent approval screen to the user
        ConsentStatus status,
        Instant createdAt
) {
}
