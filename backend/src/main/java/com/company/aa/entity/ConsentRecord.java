package com.company.aa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consent_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentRecord {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "customer_ref", nullable = false, length = 128)
    private String customerRef;

    @Column(name = "consent_handle", unique = true, length = 128)
    private String consentHandle;

    @Column(name = "consent_id", unique = true, length = 128)
    private String consentId;

    @Column(name = "template_id", nullable = false, length = 128)
    private String templateId;

    @Column(name = "purpose_code", nullable = false, length = 64)
    private String purposeCode;

    @Column(name = "fi_types", nullable = false, length = 256)
    private String fiTypes;

    @Column(name = "consent_start")
    private Instant consentStart;

    @Column(name = "consent_expiry")
    private Instant consentExpiry;

    @Column(name = "data_range_from")
    private Instant dataRangeFrom;

    @Column(name = "data_range_to")
    private Instant dataRangeTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private ConsentStatus status = ConsentStatus.PENDING;

    @Column(name = "idempotency_key", unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "requested_by", length = 128)
    private String requestedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
