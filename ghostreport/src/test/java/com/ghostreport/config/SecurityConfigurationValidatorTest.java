package com.ghostreport.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigurationValidatorTest {

    private static final String STRONG_SECRET = "strong-secret-with-at-least-32-characters";

    @Test
    void productionLikeConfigurationRejectsDevelopmentJwtSecret() {
        SecurityConfigurationValidator validator = validator(
                new MockEnvironment(),
                "dev-only-change-this-secret-32-chars",
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
                STRONG_SECRET,
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
    void rejectsNonPositiveJwtExpiration() {
        SecurityConfigurationValidator validator = validator(
                new MockEnvironment(),
                STRONG_SECRET,
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
                STRONG_SECRET,
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
            long expirationSeconds,
            boolean seedUsersEnabled,
            String uploadDir,
            String backupDir
    ) {
        return new SecurityConfigurationValidator(
                environment,
                jwtSecret,
                expirationSeconds,
                seedUsersEnabled,
                uploadDir,
                backupDir
        );
    }
}
