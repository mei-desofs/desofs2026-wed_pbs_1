package com.ghostreport.controller;

import com.ghostreport.dto.*;
import com.ghostreport.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public CreateReportResponse createReport(@RequestBody CreateReportRequest request) {
        return reportService.createReport(request);
    }

    @PostMapping("/verify")
    public ReportResponse verifyTrackingCodeOnly(@RequestBody VerifyTrackingCodeRequest request) {
        return reportService.verifyTrackingCodeOnly(request.getTrackingCode());
    }

    @PostMapping("/{id}/attachments")
    public AttachmentResponse uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        return reportService.uploadAttachment(id, file);
    }
}