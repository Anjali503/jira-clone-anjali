package com.example.jira.service;

import com.example.jira.model.AuditLog;
import com.example.jira.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String action, String userId, String entityId, String entityType, 
                    Map<String, Object> previousValues, Map<String, Object> newValues) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setUserId(userId);
        log.setEntityId(entityId);
        log.setEntityType(entityType);
        log.setPreviousValues(previousValues);
        log.setNewValues(newValues);
        log.setTimestamp(Instant.now());
        auditLogRepository.save(log);
    }
}
