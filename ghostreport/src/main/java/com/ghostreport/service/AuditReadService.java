package com.ghostreport.service;

import com.ghostreport.dto.AuditClosedCaseResponse;
import com.ghostreport.model.CaseReview;
import com.ghostreport.model.Report;
import com.ghostreport.model.ReportStatus;
import com.ghostreport.repository.CaseReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditReadService {

    private final CaseReviewRepository caseReviewRepository;

    public AuditReadService(CaseReviewRepository caseReviewRepository) {
        this.caseReviewRepository = caseReviewRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditClosedCaseResponse> getClosedCaseHistory() {
        return caseReviewRepository.findAll()
                .stream()
                .filter(this::isClosedCase)
                .map(this::toClosedCaseResponse)
                .toList();
    }

    private boolean isClosedCase(CaseReview caseReview) {
        Report report = caseReview.getReport();
        return report != null &&
                (report.getStatus() == ReportStatus.RESOLVED || report.getStatus() == ReportStatus.REJECTED);
    }

    private AuditClosedCaseResponse toClosedCaseResponse(CaseReview caseReview) {
        Report report = caseReview.getReport();

        return new AuditClosedCaseResponse(
                report.getId(),
                caseReview.getId(),
                report.getStatus().name(),
                report.getCategory(),
                caseReview.getPriority() != null ? caseReview.getPriority().name() : null,
                caseReview.getAssignedAnalyst() != null ? caseReview.getAssignedAnalyst().getUsername() : null,
                report.getAttachments() != null ? report.getAttachments().size() : 0,
                report.getCreatedAt(),
                caseReview.getUpdatedAt()
        );
    }
}
