package com.ghostreport.security;

import com.ghostreport.model.*;
import com.ghostreport.repository.AuditLogRepository;
import com.ghostreport.repository.CaseReviewRepository;
import com.ghostreport.repository.ReportRepository;
import com.ghostreport.repository.UserRepository;
import com.ghostreport.service.CasePackageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuditLogSecurityTest {

    @Autowired
    private CasePackageService casePackageService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private CaseReviewRepository caseReviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "analyst_audit", roles = "ANALYST")
    void criticalOperationShouldCreateAuditLog() {
        auditLogRepository.deleteAll();

        User analyst = createUser("analyst_audit", UserRole.ANALYST);
        Report report = createReport(ReportStatus.RESOLVED);
        createCaseReview(report, analyst);

        casePackageService.generateCasePackage(report.getId());

        assertThat(auditLogRepository.findAll())
                .anyMatch(log ->
                        "CASE_PACKAGE_GENERATED".equals(log.getAction()) &&
                                "REPORT".equals(log.getTargetType()) &&
                                report.getId().equals(log.getTargetId())
                );
    }

    @Test
    @WithMockUser(username = "unauthorized_user", roles = "REPORTER")
    void unauthorizedUserShouldNotGenerateCasePackage() {
        auditLogRepository.deleteAll();

        User analyst = createUser("analyst_forbidden", UserRole.ANALYST);
        Report report = createReport(ReportStatus.RESOLVED);
        createCaseReview(report, analyst);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> casePackageService.generateCasePackage(report.getId())
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(auditLogRepository.findAll())
                .noneMatch(log ->
                        "CASE_PACKAGE_GENERATED".equals(log.getAction()) &&
                                report.getId().equals(log.getTargetId())
                );
    }

    private Report createReport(ReportStatus status) {
        Report report = new Report();
        report.setTitle("Audit test");
        report.setDescription("Descrição usada para testar audit logging.");
        report.setCategory("Security");
        report.setStatus(status);
        report.setTrackingCodeHash("hash-audit-test-" + System.nanoTime());
        return reportRepository.save(report);
    }

    private User createUser(String username, UserRole role) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User user = new User();
                    user.setUsername(username);
                    user.setEmail(username + "@ghostreport.test");
                    user.setPasswordHash("fake-hash");
                    user.setRole(role);
                    user.setActive(true);
                    return userRepository.save(user);
                });
    }

    private void createCaseReview(Report report, User analyst) {
        CaseReview caseReview = new CaseReview();
        caseReview.setReport(report);
        caseReview.setAssignedAnalyst(analyst);
        caseReview.setPriority(CasePriority.MEDIUM);
        caseReview.setNotes("Notas internas.");
        caseReviewRepository.save(caseReview);
    }
}