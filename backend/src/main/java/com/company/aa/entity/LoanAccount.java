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
@Table(name = "loan_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanAccount {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "loan_type", length = 64)
    private String loanType;

    @Column(name = "sanctioned_amount", precision = 18, scale = 2)
    private BigDecimal sanctionedAmount;

    @Column(name = "outstanding_principal", precision = 18, scale = 2)
    private BigDecimal outstandingPrincipal;

    @Column(name = "interest_rate", precision = 6, scale = 3)
    private BigDecimal interestRate;

    @Column(name = "tenure_months")
    private Integer tenureMonths;

    @Column(name = "next_emi_amount", precision = 18, scale = 2)
    private BigDecimal nextEmiAmount;

    @Column(name = "next_emi_date")
    private LocalDate nextEmiDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
