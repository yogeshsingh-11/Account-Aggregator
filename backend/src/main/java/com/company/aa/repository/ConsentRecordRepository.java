package com.company.aa.repository;

import com.company.aa.entity.ConsentRecord;
import com.company.aa.entity.ConsentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, UUID> {
    Optional<ConsentRecord> findByConsentHandle(String consentHandle);
    Optional<ConsentRecord> findByConsentId(String consentId);
    Optional<ConsentRecord> findByIdempotencyKey(String idempotencyKey);
    List<ConsentRecord> findByCustomerRefOrderByCreatedAtDesc(String customerRef);
    List<ConsentRecord> findByStatus(ConsentStatus status);
}
