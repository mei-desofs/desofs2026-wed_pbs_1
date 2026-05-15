package com.ghostreport.security;

import com.ghostreport.dto.ReportResponse;
import com.ghostreport.model.Attachment;
import com.ghostreport.model.CasePriority;
import com.ghostreport.model.CaseReview;
import com.ghostreport.model.Report;
import com.ghostreport.model.ReportStatus;
import com.ghostreport.model.User;
import com.ghostreport.model.UserRole;
import com.ghostreport.repository.AttachmentRepository;
import com.ghostreport.repository.CaseReviewRepository;
import com.ghostreport.repository.ReportRepository;
import com.ghostreport.repository.UserRepository;
import com.ghostreport.service.CaseReviewService;
import com.ghostreport.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:analyst-case-ownership-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "ghostreport.backup-dir=target/test-backups/analyst-case-ownership",
        "app.upload-dir=target/test-uploads/analyst-case-ownership",
        "ghostreport.backup-enabled=true"
})
@ActiveProfiles("test")
class AnalystCaseOwnershipTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private CaseReviewService caseReviewService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private CaseReviewRepository caseReviewRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.upload-dir}")
    private String uploadDir;

    private User owner;
    private User otherAnalyst;
    private Report unassignedReport;
    private Report ownerReport;
    private Report otherReport;

    @BeforeEach
    void setUp() {
        caseReviewRepository.deleteAll();
        reportRepository.deleteAll();

        owner = createUser("owner_analyst", UserRole.ANALYST);
        otherAnalyst = createUser("other_analyst", UserRole.ANALYST);

        unassignedReport = createReport("Unassigned case");
        ownerReport = createReport("Owner case");
        otherReport = createReport("Other case");

        createCaseReview(ownerReport, owner);
        createCaseReview(otherReport, otherAnalyst);
    }

    @Test
    @WithMockUser(username = "owner_analyst", roles = "ANALYST")
    void analystOnlySeesUnassignedCasesAndOwnCases() {
        List<Long> visibleReportIds = reportService.getAllReports()
                .stream()
                .map(ReportResponse::getId)
                .toList();

        assertThat(visibleReportIds)
                .contains(unassignedReport.getId(), ownerReport.getId())
                .doesNotContain(otherReport.getId());
    }

    @Test
    @WithMockUser(username = "other_analyst", roles = "ANALYST")
    void analystCannotTakeCaseAlreadyAssignedToAnotherAnalyst() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> caseReviewService.assignAnalystToCurrentUser(ownerReport.getId())
        );

        assertThat(exception.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    @WithMockUser(username = "other_analyst", roles = "ANALYST")
    void analystCannotDownloadAttachmentFromCaseAssignedToAnotherAnalyst() throws Exception {
        Attachment attachment = createAttachment(ownerReport);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> reportService.downloadAttachment(attachment.getId())
        );

        assertThat(exception.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    @WithMockUser(username = "owner_analyst", roles = "ANALYST")
    void analystCanDownloadAttachmentFromOwnAssignedCase() throws Exception {
        Attachment attachment = createAttachment(ownerReport);

        assertThat(reportService.downloadAttachment(attachment.getId()).getStatusCode().value())
                .isEqualTo(200);
    }

    private User createUser(String username, UserRole role) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User user = new User();
                    user.setUsername(username);
                    user.setEmail(username + "@ghostreport.test");
                    user.setPasswordHash("test-password-hash");
                    user.setRole(role);
                    user.setActive(true);
                    return userRepository.save(user);
                });
    }

    private Report createReport(String title) {
        Report report = new Report();
        report.setTitle(title);
        report.setDescription("Descricao de teste para ownership de casos.");
        report.setCategory("Security");
        report.setStatus(ReportStatus.SUBMITTED);
        report.setTrackingCodeHash("hash-" + title + "-" + System.nanoTime());
        return reportRepository.save(report);
    }

    private void createCaseReview(Report report, User analyst) {
        CaseReview caseReview = new CaseReview();
        caseReview.setReport(report);
        caseReview.setAssignedAnalyst(analyst);
        caseReview.setPriority(CasePriority.MEDIUM);
        caseReviewRepository.save(caseReview);
    }

    private Attachment createAttachment(Report report) throws Exception {
        Path file = Path.of(uploadDir)
                .toAbsolutePath()
                .normalize()
                .resolve("reports")
                .resolve(String.valueOf(report.getId()))
                .resolve("attachments")
                .resolve("owned-evidence.txt");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "owned evidence");

        Attachment attachment = new Attachment();
        attachment.setReport(report);
        attachment.setOriginalName("owned-evidence.txt");
        attachment.setStoredName("owned-evidence.txt");
        attachment.setFileReference("owned-evidence");
        attachment.setStoragePath(file.toString());
        attachment.setMimeType("text/plain");
        attachment.setSize(Files.size(file));
        attachment.setHash("test-hash");
        return attachmentRepository.save(attachment);
    }
}
