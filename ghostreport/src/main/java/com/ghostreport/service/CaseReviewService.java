package com.ghostreport.service;

import com.ghostreport.dto.AssignAnalystRequest;
import com.ghostreport.dto.CaseReviewResponse;
import com.ghostreport.dto.UpdateNotesRequest;
import com.ghostreport.dto.UpdatePriorityRequest;
import com.ghostreport.model.CasePriority;
import com.ghostreport.model.CaseReview;
import com.ghostreport.model.Report;
import com.ghostreport.model.User;
import com.ghostreport.model.UserRole;
import com.ghostreport.repository.CaseReviewRepository;
import com.ghostreport.repository.ReportRepository;
import com.ghostreport.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CaseReviewService {

    private static final Logger logger = LoggerFactory.getLogger(CaseReviewService.class);

    private final CaseReviewRepository caseReviewRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    public CaseReviewService(
            CaseReviewRepository caseReviewRepository,
            ReportRepository reportRepository,
            UserRepository userRepository
    ) {
        this.caseReviewRepository = caseReviewRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
    }

    public CaseReviewResponse assignAnalyst(Long reportId, AssignAnalystRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        User analyst = userRepository.findById(request.getAnalystId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Analyst not found"));

        if (analyst.getRole() != UserRole.ANALYST) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is not an analyst");
        }

        CaseReview caseReview = caseReviewRepository.findByReportId(reportId)
                .orElseGet(() -> {
                    CaseReview newCaseReview = new CaseReview();
                    newCaseReview.setReport(report);
                    return newCaseReview;
                });

        caseReview.setAssignedAnalyst(analyst);

        if (caseReview.getPriority() == null) {
            caseReview.setPriority(CasePriority.MEDIUM);
        }

        CaseReview saved = caseReviewRepository.save(caseReview);

        logger.info("Report id={} assigned to analyst id={}", reportId, analyst.getId());

        return toResponse(saved);
    }

    public CaseReviewResponse updatePriority(Long reportId, UpdatePriorityRequest request) {
        CaseReview caseReview = caseReviewRepository.findByReportId(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case review not found"));

        try {
            CasePriority priority = CasePriority.valueOf(request.getPriority().toUpperCase());
            caseReview.setPriority(priority);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid priority");
        }

        CaseReview saved = caseReviewRepository.save(caseReview);

        logger.info("Priority updated for report id={} to {}", reportId, saved.getPriority());

        return toResponse(saved);
    }

    public CaseReviewResponse updateNotes(Long reportId, UpdateNotesRequest request) {
        CaseReview caseReview = caseReviewRepository.findByReportId(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case review not found"));

        caseReview.setNotes(request.getNotes());

        CaseReview saved = caseReviewRepository.save(caseReview);

        logger.info("Notes updated for report id={}", reportId);

        return toResponse(saved);
    }

    public CaseReviewResponse getCaseReview(Long reportId) {
        CaseReview caseReview = caseReviewRepository.findByReportId(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case review not found"));

        return toResponse(caseReview);
    }

    private CaseReviewResponse toResponse(CaseReview caseReview) {
        return new CaseReviewResponse(
                caseReview.getReport().getId(),
                caseReview.getId(),
                caseReview.getAssignedAnalyst() != null ? caseReview.getAssignedAnalyst().getUsername() : null,
                caseReview.getPriority() != null ? caseReview.getPriority().name() : null,
                caseReview.getNotes(),
                caseReview.getReport().getStatus().name()
        );
    }
}