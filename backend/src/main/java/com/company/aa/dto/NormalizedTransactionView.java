package com.company.aa.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record NormalizedTransactionView(
        UUID transactionId,
        String txnType,
        BigDecimal amount,
        BigDecimal balanceAfterTxn,
        Instant txnTimestamp,
        LocalDate valueDate,
        String narration,
        String mode
) {
}
