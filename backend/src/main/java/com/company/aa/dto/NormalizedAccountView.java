package com.company.aa.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record NormalizedAccountView(
        UUID accountId,
        String fipId,
        String fiType,
        String maskedAccountNumber,
        String accountType,
        BigDecimal currentBalance,
        Instant balanceAsOf,
        String status
) {
}
