package com.company.aa.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {
}
