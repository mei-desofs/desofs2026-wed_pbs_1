package com.ghostreport.controller;

import com.ghostreport.dto.AttachmentResponse;
import com.ghostreport.dto.CreateReportRequest;
import com.ghostreport.dto.CreateReportResponse;
import com.ghostreport.dto.ReportResponse;
import com.ghostreport.dto.VerifyTrackingCodeRequest;
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
    @ResponseStatus(HttpStatus.CREATED)
    public CreateReportResponse createReport(@Valid @RequestBody CreateReportRequest request) {
        return reportService.createReport(request);
    }

    @PostMapping("/{id}/verify")
    public ReportResponse verifyTrackingCode(
            @PathVariable Long id,
            @Valid @RequestBody VerifyTrackingCodeRequest request
    ) {
        return reportService.verifyTrackingCode(id, request.getTrackingCode());
    }

    @PostMapping("/{id}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        return reportService.uploadAttachment(id, file);
    }
}