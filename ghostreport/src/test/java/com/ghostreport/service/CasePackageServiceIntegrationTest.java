package com.ghostreport.service;

import com.ghostreport.model.*;
import com.ghostreport.repository.AttachmentRepository;
import com.ghostreport.repository.CaseReviewRepository;
import com.ghostreport.repository.ReportRepository;
import com.ghostreport.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CasePackageServiceIntegrationTest {

    @Autowired
    private CasePackageService casePackageService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private CaseReviewRepository caseReviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Test
    @WithMockUser(username = "analyst", roles = "ANALYST")
    void shouldNotGeneratePackageForOpenCase() {
        Report report = createReport(ReportStatus.UNDER_REVIEW);
        User analyst = createAnalyst("analyst");
        createCaseReview(report, analyst);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> casePackageService.generateCasePackage(report.getId())
        );

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    @WithMockUser(username = "analyst", roles = "ANALYST")
    void shouldGeneratePackageForResolvedCase() throws Exception {
        Report report = createReport(ReportStatus.RESOLVED);
        User analyst = createAnalyst("analyst");
        createCaseReview(report, analyst);

        casePackageService.generateCasePackage(report.getId());

        Path packagePath = Path.of("uploads", "reports", String.valueOf(report.getId()), "case_package");

        assertTrue(Files.exists(packagePath));
        assertTrue(Files.exists(packagePath.resolve("case_summary.txt")));
        assertTrue(Files.exists(packagePath.resolve("evidence_manifest.json")));
        assertTrue(Files.exists(packagePath.resolve("integrity_hashes.txt")));
        assertTrue(Files.exists(packagePath.resolve("attachments")));
    }

    @Test
    @WithMockUser(username = "otherAnalyst", roles = "ANALYST")
    void shouldNotGeneratePackageForCaseAssignedToAnotherAnalyst() {
        Report report = createReport(ReportStatus.RESOLVED);
        User analyst = createAnalyst("analyst");
        createAnalyst("otherAnalyst");
        createCaseReview(report, analyst);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> casePackageService.generateCasePackage(report.getId())
        );

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminShouldGeneratePackageEvenIfNotAssigned() throws Exception {
        Report report = createReport(ReportStatus.REJECTED);
        User analyst = createAnalyst("analyst");
        createAdmin("admin");
        createCaseReview(report, analyst);

        casePackageService.generateCasePackage(report.getId());

        Path packagePath = Path.of("uploads", "reports", String.valueOf(report.getId()), "case_package");

        assertTrue(Files.exists(packagePath.resolve("case_summary.txt")));
        assertTrue(Files.exists(packagePath.resolve("evidence_manifest.json")));
        assertTrue(Files.exists(packagePath.resolve("integrity_hashes.txt")));
    }

    private Report createReport(ReportStatus status) {
        Report report = new Report();
        report.setTitle("Teste");
        report.setCategory("Fraude");
        report.setDescription("Descrição de teste para geração de pacote de evidências.");
        report.setStatus(status);
        report.setTrackingCodeHash("hash-teste-" + System.nanoTime());

        return reportRepository.save(report);
    }

    private User createAnalyst(String username) {
        return createUser(username, UserRole.ANALYST);
    }

    private User createAdmin(String username) {
        return createUser(username, UserRole.ADMIN);
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

    private CaseReview createCaseReview(Report report, User analyst) {
        CaseReview caseReview = new CaseReview();
        caseReview.setReport(report);
        caseReview.setAssignedAnalyst(analyst);
        caseReview.setPriority(CasePriority.MEDIUM);
        caseReview.setNotes("Notas internas de teste.");

        return caseReviewRepository.save(caseReview);
    }
}