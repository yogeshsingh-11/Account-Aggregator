package com.company.aa.service;

import com.company.aa.entity.AuditLog;
import com.company.aa.repository.AuditLogRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(String eventType, String entityType, String entityId, String actor, String detail) {
        AuditLog entry = AuditLog.builder()
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .actor(actor)
                .correlationId(MDC.get("correlationId"))
                .detail(detail)
                .build();
        auditLogRepository.save(entry);
    }
}
