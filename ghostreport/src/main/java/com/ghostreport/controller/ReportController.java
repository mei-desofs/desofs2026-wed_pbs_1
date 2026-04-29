package com.ghostreport.controller;

import com.ghostreport.dto.*;
import com.ghostreport.service.ReportService;
import com.ghostreport.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final RateLimiterService rateLimiterService;

    public ReportController(ReportService reportService,
                            RateLimiterService rateLimiterService) {
        this.reportService = reportService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping
    public CreateReportResponse createReport(@RequestBody CreateReportRequest request) {
        return reportService.createReport(request);
    }

    @PostMapping("/verify")
    public ReportResponse verifyTrackingCodeOnly(
            @RequestBody VerifyTrackingCodeRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = httpRequest.getRemoteAddr();

        rateLimiterService.checkLimit(ip + "_VERIFY");

        return reportService.verifyTrackingCodeOnly(request.getTrackingCode());
    }

    @PostMapping("/{id}/attachments")
    public AttachmentResponse uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest
    ) {
        String ip = httpRequest.getRemoteAddr();

        rateLimiterService.checkLimit(ip + "_UPLOAD");

        return reportService.uploadAttachment(id, file);
    }

    @PostMapping("/download")
    public ResponseEntity<Resource> downloadAttachment(
            @RequestBody DownloadRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = httpRequest.getRemoteAddr();

        rateLimiterService.checkLimit(ip + "_DOWNLOAD");

        return reportService.downloadAttachmentSecure(
                request.getAttachmentId(),
                request.getTrackingCode()
        );
    }
}