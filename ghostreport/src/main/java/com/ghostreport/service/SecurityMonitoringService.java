package com.ghostreport.service;

import com.ghostreport.model.SecurityAlert;
import com.ghostreport.repository.SecurityAlertRepository;
import com.ghostreport.security.CorrelationId;
import com.ghostreport.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SecurityMonitoringService {

    private final SecurityAlertRepository securityAlertRepository;
    private final SecurityLogSanitizer sanitizer;

    private final Map<String, AttemptCounter> counters = new ConcurrentHashMap<>();

    private static final int MAX_TRACKING_FAILURES = 5;
    private static final int MAX_UPLOAD_FAILURES = 3;
    private static final long WINDOW_MILLIS = 60_000;

    public SecurityMonitoringService(
            SecurityAlertRepository securityAlertRepository,
            SecurityLogSanitizer sanitizer
    ) {
        this.securityAlertRepository = securityAlertRepository;
        this.sanitizer = sanitizer;
    }

    public void recordFailedTrackingCode(Long reportId) {
        String key = "TRACKING_FAIL:" + reportId;

        int attempts = increment(key);

        if (attempts >= MAX_TRACKING_FAILURES) {
            createAlert(
                    "TRACKING_CODE_ENUMERATION",
                    "HIGH",
                    "REPORT",
                    reportId,
                    "Multiple invalid tracking code attempts detected"
            );
            reset(key);
        }
    }

    public void recordRejectedUpload(Long reportId, String reason) {
        String key = "UPLOAD_REJECTED:" + reportId;

        int attempts = increment(key);

        if (attempts >= MAX_UPLOAD_FAILURES) {
            createAlert(
                    "SUSPICIOUS_UPLOAD_ACTIVITY",
                    "HIGH",
                    "REPORT",
                    reportId,
                    "Multiple rejected uploads detected: " + reason
            );
            reset(key);
        }
    }

    public void recordMalwareUploadRejected(Long reportId) {
        createAlert(
                "MALWARE_UPLOAD_REJECTED",
                "CRITICAL",
                "REPORT",
                reportId,
                "Uploaded file rejected by malware scanner and quarantined"
        );
    }

    public void recordPathTraversalAttempt(String input) {
        createAlert(
                "PATH_TRAVERSAL_ATTEMPT",
                "CRITICAL",
                "FILE_SYSTEM",
                null,
                "Possible path traversal input detected"
        );
    }

    public void recordBackupPathTraversalAttempt(String input) {
        createAlert(
                "BACKUP_PATH_TRAVERSAL_ATTEMPT",
                "CRITICAL",
                "BACKUP",
                null,
                "Possible path traversal in backup filename"
        );
    }

    public void recordMissingBackupDownload(String filename) {
        createAlert(
                "BACKUP_DOWNLOAD_NOT_FOUND",
                "HIGH",
                "BACKUP",
                null,
                "Attempt to download missing backup"
        );
    }

    public void recordBackupIntegrityFailure(String filename) {
        createAlert(
                "BACKUP_INTEGRITY_FAILURE",
                "CRITICAL",
                "BACKUP",
                null,
                "Backup integrity validation failed"
        );
    }

    public void recordUnauthorizedBackupAccess(String path) {
        createAlert(
                "BACKUP_UNAUTHORIZED_ATTEMPT",
                "HIGH",
                "BACKUP",
                null,
                "Unauthorized attempt to access backup endpoint"
        );
    }

    public void recordUnauthorizedAnalystAccess(Long reportId) {
        createAlert(
                "ANALYST_OWNERSHIP_VIOLATION",
                "HIGH",
                "REPORT",
                reportId,
                "Analyst attempted to access a report without ownership"
        );
    }

    public void recordBruteForceLoginAttempt() {
        createAlert(
                "BRUTE_FORCE_LOGIN_ATTEMPT",
                "HIGH",
                "AUTHENTICATION",
                null,
                "Repeated failed login attempts detected"
        );
    }

    public void recordInvalidJwt(String path) {
        createAlert(
                "INVALID_JWT_TOKEN",
                "MEDIUM",
                "AUTHENTICATION",
                null,
                "Invalid or expired JWT presented to protected endpoint: " + sanitizePath(path)
        );
    }

    public void recordForbiddenAccess(String path) {
        createAlert(
                "FORBIDDEN_ACCESS_ATTEMPT",
                "MEDIUM",
                "HTTP_ENDPOINT",
                null,
                "Forbidden access attempt to endpoint: " + sanitizePath(path)
        );
    }

    public void recordUnexpectedError(String errorType) {
        createAlert(
                "UNEXPECTED_ERROR",
                "HIGH",
                "APPLICATION",
                null,
                "Unexpected application error: " + sanitizer.sanitize(errorType)
        );
    }

    public void createAlert(String alertType, String severity, String targetType, Long targetId, String description) {
        SecurityAlert alert = new SecurityAlert();
        alert.setTimestamp(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        alert.setCorrelationId(CorrelationId.current());
        alert.setAlertType(sanitizer.sanitize(alertType));
        alert.setSeverity(sanitizer.sanitize(severity));
        alert.setActor(getActor());
        alert.setTargetType(sanitizer.sanitize(targetType));
        alert.setTargetId(targetId);
        alert.setDescription(sanitizer.sanitize(description));
        alert.setIntegrityHash(integrityHash(alert));

        securityAlertRepository.save(alert);
    }

    private int increment(String key) {
        long now = Instant.now().toEpochMilli();

        AttemptCounter counter = counters.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart > WINDOW_MILLIS) {
                return new AttemptCounter(1, now);
            }

            existing.count++;
            return existing;
        });

        return counter.count;
    }

    private void reset(String key) {
        counters.remove(key);
    }

    private String getActor() {
        try {
            return SecurityUtils.getCurrentUsername();
        } catch (Exception e) {
            return "anonymous";
        }
    }

    private String sanitizePath(String path) {
        if (path == null || path.isBlank()) {
            return "unknown";
        }

        String sanitizedPath = sanitizer.sanitize(path);
        if (sanitizedPath.length() > 160) {
            return sanitizedPath.substring(0, 160);
        }

        return sanitizedPath;
    }

    private String integrityHash(SecurityAlert alert) {
        String payload = String.join("|",
                nullToEmpty(alert.getTimestamp()),
                nullToEmpty(alert.getCorrelationId()),
                nullToEmpty(alert.getAlertType()),
                nullToEmpty(alert.getSeverity()),
                nullToEmpty(alert.getActor()),
                nullToEmpty(alert.getTargetType()),
                nullToEmpty(alert.getTargetId()),
                nullToEmpty(alert.getDescription())
        );
        return sha256(payload);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Could not calculate security alert integrity hash", e);
        }
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private static class AttemptCounter {
        private int count;
        private final long windowStart;

        private AttemptCounter(int count, long windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }
    }
}
