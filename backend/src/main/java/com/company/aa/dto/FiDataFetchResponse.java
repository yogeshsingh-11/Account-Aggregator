package com.company.aa.dto;

import com.company.aa.entity.FiRequestStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record FiDataFetchResponse(
        UUID fiDataRequestId,
        String sessionId,
        FiRequestStatus status,
        Instant requestedAt
) {
}
