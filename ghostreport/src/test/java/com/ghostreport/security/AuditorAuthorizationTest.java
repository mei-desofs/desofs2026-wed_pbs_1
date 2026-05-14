package com.ghostreport.security;

import com.ghostreport.model.User;
import com.ghostreport.model.UserRole;
import com.ghostreport.model.CasePriority;
import com.ghostreport.model.CaseReview;
import com.ghostreport.model.Report;
import com.ghostreport.model.ReportStatus;
import com.ghostreport.repository.CaseReviewRepository;
import com.ghostreport.repository.ReportRepository;
import com.ghostreport.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auditor-authorization-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "ghostreport.backup-dir=target/test-backups/auditor-authorization",
        "app.upload-dir=target/test-uploads/auditor-authorization",
        "ghostreport.backup-enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditorAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private CaseReviewRepository caseReviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String auditorUsername;
    private String analystUsername;

    @BeforeEach
    void setUp() {
        auditorUsername = createUser(UserRole.AUDITOR);
        analystUsername = createUser(UserRole.ANALYST);
    }

    @Test
    void auditorCanReadAuditEndpoints() throws Exception {
        mockMvc.perform(get("/audit/logs").with(httpBasic(auditorUsername, "password")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/audit/security-alerts").with(httpBasic(auditorUsername, "password")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/audit/cases/closed").with(httpBasic(auditorUsername, "password")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/audit/backups").with(httpBasic(auditorUsername, "password")))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanReadAuditEndpoints() throws Exception {
        mockMvc.perform(get("/audit/logs").with(httpBasic("admin", "Admin123!")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/audit/security-alerts").with(httpBasic("admin", "Admin123!")))
                .andExpect(status().isOk());
    }

    @Test
    void analystAndAnonymousCannotReadAuditEndpoints() throws Exception {
        mockMvc.perform(get("/audit/logs"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/audit/logs").with(httpBasic(analystUsername, "password")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/audit/cases/closed").with(httpBasic(analystUsername, "password")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/audit/backups").with(httpBasic(analystUsername, "password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditorCannotUseAdminEndpoints() throws Exception {
        String createUserJson = """
                {
                  "username": "created-by-auditor",
                  "email": "created-by-auditor@ghostreport.test",
                  "password": "Password123!",
                  "role": "ANALYST"
                }
                """;

        mockMvc.perform(post("/admin/users")
                        .with(httpBasic(auditorUsername, "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserJson))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/backups/ghostreport-backup-20260507-165524.zip/restore")
                        .with(httpBasic(auditorUsername, "password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditorCannotUseAnalystWriteEndpoints() throws Exception {
        mockMvc.perform(patch("/analyst/reports/1/status")
                        .with(httpBasic(auditorUsername, "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/analyst/reports/1/priority")
                        .with(httpBasic(auditorUsername, "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"HIGH\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/analyst/reports/1/notes")
                        .with(httpBasic(auditorUsername, "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"changed\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/analyst/reports/1/assign")
                        .with(httpBasic(auditorUsername, "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditorClosedCaseHistoryDoesNotExposeSensitiveReportData() throws Exception {
        Report report = createClosedReport();
        User analyst = userRepository.findByUsername(analystUsername).orElseThrow();
        createCaseReview(report, analyst);

        String body = mockMvc.perform(get("/audit/cases/closed").with(httpBasic(auditorUsername, "password")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body).contains("\"reportId\":" + report.getId());
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("Sensitive description");
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("tracking-hash");
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("Internal sensitive notes");
    }

    @Test
    void auditorCanVerifyEvidencePackageWithoutReceivingPathsOrFilenames() throws Exception {
        Report report = createClosedReport();
        User analyst = userRepository.findByUsername(analystUsername).orElseThrow();
        createCaseReview(report, analyst);

        String storedName = "stored-" + UUID.randomUUID() + ".txt";
        byte[] evidence = "evidence bytes".getBytes(StandardCharsets.UTF_8);
        String hash = sha256(evidence);

        Path packagePath = Path.of("target/test-uploads/auditor-authorization", "reports", String.valueOf(report.getId()), "case_package");
        Path attachmentsPath = packagePath.resolve("attachments");
        Files.createDirectories(attachmentsPath);
        Files.write(attachmentsPath.resolve(storedName), evidence);
        Files.writeString(packagePath.resolve("evidence_manifest.json"), """
                {
                  "reportId": %d,
                  "status": "RESOLVED",
                  "attachments": [
                    {
                      "originalName": "citizen-document.pdf",
                      "storedName": "%s",
                      "sha256": "%s"
                    }
                  ]
                }
                """.formatted(report.getId(), storedName, hash));

        String body = mockMvc.perform(get("/audit/cases/{id}/evidence-package/verify", report.getId())
                        .with(httpBasic(auditorUsername, "password")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body).contains("\"valid\":true");
        org.assertj.core.api.Assertions.assertThat(body).contains(hash);
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain(storedName);
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("citizen-document.pdf");
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain(packagePath.toAbsolutePath().toString());
    }

    @Test
    void auditorCanVerifyBackupButCannotCreateOrRestoreBackup() throws Exception {
        String body = mockMvc.perform(post("/admin/backups").with(httpBasic("admin", "Admin123!")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String filename = body.replaceAll(".*\"filename\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/audit/backups/{filename}/verify", filename)
                        .with(httpBasic(auditorUsername, "password")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/audit/backups/{filename}/manifest", filename)
                        .with(httpBasic(auditorUsername, "password")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/backups").with(httpBasic(auditorUsername, "password")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/backups/{filename}/restore", filename)
                        .with(httpBasic(auditorUsername, "password")))
                .andExpect(status().isForbidden());
    }

    private String createUser(UserRole role) {
        String username = role.name().toLowerCase() + "_" + UUID.randomUUID();

        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@ghostreport.test");
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);

        userRepository.save(user);

        return username;
    }

    private Report createClosedReport() {
        Report report = new Report();
        report.setTitle("Sensitive title");
        report.setDescription("Sensitive description that auditors must not receive");
        report.setCategory("Fraude");
        report.setStatus(ReportStatus.RESOLVED);
        report.setTrackingCodeHash("tracking-hash-" + UUID.randomUUID());
        return reportRepository.save(report);
    }

    private CaseReview createCaseReview(Report report, User analyst) {
        CaseReview caseReview = new CaseReview();
        caseReview.setReport(report);
        caseReview.setAssignedAnalyst(analyst);
        caseReview.setPriority(CasePriority.HIGH);
        caseReview.setNotes("Internal sensitive notes");
        return caseReviewRepository.save(caseReview);
    }

    private String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }
}
