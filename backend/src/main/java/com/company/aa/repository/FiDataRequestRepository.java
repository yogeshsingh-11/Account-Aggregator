package com.company.aa.repository;

import com.company.aa.entity.FiDataRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FiDataRequestRepository extends JpaRepository<FiDataRequest, UUID> {
    Optional<FiDataRequest> findBySessionId(String sessionId);
    Optional<FiDataRequest> findByIdempotencyKey(String idempotencyKey);
    List<FiDataRequest> findByConsentRecordIdOrderByCreatedAtDesc(UUID consentRecordId);
}
