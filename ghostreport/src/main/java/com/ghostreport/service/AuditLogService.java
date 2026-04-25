package com.ghostreport.service;

import com.ghostreport.model.AuditLog;
import com.ghostreport.repository.AuditLogRepository;
import com.ghostreport.security.SecurityUtils;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String action, String targetType, Long targetId, String details) {
        AuditLog auditLog = new AuditLog();

        auditLog.setActor(getActor());
        auditLog.setAction(action);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        auditLog.setDetails(sanitize(details));

        auditLogRepository.save(auditLog);
    }

    private String getActor() {
        try {
            return SecurityUtils.getCurrentUsername();
        } catch (Exception e) {
            return "anonymous";
        }
    }

    private String sanitize(String details) {
        if (details == null) {
            return null;
        }

        return details
                .replaceAll("[\\r\\n]", " ")
                .replaceAll("[\\x00-\\x1F\\x7F]", "")
                .trim();
    }
}