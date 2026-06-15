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
    void explicitProductionProfileRejectsDevelopmentJwtSecret() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        SecurityConfigurationValidator validator = validator(
                environment,
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
    void runTriggersConfigurationValidation() {
        SecurityConfigurationValidator validator = validator(
                new MockEnvironment(),
                "short-secret",
                VALID_BACKUP_SECRET,
                3600,
                false,
                "uploads",
                "backups"
        );

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
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
    void acceptsJwtSecretWithExactlyThirtyTwoBytes() {
        SecurityConfigurationValidator validator = validator(
                new MockEnvironment(),
                "j".repeat(32),
                VALID_BACKUP_SECRET,
                3600,
                false,
                "uploads",
                "backups"
        );

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsJwtSecretBelowThirtyTwoBytes() {
        SecurityConfigurationValidator validator = validator(
                new MockEnvironment(),
                "j".repeat(31),
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
    void acceptsBackupHmacSecretWithExactlyThirtyTwoBytes() {
        SecurityConfigurationValidator validator = validator(
                new MockEnvironment(),
                VALID_TEST_VALUE,
                "b".repeat(32),
                3600,
                false,
                "uploads",
                "backups"
        );

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsBackupHmacSecretBelowThirtyTwoBytes() {
        SecurityConfigurationValidator validator = validator(
                new MockEnvironment(),
                VALID_TEST_VALUE,
                "b".repeat(31),
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
    void acceptsOneSecondJwtExpiration() {
        SecurityConfigurationValidator validator = validator(
                new MockEnvironment(),
                VALID_TEST_VALUE,
                VALID_BACKUP_SECRET,
                1,
                false,
                "uploads",
                "backups"
        );

        assertThatCode(validator::validate).doesNotThrowAnyException();
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

    @Test
    void productionLikeConfigurationRejectsMissingTlsMode() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://db:5432/ghostreport?sslmode=verify-full");

        SecurityConfigurationValidator validator = rawValidator(
                environment,
                VALID_TEST_VALUE,
                VALID_BACKUP_SECRET,
                3600,
                false,
                "uploads",
                "backups"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ghostreport.transport.tls-mode");
    }

    @Test
    void directTlsModeRequiresSslKeyStore() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("ghostreport.transport.tls-mode", "direct")
                .withProperty("server.ssl.enabled", "false")
                .withProperty("server.ssl.enabled-protocols", "TLSv1.3,TLSv1.2")
                .withProperty("spring.datasource.url", "jdbc:postgresql://db:5432/ghostreport?sslmode=verify-full");

        SecurityConfigurationValidator validator = rawValidator(
                environment,
                VALID_TEST_VALUE,
                VALID_BACKUP_SECRET,
                3600,
                false,
                "uploads",
                "backups"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Direct TLS mode");
    }

    @Test
    void directTlsModeRejectsLegacyTlsProtocols() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("ghostreport.transport.tls-mode", "direct")
                .withProperty("server.ssl.enabled", "true")
                .withProperty("server.ssl.key-store", "file:/run/secrets/ghostreport.p12")
                .withProperty("server.ssl.enabled-protocols", "TLSv1,TLSv1.2")
                .withProperty("spring.datasource.url", "jdbc:postgresql://db:5432/ghostreport?sslmode=verify-full");

        SecurityConfigurationValidator validator = rawValidator(
                environment,
                VALID_TEST_VALUE,
                VALID_BACKUP_SECRET,
                3600,
                false,
                "uploads",
                "backups"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TLSv1.2 or TLSv1.3");
    }

    @Test
    void directTlsModeAcceptsModernTlsProtocols() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("ghostreport.transport.tls-mode", "direct")
                .withProperty("server.ssl.enabled", "true")
                .withProperty("server.ssl.key-store", "file:/run/secrets/ghostreport.p12")
                .withProperty("server.ssl.enabled-protocols", "TLSv1.3,TLSv1.2")
                .withProperty("spring.datasource.url", "jdbc:postgresql://db:5432/ghostreport?sslmode=verify-full");
        withResourceLimits(environment);

        SecurityConfigurationValidator validator = rawValidator(
                environment,
                VALID_TEST_VALUE,
                VALID_BACKUP_SECRET,
                3600,
                false,
                "uploads",
                "backups"
        );

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void reverseProxyTlsModeRequiresTrustedForwardedHeaders() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("ghostreport.transport.tls-mode", "reverse-proxy")
                .withProperty("server.forward-headers-strategy", "none")
                .withProperty("ghostreport.transport.trusted-proxy-enabled", "false")
                .withProperty("spring.datasource.url", "jdbc:postgresql://db:5432/ghostreport?sslmode=verify-full");

        SecurityConfigurationValidator validator = rawValidator(
                environment,
                VALID_TEST_VALUE,
                VALID_BACKUP_SECRET,
                3600,
                false,
                "uploads",
                "backups"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Reverse proxy TLS mode");
    }

    @Test
    void productionPostgresRequiresTlsSslMode() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("ghostreport.transport.tls-mode", "reverse-proxy")
                .withProperty("server.forward-headers-strategy", "framework")
                .withProperty("ghostreport.transport.trusted-proxy-enabled", "true")
                .withProperty("spring.datasource.url", "jdbc:postgresql://db:5432/ghostreport");

        SecurityConfigurationValidator validator = rawValidator(
                environment,
                VALID_TEST_VALUE,
                VALID_BACKUP_SECRET,
                3600,
                false,
                "uploads",
                "backups"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PostgreSQL connections must validate TLS certificates");
    }

    @Test
    void productionPostgresRejectsEncryptedButUnverifiedTlsMode() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("ghostreport.transport.tls-mode", "reverse-proxy")
                .withProperty("server.forward-headers-strategy", "framework")
                .withProperty("ghostreport.transport.trusted-proxy-enabled", "true")
                .withProperty("spring.datasource.url", "jdbc:postgresql://db:5432/ghostreport?sslmode=require");
        withResourceLimits(environment);

        SecurityConfigurationValidator validator = rawValidator(
                environment,
                VALID_TEST_VALUE,
                VALID_BACKUP_SECRET,
                3600,
                false,
                "uploads",
                "backups"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PostgreSQL connections must validate TLS certificates");
    }

    @Test
    void productionLikeConfigurationRejectsMissingResourceLimits() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("ghostreport.transport.tls-mode", "reverse-proxy")
                .withProperty("server.forward-headers-strategy", "framework")
                .withProperty("ghostreport.transport.trusted-proxy-enabled", "true")
                .withProperty("spring.datasource.url", "jdbc:postgresql://db:5432/ghostreport?sslmode=verify-full");

        SecurityConfigurationValidator validator = rawValidator(
                environment,
                VALID_TEST_VALUE,
                VALID_BACKUP_SECRET,
                3600,
                false,
                "uploads",
                "backups"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.datasource.hikari.maximum-pool-size");
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
        if (environment.getProperty("ghostreport.transport.tls-mode") == null) {
            environment
                    .withProperty("ghostreport.transport.tls-mode", "reverse-proxy")
                    .withProperty("server.forward-headers-strategy", "framework")
                    .withProperty("ghostreport.transport.trusted-proxy-enabled", "true");
        }
        if (environment.getProperty("spring.datasource.url") == null) {
            environment.withProperty("spring.datasource.url", "jdbc:postgresql://db:5432/ghostreport?sslmode=verify-full");
        }
        if (environment.getProperty("spring.datasource.hikari.maximum-pool-size") == null) {
            withResourceLimits(environment);
        }
        return rawValidator(environment, jwtSecret, backupHmacSecret, expirationSeconds, seedUsersEnabled, uploadDir, backupDir);
    }

    private void withResourceLimits(MockEnvironment environment) {
        environment
                .withProperty("spring.datasource.hikari.maximum-pool-size", "10")
                .withProperty("spring.datasource.hikari.minimum-idle", "2")
                .withProperty("spring.datasource.hikari.connection-timeout", "30000")
                .withProperty("spring.datasource.hikari.validation-timeout", "5000")
                .withProperty("server.tomcat.max-connections", "200")
                .withProperty("server.tomcat.accept-count", "100")
                .withProperty("server.tomcat.threads.max", "50")
                .withProperty("server.tomcat.threads.min-spare", "5");
    }

    private SecurityConfigurationValidator rawValidator(
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
