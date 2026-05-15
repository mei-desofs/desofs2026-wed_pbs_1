package com.ghostreport.service;

import com.ghostreport.model.SecurityAlert;
import com.ghostreport.repository.SecurityAlertRepository;
import com.ghostreport.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SecurityMonitoringService {

    private final SecurityAlertRepository securityAlertRepository;

    private final Map<String, AttemptCounter> counters = new ConcurrentHashMap<>();

    private static final int MAX_TRACKING_FAILURES = 5;
    private static final int MAX_UPLOAD_FAILURES = 3;
    private static final long WINDOW_MILLIS = 60_000;

    public SecurityMonitoringService(SecurityAlertRepository securityAlertRepository) {
        this.securityAlertRepository = securityAlertRepository;
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

    public void createAlert(String alertType, String severity, String targetType, Long targetId, String description) {
        SecurityAlert alert = new SecurityAlert();
        alert.setAlertType(alertType);
        alert.setSeverity(severity);
        alert.setActor(getActor());
        alert.setTargetType(targetType);
        alert.setTargetId(targetId);
        alert.setDescription(sanitize(description));

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

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }

        return value
                .replaceAll("[\\r\\n]", " ")
                .replaceAll("[\\x00-\\x1F\\x7F]", "")
                .trim();
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
