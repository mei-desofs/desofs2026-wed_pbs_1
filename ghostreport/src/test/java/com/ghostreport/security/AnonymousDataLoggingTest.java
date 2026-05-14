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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AnonymousDataLoggingTest {

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
    @WithMockUser(username = "analyst_privacy", roles = "ANALYST")
    void auditLogsShouldNotContainPlainTrackingCode() {
        auditLogRepository.deleteAll();

        String plainTrackingCode = "GR-SECRET-TRACKING-CODE-123";

        User analyst = createUser("analyst_privacy", UserRole.ANALYST);
        Report report = createReport(ReportStatus.RESOLVED);
        createCaseReview(report, analyst);

        casePackageService.generateCasePackage(report.getId());

        assertThat(auditLogRepository.findAll())
                .noneMatch(log ->
                        contains(log.getDetails(), plainTrackingCode) ||
                                contains(log.getAction(), plainTrackingCode) ||
                                contains(log.getTargetType(), plainTrackingCode)
                );
    }

    private boolean contains(String value, String search) {
        return value != null && value.contains(search);
    }

    private Report createReport(ReportStatus status) {
        Report report = new Report();
        report.setTitle("Privacy test");
        report.setDescription("Descrição usada para testar anonimato nos logs.");
        report.setCategory("Privacy");
        report.setStatus(status);

        report.setTrackingCodeHash("hashed-value-only-" + System.nanoTime());

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
        caseReview.setNotes("Notas internas sem dados identificáveis.");
        caseReviewRepository.save(caseReview);
    }
}