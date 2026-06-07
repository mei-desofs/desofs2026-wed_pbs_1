package com.ghostreport.service;

import com.ghostreport.model.AuditLog;
import com.ghostreport.repository.AuditLogRepository;
import com.ghostreport.security.CorrelationId;
import com.ghostreport.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final SecurityLogSanitizer sanitizer;

    public AuditLogService(AuditLogRepository auditLogRepository, SecurityLogSanitizer sanitizer) {
        this.auditLogRepository = auditLogRepository;
        this.sanitizer = sanitizer;
    }

    public void log(String action, String targetType, Long targetId, String details) {
        AuditLog auditLog = new AuditLog();

        auditLog.setTimestamp(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        auditLog.setCorrelationId(CorrelationId.current());
        auditLog.setActor(getActor());
        auditLog.setAction(sanitizer.sanitize(action));
        auditLog.setTargetType(sanitizer.sanitize(targetType));
        auditLog.setTargetId(targetId);
        auditLog.setDetails(sanitizer.sanitize(details));
        auditLog.setIntegrityHash(integrityHash(auditLog));

        auditLogRepository.save(auditLog);
    }

    private String getActor() {
        try {
            return SecurityUtils.getCurrentUsername();
        } catch (Exception e) {
            return "anonymous";
        }
    }

    private String integrityHash(AuditLog auditLog) {
        String payload = String.join("|",
                nullToEmpty(auditLog.getTimestamp()),
                nullToEmpty(auditLog.getCorrelationId()),
                nullToEmpty(auditLog.getActor()),
                nullToEmpty(auditLog.getAction()),
                nullToEmpty(auditLog.getTargetType()),
                nullToEmpty(auditLog.getTargetId()),
                nullToEmpty(auditLog.getDetails())
        );
        return sha256(payload);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Could not calculate audit integrity hash", e);
        }
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : value.toString();
    }
}
