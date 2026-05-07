package com.ghostreport.security;

import com.ghostreport.dto.UpdateNotesRequest;
import com.ghostreport.model.CaseReview;
import com.ghostreport.model.Report;
import com.ghostreport.model.ReportStatus;
import com.ghostreport.repository.CaseReviewRepository;
import com.ghostreport.repository.ReportRepository;
import com.ghostreport.service.CaseReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class ClosedCaseSecurityTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private CaseReviewRepository caseReviewRepository;

    @Autowired
    private CaseReviewService caseReviewService;

    @Test
    void closedCaseCannotBeModified() {

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        Collections.emptyList()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        Report report = new Report();

        report.setTitle("Test");
        report.setDescription("Test");
        report.setCategory("Test");
        report.setStatus(ReportStatus.RESOLVED);

        report.setTrackingCodeHash(UUID.randomUUID().toString());

        Report savedReport = reportRepository.save(report);

        CaseReview review = new CaseReview();

        review.setReport(savedReport);

        caseReviewRepository.save(review);

        UpdateNotesRequest request = new UpdateNotesRequest();

        request.setNotes("Malicious update");

        assertThrows(
                ResponseStatusException.class,
                () -> caseReviewService.updateNotes(savedReport.getId(), request)
        );
    }
}