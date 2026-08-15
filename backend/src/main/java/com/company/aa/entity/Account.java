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
import java.util.UUID;

@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fi_data_request_id", nullable = false)
    private FiDataRequest fiDataRequest;

    @Column(name = "fip_id", nullable = false, length = 128)
    private String fipId;

    @Column(name = "fi_type", nullable = false, length = 64)
    private String fiType;

    @Column(name = "masked_account_number", nullable = false, length = 64)
    private String maskedAccountNumber;

    @Column(name = "account_type", length = 64)
    private String accountType;

    @Column(name = "ifsc", length = 16)
    private String ifsc;

    @Column(name = "currency", length = 8)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "current_balance", precision = 18, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "balance_as_of")
    private Instant balanceAsOf;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
