package com.ghostreport.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Arrays;

@Component
public class SecurityConfigurationValidator implements ApplicationRunner {

    private static final String DEV_ONLY_SECRET = "dev-only-change-this-secret-32-chars";
    private static final String TEST_ONLY_SECRET = "test-only-change-this-secret-32-chars";
    private static final String DEV_ONLY_BACKUP_SECRET = "dev-only-backup-hmac-secret-32-chars";
    private static final String TEST_ONLY_BACKUP_SECRET = "test-only-backup-hmac-secret-32-chars";

    private final Environment environment;
    private final String jwtSecret;
    private final String backupHmacSecret;
    private final String backupHmacKeyId;
    private final long jwtExpirationSeconds;
    private final boolean seedUsersEnabled;
    private final String uploadDir;
    private final String backupDir;

    public SecurityConfigurationValidator(
            Environment environment,
            @Value("${ghostreport.jwt.secret}") String jwtSecret,
            @Value("${ghostreport.backup.hmac-secret}") String backupHmacSecret,
            @Value("${ghostreport.backup.hmac-key-id:backup-hmac-v1}") String backupHmacKeyId,
            @Value("${ghostreport.jwt.expiration-seconds:3600}") long jwtExpirationSeconds,
            @Value("${ghostreport.seed-users.enabled:false}") boolean seedUsersEnabled,
            @Value("${app.upload-dir:uploads}") String uploadDir,
            @Value("${ghostreport.backup-dir:backups}") String backupDir
    ) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.backupHmacSecret = backupHmacSecret;
        this.backupHmacKeyId = backupHmacKeyId;
        this.jwtExpirationSeconds = jwtExpirationSeconds;
        this.seedUsersEnabled = seedUsersEnabled;
        this.uploadDir = uploadDir;
        this.backupDir = backupDir;
    }

    @Override
    public void run(ApplicationArguments args) {
        validate();
    }

    void validate() {
        if (jwtSecret == null || jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be configured with at least 32 characters");
        }

        if (backupHmacSecret == null || backupHmacSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("BACKUP_HMAC_SECRET must be configured with at least 32 characters");
        }

        if (jwtSecret.equals(backupHmacSecret)) {
            throw new IllegalStateException("BACKUP_HMAC_SECRET must be separate from JWT_SECRET");
        }

        if (isBlank(backupHmacKeyId)) {
            throw new IllegalStateException("BACKUP_HMAC_KEY_ID must not be blank");
        }

        if (jwtExpirationSeconds < 1) {
            throw new IllegalStateException("JWT_EXPIRATION_SECONDS must be positive");
        }

        if (isBlank(uploadDir)) {
            throw new IllegalStateException("app.upload-dir must not be blank");
        }

        if (isBlank(backupDir)) {
            throw new IllegalStateException("ghostreport.backup-dir must not be blank");
        }

        Path normalizedUploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        Path normalizedBackupDir = Path.of(backupDir).toAbsolutePath().normalize();
        if (normalizedUploadDir.equals(normalizedBackupDir)) {
            throw new IllegalStateException("ghostreport.backup-dir cannot be the same as app.upload-dir");
        }

        if (isProductionLikeProfile()) {
            validateProductionLikeConfiguration();
        }
    }

    private void validateProductionLikeConfiguration() {
        if (DEV_ONLY_SECRET.equals(jwtSecret) || TEST_ONLY_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("Production-like profiles must not use development or test JWT secrets");
        }

        if (DEV_ONLY_BACKUP_SECRET.equals(backupHmacSecret) || TEST_ONLY_BACKUP_SECRET.equals(backupHmacSecret)) {
            throw new IllegalStateException("Production-like profiles must not use development or test backup HMAC secrets");
        }

        if (seedUsersEnabled) {
            throw new IllegalStateException("Seed users must be disabled in production-like profiles");
        }
    }

    private boolean isProductionLikeProfile() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            return true;
        }

        return Arrays.stream(profiles)
                .noneMatch(profile -> "dev".equals(profile) || "test".equals(profile));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
