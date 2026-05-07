package com.ghostreport.controller;

import com.ghostreport.dto.*;
import com.ghostreport.service.CasePackageService;
import com.ghostreport.service.CaseReviewService;
import com.ghostreport.service.ReportService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/analyst")
public class AnalystController {

    private final ReportService reportService;
    private final CaseReviewService caseReviewService;
    private final CasePackageService casePackageService;

    public AnalystController(ReportService reportService, CaseReviewService caseReviewService, CasePackageService casePackageService) {
        this.reportService = reportService;
        this.caseReviewService = caseReviewService;
        this.casePackageService = casePackageService;
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
    public CaseReviewResponse assignAnalyst(@PathVariable Long id) {
        return caseReviewService.assignAnalystToCurrentUser(id);
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

    @GetMapping("/reports/{id}/attachments")
    public List<AttachmentListResponse> listAttachments(@PathVariable Long id) {
        return reportService.listAttachments(id);
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        return reportService.downloadAttachment(attachmentId);
    }

    @PostMapping("/reports/{id}/case-package")
    public ResponseEntity<CasePackageResponse> generateCasePackage(@PathVariable Long id) {
        return ResponseEntity.ok(casePackageService.generateCasePackage(id));
    }

}