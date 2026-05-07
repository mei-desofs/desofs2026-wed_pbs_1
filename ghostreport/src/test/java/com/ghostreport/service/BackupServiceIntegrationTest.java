package com.ghostreport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostreport.dto.BackupOperationResponse;
import com.ghostreport.model.Report;
import com.ghostreport.model.ReportStatus;
import com.ghostreport.repository.ReportRepository;
import com.ghostreport.repository.SecurityAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.UUID;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:backup-service-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "ghostreport.backup-dir=target/test-backups/service",
        "app.upload-dir=target/test-uploads/service",
        "ghostreport.backup-enabled=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BackupServiceIntegrationTest {

    private static final Path BACKUP_DIR = Path.of("target/test-backups/service");
    private static final Path UPLOAD_DIR = Path.of("target/test-uploads/service");

    @Autowired
    private BackupService backupService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private SecurityAlertRepository securityAlertRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        cleanDirectory(BACKUP_DIR);
        cleanDirectory(UPLOAD_DIR);
        Files.createDirectories(BACKUP_DIR);
        Files.createDirectories(UPLOAD_DIR);
        reportRepository.deleteAll();
        securityAlertRepository.deleteAll();
    }

    @Test
    void createsBackupWithManifestDatabaseExportsFileHashesAndFinalZipHash() throws Exception {
        createReport();
        Path evidence = UPLOAD_DIR.resolve("reports/1/attachments/evidence.txt");
        Files.createDirectories(evidence.getParent());
        Files.writeString(evidence, "important evidence");

        BackupOperationResponse response = backupService.createBackup();

        Path zipPath = BACKUP_DIR.resolve(response.filename());
        assertThat(Files.exists(zipPath)).isTrue();
        assertThat(Files.exists(Path.of(zipPath + ".sha256"))).isTrue();
        assertThat(response.sha256()).hasSize(64);
        assertThat(Files.readString(Path.of(zipPath + ".sha256")).trim()).isEqualTo(response.sha256());

        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            assertThat(zip.getEntry("db/reports.json")).isNotNull();
            assertThat(zip.getEntry("db/case-reviews.json")).isNotNull();
            assertThat(zip.getEntry("db/users.json")).isNotNull();
            assertThat(zip.getEntry("db/audit-logs.json")).isNotNull();
            assertThat(zip.getEntry("db/security-alerts.json")).isNotNull();
            assertThat(zip.getEntry("files/reports/1/attachments/evidence.txt")).isNotNull();

            JsonNode manifest = readManifest(zip);
            assertThat(manifest.path("formatVersion").asText()).isEqualTo("1");
            assertThat(manifest.path("totalFiles").asInt()).isEqualTo(response.totalFiles());
            assertThat(manifest.path("databaseExports").path("reports").asInt()).isEqualTo(1);
            assertThat(manifest.path("files")).allSatisfy(file -> {
                assertThat(file.path("path").asText()).isNotBlank();
                assertThat(file.path("sha256").asText()).hasSize(64);
            });
        }
    }

    @Test
    void verifiesValidBackup() {
        createReport();
        BackupOperationResponse response = backupService.createBackup();

        assertThat(backupService.verifyBackup(response.filename()).valid()).isTrue();
    }

    @Test
    void rejectsTamperedBackup() throws Exception {
        createReport();
        BackupOperationResponse response = backupService.createBackup();
        Path zipPath = BACKUP_DIR.resolve(response.filename());
        Files.write(zipPath, new byte[]{1}, StandardOpenOption.APPEND);

        assertThatThrownBy(() -> backupService.verifyBackup(response.filename()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(securityAlertRepository.findAll())
                .anyMatch(alert -> alert.getAlertType().equals("BACKUP_INTEGRITY_FAILURE"));
    }

    @Test
    void blocksPathTraversalInBackupFilename() {
        assertThatThrownBy(() -> backupService.verifyBackup("../secret.zip"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(securityAlertRepository.findAll())
                .anyMatch(alert -> alert.getAlertType().equals("BACKUP_PATH_TRAVERSAL_ATTEMPT"));
    }

    @Test
    void rs13SimulatedLossCanBeRecoveredToControlledStagingAfterValidation() throws Exception {
        createReport();
        Path evidence = UPLOAD_DIR.resolve("reports/1/attachments/evidence-rs13.txt");
        Files.createDirectories(evidence.getParent());
        Files.writeString(evidence, "rs13 evidence");

        BackupOperationResponse response = backupService.createBackup();
        Files.delete(evidence);

        var restore = backupService.restoreBackup(response.filename());

        assertThat(restore.restored()).isTrue();
        assertThat(restore.restorePath()).contains("restore-staging");
        assertThat(Path.of(restore.restorePath()).resolve("files/reports/1/attachments/evidence-rs13.txt"))
                .hasContent("rs13 evidence");
        assertThat(backupService.verifyBackup(response.filename()).valid()).isTrue();
    }

    private void createReport() {
        Report report = new Report();
        report.setTitle("Backup test");
        report.setDescription("Description");
        report.setCategory("Security");
        report.setStatus(ReportStatus.SUBMITTED);
        report.setTrackingCodeHash("{bcrypt}" + UUID.randomUUID());
        reportRepository.save(report);
    }

    private JsonNode readManifest(ZipFile zip) throws Exception {
        try (InputStream input = zip.getInputStream(zip.getEntry("manifest.json"))) {
            return objectMapper.readTree(input);
        }
    }

    private void cleanDirectory(Path dir) throws Exception {
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
