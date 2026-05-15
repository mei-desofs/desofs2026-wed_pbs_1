package com.ghostreport.controller;

import com.ghostreport.dto.*;
import com.ghostreport.service.ReportService;
import com.ghostreport.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
    public CreateReportResponse createReport(@Valid @RequestBody CreateReportRequest request) {
        return reportService.createReport(request);
    }

    @PostMapping("/verify")
    public ReportResponse verifyTrackingCodeOnly(
            @Valid @RequestBody VerifyTrackingCodeRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = httpRequest.getRemoteAddr();

        rateLimiterService.checkLimit(ip + "_VERIFY");

        return reportService.verifyTrackingCodeOnly(request.getTrackingCode());
    }

    @PostMapping("/{id}/attachments")
    public List<AttachmentResponse> uploadAttachments(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("trackingCode") String trackingCode,
            HttpServletRequest httpRequest
    ) {
        String ip = httpRequest.getRemoteAddr();

        rateLimiterService.checkLimit(ip + "_UPLOAD");

        if (files == null || files.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhum ficheiro enviado");
        }

        return reportService.uploadMultipleAttachments(id, files, trackingCode);
    }

    @PostMapping("/download")
    public ResponseEntity<Resource> downloadAttachment(
            @Valid @RequestBody DownloadRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = httpRequest.getRemoteAddr();

        rateLimiterService.checkLimit(ip + "_DOWNLOAD");

        if (request.getAttachmentId() == null || request.getTrackingCode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados inválidos");
        }

        return reportService.downloadAttachmentSecure(
                request.getAttachmentId(),
                request.getTrackingCode()
        );
    }
}
