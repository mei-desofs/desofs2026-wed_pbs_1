package com.ghostreport.controller;

import com.ghostreport.dto.AssignAnalystRequest;
import com.ghostreport.dto.CaseReviewResponse;
import com.ghostreport.dto.ReportResponse;
import com.ghostreport.dto.UpdateNotesRequest;
import com.ghostreport.dto.UpdatePriorityRequest;
import com.ghostreport.dto.UpdateReportStatusRequest;
import com.ghostreport.service.CaseReviewService;
import com.ghostreport.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/analyst")
public class AnalystController {

    private final ReportService reportService;
    private final CaseReviewService caseReviewService;

    public AnalystController(ReportService reportService, CaseReviewService caseReviewService) {
        this.reportService = reportService;
        this.caseReviewService = caseReviewService;
    }

    @GetMapping("/panel")
    public String analystPanel() {
        return "Access granted: ANALYST or ADMIN";
    }

    @GetMapping("/reports")
    public List<ReportResponse> getAllReports() {
        return reportService.getAllReports();
    }

    @PatchMapping("/reports/{id}/status")
    public ReportResponse updateReportStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReportStatusRequest request
    ) {
        return reportService.updateReportStatus(id, request);
    }

    @PostMapping("/reports/{id}/assign")
    public CaseReviewResponse assignAnalyst(
            @PathVariable Long id,
            @Valid @RequestBody AssignAnalystRequest request
    ) {
        return caseReviewService.assignAnalyst(id, request);
    }

    @PatchMapping("/reports/{id}/priority")
    public CaseReviewResponse updatePriority(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePriorityRequest request
    ) {
        return caseReviewService.updatePriority(id, request);
    }

    @PatchMapping("/reports/{id}/notes")
    public CaseReviewResponse updateNotes(
            @PathVariable Long id,
            @Valid @RequestBody UpdateNotesRequest request
    ) {
        return caseReviewService.updateNotes(id, request);
    }

    @GetMapping("/reports/{id}/case-review")
    public CaseReviewResponse getCaseReview(@PathVariable Long id) {
        return caseReviewService.getCaseReview(id);
    }

    @GetMapping("/my-cases")
    public List<CaseReviewResponse> getMyAssignedCases() {
        return caseReviewService.getMyAssignedCases();
    }
}