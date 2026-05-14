package com.ghostreport.service;

import com.ghostreport.dto.AssignAnalystRequest;
import com.ghostreport.dto.CaseReviewResponse;
import com.ghostreport.dto.UpdateNotesRequest;
import com.ghostreport.dto.UpdatePriorityRequest;
import com.ghostreport.model.CasePriority;
import com.ghostreport.model.CaseReview;
import com.ghostreport.model.Report;
import com.ghostreport.model.ReportStatus;
import com.ghostreport.model.User;
import com.ghostreport.model.UserRole;
import com.ghostreport.repository.CaseReviewRepository;
import com.ghostreport.repository.ReportRepository;
import com.ghostreport.repository.UserRepository;
import com.ghostreport.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CaseReviewService {

    private static final Logger logger = LoggerFactory.getLogger(CaseReviewService.class);

    private final CaseReviewRepository caseReviewRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public CaseReviewService(
            CaseReviewRepository caseReviewRepository,
            ReportRepository reportRepository,
            UserRepository userRepository,
            AuditLogService auditLogService
    ) {
        this.caseReviewRepository = caseReviewRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public CaseReviewResponse assignAnalyst(Long reportId, AssignAnalystRequest request) {

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Report not found"
                        )
                );

        String currentUsername = SecurityUtils.getCurrentUsername();

        User analyst = userRepository.findByUsername(currentUsername)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Analyst not found"
                        )
                );

        if (analyst.getRole() != UserRole.ANALYST) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Current user is not an analyst"
            );
        }

        CaseReview caseReview = caseReviewRepository.findByReportId(reportId)
                .orElseGet(() -> {
                    CaseReview newCaseReview = new CaseReview();
                    newCaseReview.setReport(report);
                    return newCaseReview;
                });

        validateCaseCanBeAssignedTo(caseReview, analyst);

        caseReview.setAssignedAnalyst(analyst);

        if (caseReview.getPriority() == null) {
            caseReview.setPriority(CasePriority.MEDIUM);
        }

        CaseReview saved = caseReviewRepository.save(caseReview);

        auditLogService.log(
                "REPORT_ASSIGNED",
                "REPORT",
                reportId,
                "Assigned to analyst username " + analyst.getUsername()
        );

        logger.info(
                "Report id={} assigned to analyst username={}",
                reportId,
                analyst.getUsername()
        );

        return toResponse(saved);
    }

    public CaseReviewResponse assignAnalystToCurrentUser(Long reportId) {

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Report not found"
                        )
                );

        String currentUsername = SecurityUtils.getCurrentUsername();

        User analyst = userRepository.findByUsername(currentUsername)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Analyst not found"
                        )
                );

        if (analyst.getRole() != UserRole.ANALYST) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Current user is not an analyst"
            );
        }

        CaseReview caseReview = caseReviewRepository.findByReportId(reportId)
                .orElseGet(() -> {
                    CaseReview newCaseReview = new CaseReview();
                    newCaseReview.setReport(report);
                    return newCaseReview;
                });

        validateCaseCanBeAssignedTo(caseReview, analyst);

        caseReview.setAssignedAnalyst(analyst);

        if (caseReview.getPriority() == null) {
            caseReview.setPriority(CasePriority.MEDIUM);
        }

        CaseReview saved = caseReviewRepository.save(caseReview);

        auditLogService.log(
                "REPORT_ASSIGNED",
                "REPORT",
                reportId,
                "Assigned to authenticated analyst"
        );

        return toResponse(saved);
    }

    public CaseReviewResponse updatePriority(Long reportId, UpdatePriorityRequest request) {

        CaseReview caseReview = getAccessibleCaseReview(reportId);

        validateCaseIsEditable(caseReview);

        try {

            CasePriority priority =
                    CasePriority.valueOf(request.getPriority().toUpperCase());

            caseReview.setPriority(priority);

        } catch (IllegalArgumentException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid priority"
            );
        }

        CaseReview saved = caseReviewRepository.save(caseReview);

        auditLogService.log(
                "CASE_PRIORITY_UPDATED",
                "REPORT",
                reportId,
                "Priority updated to " + saved.getPriority()
        );

        logger.info(
                "Priority updated for report id={} to {}",
                reportId,
                saved.getPriority()
        );

        return toResponse(saved);
    }

    public CaseReviewResponse updateNotes(Long reportId, UpdateNotesRequest request) {

        CaseReview caseReview = getAccessibleCaseReview(reportId);

        validateCaseIsEditable(caseReview);

        caseReview.setNotes(request.getNotes());

        CaseReview saved = caseReviewRepository.save(caseReview);

        auditLogService.log(
                "CASE_NOTES_UPDATED",
                "REPORT",
                reportId,
                "Internal notes updated"
        );

        logger.info("Notes updated for report id={}", reportId);

        return toResponse(saved);
    }

    public CaseReviewResponse getCaseReview(Long reportId) {

        CaseReview caseReview = getAccessibleCaseReview(reportId);

        return toResponse(caseReview);
    }

    public List<CaseReviewResponse> getMyAssignedCases() {

        String username = SecurityUtils.getCurrentUsername();

        return caseReviewRepository.findByAssignedAnalystUsername(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CaseReview getAccessibleCaseReview(Long reportId) {

        CaseReview caseReview = caseReviewRepository.findByReportId(reportId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Case review not found"
                        )
                );

        if (SecurityUtils.hasRole("ADMIN")) {
            return caseReview;
        }

        String currentUsername = SecurityUtils.getCurrentUsername();

        if (caseReview.getAssignedAnalyst() == null ||
                !caseReview.getAssignedAnalyst()
                        .getUsername()
                        .equals(currentUsername)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have access to this case"
            );
        }

        return caseReview;
    }

    private void validateCaseIsEditable(CaseReview caseReview) {

        ReportStatus status = caseReview.getReport().getStatus();

        if (status == ReportStatus.RESOLVED ||
                status == ReportStatus.REJECTED) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Closed cases cannot be modified"
            );
        }
    }

    private void validateCaseCanBeAssignedTo(CaseReview caseReview, User analyst) {

        if (caseReview.getAssignedAnalyst() == null) {
            return;
        }

        if (caseReview.getAssignedAnalyst().getUsername().equals(analyst.getUsername())) {
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Case is already assigned to another analyst"
        );
    }

    private CaseReviewResponse toResponse(CaseReview caseReview) {

        return new CaseReviewResponse(
                caseReview.getReport().getId(),
                caseReview.getId(),
                caseReview.getAssignedAnalyst() != null
                        ? caseReview.getAssignedAnalyst().getUsername()
                        : null,
                caseReview.getPriority() != null
                        ? caseReview.getPriority().name()
                        : null,
                caseReview.getNotes(),
                caseReview.getReport().getStatus().name()
        );
    }
}
