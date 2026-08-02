package com.mindhaven.service;

import com.mindhaven.common.RequestIdHolder;
import com.mindhaven.entity.AuditLog;
import com.mindhaven.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(Long actorUserId, String action, String resourceType, String resourceId, String result,
                       String reason, String metadataJson) {
        AuditLog log = new AuditLog();
        log.setActorUserId(actorUserId);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setResult(result == null ? "SUCCESS" : result);
        log.setReason(reason);
        log.setRequestId(RequestIdHolder.get());
        log.setMetadataJson(metadataJson);
        try {
            auditLogRepository.save(log);
        } catch (Exception ignored) {
            // 审计失败不应阻断主流程
        }
    }
}
