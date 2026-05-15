package com.ghostreport.controller;

import com.ghostreport.model.Attachment;
import com.ghostreport.model.Report;
import com.ghostreport.model.ReportStatus;
import com.ghostreport.repository.AttachmentRepository;
import com.ghostreport.repository.AuditLogRepository;
import com.ghostreport.repository.ReportRepository;
import com.ghostreport.repository.SecurityAlertRepository;
import com.ghostreport.service.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:report-controller-attachment-upload-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "server.error.include-stacktrace=never",
        "ghostreport.backup-dir=target/test-backups/report-controller-attachment-upload",
        "app.upload-dir=target/test-uploads/report-controller-attachment-upload",
        "ghostreport.backup-enabled=true",
        "security.rate-limit.upload.max-attempts=100"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerAttachmentUploadTest {

    private static final String TRACKING_CODE = "GR-abcdefghijklmnopqrst";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private SecurityAlertRepository securityAlertRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.upload-dir}")
    private String uploadDir;

    private Path uploadBase;

    @BeforeEach
    void setup() throws Exception {
        uploadBase = Path.of(uploadDir).toAbsolutePath().normalize();

        securityAlertRepository.deleteAll();
        auditLogRepository.deleteAll();
        attachmentRepository.deleteAll();
        reportRepository.deleteAll();
        deleteRecursively(uploadBase);
        Files.createDirectories(uploadBase);
    }

    @AfterEach
    void cleanup() throws Exception {
        securityAlertRepository.deleteAll();
        auditLogRepository.deleteAll();
        attachmentRepository.deleteAll();
        reportRepository.deleteAll();
        deleteRecursively(uploadBase);
    }

    @Test
    void allowedUploadPersistsAttachmentAndStoresUuidFileInsideBase() throws Exception {
        Report report = createReport();
        byte[] content = "approved evidence".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "invoice.txt",
                "text/plain",
                content
        );

        mockMvc.perform(multipart("/reports/{id}/attachments", report.getId())
                        .file(file)
                        .param("trackingCode", TRACKING_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].originalName").value("invoice.txt"))
                .andExpect(jsonPath("$[0].mimeType").value("text/plain"))
                .andExpect(jsonPath("$[0].size").value(content.length));

        List<Attachment> attachments = attachmentRepository.findByReportId(report.getId());
        assertEquals(1, attachments.size(), "Expected one persisted attachment");

        Attachment attachment = attachments.get(0);
        assertEquals(report.getId(), attachment.getReport().getId());
        assertEquals("invoice.txt", attachment.getOriginalName());
        assertNotEquals(attachment.getOriginalName(), attachment.getStoredName());
        assertTrue(
                attachment.getStoredName().matches("^[0-9a-fA-F-]{36}\\.txt$"),
                "Expected stored filename to be UUID-based"
        );
        assertNotNull(UUID.fromString(attachment.getFileReference()));
        assertEquals(sha256(content), attachment.getHash());

        Path storedPath = uploadBase.resolve(attachment.getStoragePath()).toAbsolutePath().normalize();
        assertTrue(storedPath.startsWith(uploadBase), "Stored file must remain inside upload base");
        assertTrue(Files.exists(storedPath), "Stored file should exist on disk");
        assertEquals(content.length, Files.size(storedPath));
    }

    @Test
    void forbiddenMimeTypeIsRejectedWithoutPersistingOrLeakingInternalData() throws Exception {
        Report report = createReport();
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "malware.exe",
                "application/octet-stream",
                "not really executable".getBytes()
        );

        String response = mockMvc.perform(multipart("/reports/{id}/attachments", report.getId())
                        .file(file)
                        .param("trackingCode", TRACKING_CODE))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(attachmentRepository.findByReportId(report.getId()).isEmpty());
        assertEquals(0, countRegularFiles(uploadBase));
        assertDoesNotExposeInternalData(response);
    }

    @ParameterizedTest
    @ValueSource(strings = {"../../evil.txt", "..\\..\\evil.txt"})
    void maliciousOriginalFilenameIsRejected(String originalFilename) throws Exception {
        Report report = createReport();
        byte[] content = "malicious name, valid content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "files",
                originalFilename,
                "text/plain",
                content
        );

        mockMvc.perform(multipart("/reports/{id}/attachments", report.getId())
                        .file(file)
                        .param("trackingCode", TRACKING_CODE))
                .andExpect(status().isBadRequest());

        assertTrue(attachmentRepository.findByReportId(report.getId()).isEmpty());
        assertEquals(0, countRegularFiles(uploadBase));
    }

    @Test
    void oversizedUploadUsesProductionLimitAndReturnsControlledError() throws Exception {
        Report report = createReport();
        long maxFileSize = productionMaxFileSize();
        byte[] oversizedContent = new byte[Math.toIntExact(maxFileSize + 1)];
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "large-file.pdf",
                "application/pdf",
                oversizedContent
        );

        String response = mockMvc.perform(multipart("/reports/{id}/attachments", report.getId())
                        .file(file)
                        .param("trackingCode", TRACKING_CODE))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(attachmentRepository.findByReportId(report.getId()).isEmpty());
        assertEquals(0, countRegularFiles(uploadBase));
        assertDoesNotExposeInternalData(response);
    }

    @Test
    void uploadRequiresMatchingTrackingCode() throws Exception {
        Report report = createReport();
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "invoice.txt",
                "text/plain",
                "approved evidence".getBytes()
        );

        mockMvc.perform(multipart("/reports/{id}/attachments", report.getId()).file(file))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/reports/{id}/attachments", report.getId())
                        .file(file)
                        .param("trackingCode", "GR-zzzzzzzzzzzzzzzzzzzz"))
                .andExpect(status().isForbidden());

        assertTrue(attachmentRepository.findByReportId(report.getId()).isEmpty());
        assertEquals(0, countRegularFiles(uploadBase));
    }

    @Test
    void invalidReportIdAndInvalidTrackingCodeReturnSameGenericError() throws Exception {
        Report report = createReport();
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "invoice.txt",
                "text/plain",
                "approved evidence".getBytes()
        );

        String wrongCodeResponse = mockMvc.perform(multipart("/reports/{id}/attachments", report.getId())
                        .file(file)
                        .param("trackingCode", "GR-zzzzzzzzzzzzzzzzzzzz"))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String wrongReportResponse = mockMvc.perform(multipart("/reports/{id}/attachments", report.getId() + 10_000)
                        .file(file)
                        .param("trackingCode", TRACKING_CODE))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(wrongCodeResponse.contains("Upload not authorized"));
        assertTrue(wrongReportResponse.contains("Upload not authorized"));
        assertFalse(wrongCodeResponse.contains("tracking"));
        assertFalse(wrongReportResponse.contains("not found"));
    }

    @Test
    void fakePdfWithWrongMagicBytesIsRejected() throws Exception {
        Report report = createReport();
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "evidence.pdf",
                "application/pdf",
                "MZ executable content".getBytes()
        );

        mockMvc.perform(multipart("/reports/{id}/attachments", report.getId())
                        .file(file)
                        .param("trackingCode", TRACKING_CODE))
                .andExpect(status().isBadRequest());

        assertTrue(attachmentRepository.findByReportId(report.getId()).isEmpty());
        assertEquals(0, countRegularFiles(uploadBase));
    }

    @Test
    void maliciousFilenameRejectionsAreAuditedWithoutRawFilename() throws Exception {
        Report report = createReport();
        String maliciousFilename = "../evil\r\nname.txt";

        for (int i = 0; i < 3; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "files",
                    maliciousFilename,
                    "text/plain",
                    "approved evidence".getBytes()
            );

            mockMvc.perform(multipart("/reports/{id}/attachments", report.getId())
                            .file(file)
                            .param("trackingCode", TRACKING_CODE))
                    .andExpect(status().isBadRequest());
        }

        assertTrue(
                auditLogRepository.findAll()
                        .stream()
                        .anyMatch(log ->
                                "UPLOAD_REJECTED".equals(log.getAction()) &&
                                        log.getDetails() != null &&
                                        log.getDetails().contains("Invalid filename") &&
                                        !log.getDetails().contains("evil") &&
                                        !log.getDetails().contains("\n")
                        )
        );
        assertTrue(
                securityAlertRepository.findAll()
                        .stream()
                        .anyMatch(alert ->
                                "SUSPICIOUS_UPLOAD_ACTIVITY".equals(alert.getAlertType()) &&
                                        alert.getDescription() != null &&
                                        !alert.getDescription().contains("evil") &&
                                        !alert.getDescription().contains("\n")
                        )
        );
    }

    @Test
    void repeatedRejectedUploadsCreateSecurityAlert() throws Exception {
        Report report = createReport();

        for (int i = 0; i < 3; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "files",
                    "invoice-" + i + ".txt",
                    "text/plain",
                    "approved evidence".getBytes()
            );

            mockMvc.perform(multipart("/reports/{id}/attachments", report.getId())
                            .file(file)
                            .param("trackingCode", "GR-zzzzzzzzzzzzzzzzzzzz"))
                    .andExpect(status().isForbidden());
        }

        assertTrue(
                securityAlertRepository.findAll()
                        .stream()
                        .anyMatch(alert -> "SUSPICIOUS_UPLOAD_ACTIVITY".equals(alert.getAlertType()))
        );
        assertTrue(attachmentRepository.findByReportId(report.getId()).isEmpty());
    }

    private Report createReport() {
        Report report = new Report();
        report.setTitle("Upload security test");
        report.setDescription("Controlled test report for attachment upload.");
        report.setCategory("Security");
        report.setStatus(ReportStatus.SUBMITTED);
        report.setTrackingCodeHash(passwordEncoder.encode(TRACKING_CODE));

        return reportRepository.save(report);
    }

    private long productionMaxFileSize() throws Exception {
        Field field = FileStorageService.class.getDeclaredField("MAX_FILE_SIZE");
        field.setAccessible(true);
        return (long) field.get(null);
    }

    private String sha256(byte[] content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(content));
    }

    private long countRegularFiles(Path path) throws Exception {
        if (!Files.exists(path)) {
            return 0;
        }

        try (var files = Files.walk(path)) {
            return files.filter(Files::isRegularFile).count();
        }
    }

    private void assertDoesNotExposeInternalData(String response) {
        assertFalse(response.contains(uploadBase.toString()));
        assertFalse(response.contains("java."));
        assertFalse(response.contains("org.springframework"));
        assertFalse(response.contains("trackingCodeHash"));
    }

    private void deleteRecursively(Path path) throws Exception {
        if (path == null || !Files.exists(path)) {
            return;
        }

        try (var files = Files.walk(path)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(file);
            }
        }
    }
}
