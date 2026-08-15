package com.company.aa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "txn_ref_id", length = 128)
    private String txnRefId;

    @Column(name = "txn_type", nullable = false, length = 16)
    private String txnType; // DEBIT / CREDIT

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after_txn", precision = 18, scale = 2)
    private BigDecimal balanceAfterTxn;

    @Column(name = "txn_timestamp", nullable = false)
    private Instant txnTimestamp;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(name = "narration", length = 512)
    private String narration;

    @Column(name = "mode", length = 32)
    private String mode;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
