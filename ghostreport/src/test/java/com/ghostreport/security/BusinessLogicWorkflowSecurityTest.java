package com.ghostreport.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostreport.model.CasePriority;
import com.ghostreport.model.CaseReview;
import com.ghostreport.model.Report;
import com.ghostreport.model.ReportStatus;
import com.ghostreport.model.User;
import com.ghostreport.model.UserRole;
import com.ghostreport.repository.AttachmentRepository;
import com.ghostreport.repository.AuditLogRepository;
import com.ghostreport.repository.CaseReviewRepository;
import com.ghostreport.repository.ReportRepository;
import com.ghostreport.repository.SecurityAlertRepository;
import com.ghostreport.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:business-logic-workflow-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "ghostreport.backup-dir=target/test-backups/business-logic-workflow",
        "app.upload-dir=target/test-uploads/business-logic-workflow",
        "ghostreport.backup-enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BusinessLogicWorkflowSecurityTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private CaseReviewRepository caseReviewRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private SecurityAlertRepository securityAlertRepository;

    @Autowired
    private EntityManager entityManager;

    private String analystUsername;
    private String otherAnalystUsername;
    private String auditorUsername;
    private String adminUsername;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        securityAlertRepository.deleteAll();
        attachmentRepository.deleteAll();
        caseReviewRepository.deleteAll();
        reportRepository.deleteAll();
        userRepository.deleteAll();

        String suffix = UUID.randomUUID().toString();
        analystUsername = createUser("workflow_analyst_" + suffix, UserRole.ANALYST);
        otherAnalystUsername = createUser("workflow_other_" + suffix, UserRole.ANALYST);
        auditorUsername = createUser("workflow_auditor_" + suffix, UserRole.AUDITOR);
        adminUsername = createUser("workflow_admin_" + suffix, UserRole.ADMIN);
    }

    @Test
    void permittedStatusTransitionSucceedsForOwningAnalyst() throws Exception {
        long reportId = createReportThroughApi();

        assignToCurrentAnalyst(reportId, analystUsername).andExpect(status().isOk());

        mockMvc.perform(patch("/analyst/reports/{id}/status", reportId)
                        .with(csrf())
                        .header("Authorization", bearerToken(analystUsername))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "UNDER_REVIEW"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        assertThat(reportRepository.findById(reportId).orElseThrow().getStatus())
                .isEqualTo(ReportStatus.UNDER_REVIEW);
        assertThat(auditLogRepository.findAll())
                .anySatisfy(log -> {
                    assertThat(log.getAction()).isEqualTo("REPORT_STATUS_CHANGED");
                    assertThat(log.getTargetId()).isEqualTo(reportId);
                    assertThat(log.getDetails()).contains("SUBMITTED").contains("UNDER_REVIEW");
                    assertThat(log.getIntegrityHash()).hasSize(64);
                });
    }

    @Test
    void owningAnalystCanResolveCaseAndRepeatResolvedRequestIdempotently() throws Exception {
        long reportId = createReportThroughApi();

        assignToCurrentAnalyst(reportId, analystUsername).andExpect(status().isOk());

        mockMvc.perform(patch("/analyst/reports/{id}/status", reportId)
                        .with(csrf())
                        .header("Authorization", bearerToken(analystUsername))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(patch("/analyst/reports/{id}/status", reportId)
                        .with(csrf())
                        .header("Authorization", bearerToken(analystUsername))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        assertThat(reportRepository.findById(reportId).orElseThrow().getStatus())
                .isEqualTo(ReportStatus.RESOLVED);
    }

    @Test
    void invalidStatusPayloadReturnsBadRequestAndDoesNotChangeCase() throws Exception {
        long reportId = createReportThroughApi();

        assignToCurrentAnalyst(reportId, analystUsername).andExpect(status().isOk());

        mockMvc.perform(patch("/analyst/reports/{id}/status", reportId)
                        .with(csrf())
                        .header("Authorization", bearerToken(analystUsername))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DONE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"));

        assertThat(reportRepository.findById(reportId).orElseThrow().getStatus())
                .isEqualTo(ReportStatus.UNDER_REVIEW);
    }

    @Test
    void statusUpdateRequiresAuthentication() throws Exception {
        long reportId = createReportThroughApi();

        mockMvc.perform(patch("/analyst/reports/{id}/status", reportId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forbiddenStatusTransitionFailsAndKeepsPreviousState() throws Exception {
        long reportId = createReportThroughApi();

        mockMvc.perform(patch("/analyst/reports/{id}/status", reportId)
                        .with(csrf())
                        .header("Authorization", bearerToken(adminUsername))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED"
                                }
                                """))
                .andExpect(status().isBadRequest());

        assertThat(reportRepository.findById(reportId).orElseThrow().getStatus())
                .isEqualTo(ReportStatus.SUBMITTED);
    }

    @Test
    void userWithoutWorkflowRoleCannotChangeReportStatus() throws Exception {
        long reportId = createReportThroughApi();

        mockMvc.perform(patch("/analyst/reports/{id}/status", reportId)
                        .with(csrf())
                        .header("Authorization", bearerToken(auditorUsername))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "UNDER_REVIEW"
                                }
                                """))
                .andExpect(status().isForbidden());

        assertThat(reportRepository.findById(reportId).orElseThrow().getStatus())
                .isEqualTo(ReportStatus.SUBMITTED);
    }

    @Test
    void analystWhoDoesNotOwnCaseCannotChangeReportStatus() throws Exception {
        long reportId = createReportThroughApi();

        assignToCurrentAnalyst(reportId, analystUsername).andExpect(status().isOk());

        mockMvc.perform(patch("/analyst/reports/{id}/status", reportId)
                        .with(csrf())
                        .header("Authorization", bearerToken(otherAnalystUsername))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "UNDER_REVIEW"
                                }
                                """))
                .andExpect(status().isForbidden());

        assertThat(reportRepository.findById(reportId).orElseThrow().getStatus())
                .isEqualTo(ReportStatus.UNDER_REVIEW);
    }

    @Test
    void concurrentReportUpdatesAreRejectedByOptimisticLocking() {
        Report saved = createReportEntity("Concurrent report");

        Report firstCopy = reportRepository.findById(saved.getId()).orElseThrow();
        entityManager.detach(firstCopy);

        Report staleCopy = reportRepository.findById(saved.getId()).orElseThrow();
        entityManager.detach(staleCopy);

        firstCopy.setStatus(ReportStatus.UNDER_REVIEW);
        reportRepository.saveAndFlush(firstCopy);

        staleCopy.setStatus(ReportStatus.REJECTED);

        assertThatThrownBy(() -> reportRepository.saveAndFlush(staleCopy))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(reportRepository.findById(saved.getId()).orElseThrow().getStatus())
                .isEqualTo(ReportStatus.UNDER_REVIEW);
    }

    @Test
    void closedCaseWorkflowDataCannotBePartiallyModified() throws Exception {
        Report report = createReportEntity("Closed workflow case");
        report.setStatus(ReportStatus.RESOLVED);
        report = reportRepository.saveAndFlush(report);
        createCaseReview(report, analystUsername);

        mockMvc.perform(patch("/analyst/reports/{id}/notes", report.getId())
                        .with(csrf())
                        .header("Authorization", bearerToken(analystUsername))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notes": "This update must be rejected."
                                }
                                """))
                .andExpect(status().isBadRequest());

        CaseReview caseReview = caseReviewRepository.findByReportId(report.getId()).orElseThrow();
        assertThat(caseReview.getNotes()).isNull();
    }

    private String createUser(String username, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@ghostreport.test");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setActive(true);
        userRepository.save(user);
        return username;
    }

    private long createReportThroughApi() throws Exception {
        String response = mockMvc.perform(post("/reports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Business workflow report",
                                  "description": "Report used to verify business workflow controls.",
                                  "category": "Security"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("id").asLong();
    }

    private org.springframework.test.web.servlet.ResultActions assignToCurrentAnalyst(
            long reportId,
            String username
    ) throws Exception {
        return mockMvc.perform(post("/analyst/reports/{id}/assign", reportId)
                .with(csrf())
                .header("Authorization", bearerToken(username))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
    }

    private Report createReportEntity(String title) {
        Report report = new Report();
        report.setTitle(title);
        report.setDescription("Report used by business workflow tests.");
        report.setCategory("Security");
        report.setStatus(ReportStatus.SUBMITTED);
        report.setTrackingCodeHash("tracking-" + UUID.randomUUID());
        return reportRepository.saveAndFlush(report);
    }

    private void createCaseReview(Report report, String analystUsername) {
        User analyst = userRepository.findByUsername(analystUsername).orElseThrow();
        CaseReview caseReview = new CaseReview();
        caseReview.setReport(report);
        caseReview.setAssignedAnalyst(analyst);
        caseReview.setPriority(CasePriority.MEDIUM);
        caseReviewRepository.saveAndFlush(caseReview);
    }

    private String bearerToken(String username) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = response.replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
        return "Bearer " + token;
    }
}
