package com.ghostreport.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DangerousFunctionalityInventoryTest {

    private static final Path INVENTORY = Path.of(
            "..",
            "Deliverables",
            "Phase 2",
            "Sprint 2",
            "DANGEROUS_FUNCTIONALITY.md"
    );

    private static final Path SOURCE_ROOT = Path.of("src/main/java/com/ghostreport");

    @Test
    void dangerousFunctionalityIsDocumented() throws IOException {
        String inventory = Files.readString(INVENTORY, StandardCharsets.UTF_8);

        Map<String, String> expectedEvidence = Map.ofEntries(
                Map.entry("restoreBackup", "Backup restore"),
                Map.entry("createBackup", "Backup create/download/verify"),
                Map.entry("storeAttachment", "Uploads anonimos"),
                Map.entry("downloadAttachment", "Download de anexos"),
                Map.entry("generateCasePackage", "Evidence packages"),
                Map.entry("requestResetForUserId", "Password reset/admin reset"),
                Map.entry("JwtService", "JWT e revogacao"),
                Map.entry("SecurityLogSanitizer", "Audit/security logs"),
                Map.entry("Mac.getInstance", "Criptografia/hashing")
        );

        expectedEvidence.forEach((codeToken, inventoryToken) -> {
            assertThat(sourceContains(codeToken))
                    .as(codeToken + " should still be present in source")
                    .isTrue();
            assertThat(inventory)
                    .as(codeToken + " should be documented as dangerous functionality")
                    .contains(inventoryToken);
        });
    }

    private static boolean sourceContains(String token) {
        try (var paths = Files.walk(SOURCE_ROOT)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .anyMatch(path -> contains(path, token));
        } catch (IOException e) {
            throw new IllegalStateException("Could not scan source tree", e);
        }
    }

    private static boolean contains(Path path, String token) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8).contains(token);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }
    }
}
