package com.ghostreport.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigurationValidatorTest {

    private static final String VALID_TEST_VALUE = "valid-test-value-with-at-least-32-chars";
    private static final String VALID_BACKUP_SECRET = "valid-backup-hmac-secret-32-chars";

    @Test
    void productionLikeConfigurationRejectsDevelopmentJwtSecret() {
        SecurityConfigurationValidator validator = validator(
                new MockEnvironment(),
                "dev-only-change-this-secret-32-chars",
                VALID_BACKUP_SECRET,
                3600,
                false,
                "uploads",
                "backups"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not use development or test JWT secrets");
    }

    @Test
    void productionLikeConfigurationRejectsSeedUsers() {
        SecurityConfigurationValidator validator = validator(
                new MockEnvironment(),
                VALID_TEST_VALUE,
                VALID_BACKUP_SECRET,
                3600,
                true,
                "uploads",
                "backups"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Seed users must be disabled");
    }

    @Test
    void devProfileAllowsDevelopmentJwtSecretAndSeedUsers() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        SecurityConfigurationValidator validator = validator(
                environment,
                "dev-only-change-this-secret-32-chars",
                "dev-only-backup-hmac-secret-32-chars",
                3600,
                true,
                "uploads",
                "backups"
        );

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void testProfileAllowsTestJwtSecretAndSeedUsers() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");

        SecurityConfigurationValidator validator = validator(
                environment,
                "test-only-change-this-secret-32-chars",
                "test-only-backup-hmac-secret-32-chars",
                3600,
                true,
                "target/test-uploads",
                "target/test-backups"
        );

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsWeakJwtSecret() {
        SecurityConfigurationValidator validator = validator(
                new MockEnvironment(),
                "short-secret",
                VALID_BACKUP_SECRET,
                3600,
                false,
                "uploads",
                "backups"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void rejectsWeakBackupHmacSecret() {
        SecurityConfigurationValidator validator = validator(
                new MockEnvironment(),
                VALID_TEST_VALUE,
                "short-backup-secret",
                3600,
                false,
                "uploads",
                "backups"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BACKUP_HMAC_SECRET");
    }

    @Test
    void rejectsBackupHmacSecretReusedFromJwtSecret() {
        SecurityConfigurationValidator validator = validator(
                new MockEnvironment(),
                VALID_TEST_VALUE,
                VALID_TEST_VALUE,
                3600,
                false,
                "uploads",
                "backups"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("separate from JWT_SECRET");
    }

    @Test
    void productionLikeConfigurationRejectsDevelopmentBackupHmacSecret() {
        SecurityConfigurationValidator validator = validator(
                new MockEnvironment(),
                VALID_TEST_VALUE,
                "dev-only-backup-hmac-secret-32-chars",
                3600,
                false,
                "uploads",
                "backups"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not use development or test backup HMAC secrets");
    }

    @Test
    void rejectsNonPositiveJwtExpiration() {
        SecurityConfigurationValidator validator = validator(
                new MockEnvironment(),
                VALID_TEST_VALUE,
                VALID_BACKUP_SECRET,
                0,
                false,
                "uploads",
                "backups"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_EXPIRATION_SECONDS");
    }

    @Test
    void rejectsSameUploadAndBackupDirectory() {
        SecurityConfigurationValidator validator = validator(
                new MockEnvironment(),
                VALID_TEST_VALUE,
                VALID_BACKUP_SECRET,
                3600,
                false,
                "storage",
                "storage"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be the same");
    }

    private SecurityConfigurationValidator validator(
            MockEnvironment environment,
            String jwtSecret,
            String backupHmacSecret,
            long expirationSeconds,
            boolean seedUsersEnabled,
            String uploadDir,
            String backupDir
    ) {
        return new SecurityConfigurationValidator(
                environment,
                jwtSecret,
                backupHmacSecret,
                "test-backup-hmac-v1",
                expirationSeconds,
                seedUsersEnabled,
                uploadDir,
                backupDir
        );
    }
}
