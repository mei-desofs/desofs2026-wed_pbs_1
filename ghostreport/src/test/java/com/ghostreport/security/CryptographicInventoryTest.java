package com.ghostreport.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CryptographicInventoryTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java/com/ghostreport");
    private static final Path INVENTORY = Path.of(
            "..",
            "Deliverables",
            "Phase 2",
            "Sprint 2",
            "CRYPTOGRAPHIC_INVENTORY.md"
    ).normalize();

    @Test
    void cryptographicMechanismsAreDocumentedInInventory() throws IOException {
        String inventory = Files.readString(INVENTORY, StandardCharsets.UTF_8);
        Map<String, String> expectedEvidence = Map.ofEntries(
                Map.entry("BCryptPasswordEncoder", "Passwords internas"),
                Map.entry("SecureRandom", "Tracking code anonimo"),
                Map.entry("MfaChallengeService", "MFA dev/test"),
                Map.entry("PasswordResetService", "Password reset"),
                Map.entry("HmacSHA256", "JWT"),
                Map.entry("BackupService", "Backups"),
                Map.entry("AuditLogService", "Audit logs e security alerts"),
                Map.entry("FileStorageService", "Uploads e pacotes de evidencia")
        );

        expectedEvidence.forEach((codeToken, inventoryToken) -> {
            assertThat(sourceContains(codeToken))
                    .as(codeToken + " should still be present in source")
                    .isTrue();
            assertThat(inventory)
                    .as(codeToken + " should be mapped in the cryptographic inventory")
                    .contains(inventoryToken);
        });
    }

    @Test
    void sourceDoesNotUseKnownDeprecatedCryptographicAlgorithms() throws IOException {
        String source = readAllApplicationSource();

        assertThat(source)
                .doesNotContain("MD5")
                .doesNotContain("SHA-1")
                .doesNotContain("DES")
                .doesNotContain("RC4")
                .doesNotContain("AES/ECB")
                .doesNotContain("RSA/ECB/PKCS1Padding");
    }

    private static boolean sourceContains(String token) {
        try {
            return readAllApplicationSource().contains(token);
        } catch (IOException e) {
            throw new IllegalStateException("Could not scan source", e);
        }
    }

    private static String readAllApplicationSource() throws IOException {
        StringBuilder source = new StringBuilder();
        try (var paths = Files.walk(SOURCE_ROOT)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .forEach(path -> source.append(read(path)).append('\n'));
        }
        return source.toString();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }
    }
}
