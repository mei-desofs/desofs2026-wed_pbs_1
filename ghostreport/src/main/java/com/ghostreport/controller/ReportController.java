package com.ghostreport.controller;

import com.ghostreport.dto.*;
import com.ghostreport.service.ReportService;
import com.ghostreport.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
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
    private final int maxFilesPerRequest;

    public ReportController(ReportService reportService,
                            RateLimiterService rateLimiterService,
                            @Value("${app.upload.max-files-per-request:5}") int maxFilesPerRequest) {
        this.reportService = reportService;
        this.rateLimiterService = rateLimiterService;
        this.maxFilesPerRequest = maxFilesPerRequest;
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

        rateLimiterService.checkTrackingLimit(ip);

        return reportService.verifyTrackingCodeOnly(request.getTrackingCode());
    }

    @PostMapping("/{id}/attachments")
    public List<AttachmentResponse> uploadAttachments(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(name = "trackingCode", required = false) String trackingCode,
            HttpServletRequest httpRequest
    ) {
        String ip = httpRequest.getRemoteAddr();

        rateLimiterService.checkUploadLimit(ip);

        if (files == null || files.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhum ficheiro enviado");
        }

        if (files.length > maxFilesPerRequest) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many files in one request");
        }

        return reportService.uploadMultipleAttachments(id, files, trackingCode);
    }

    @PostMapping("/{id}/attachments/list")
    public List<AttachmentListResponse> listAttachments(
            @PathVariable Long id,
            @Valid @RequestBody VerifyTrackingCodeRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = httpRequest.getRemoteAddr();

        rateLimiterService.checkTrackingLimit(ip);

        return reportService.listAttachmentsSecure(id, request.getTrackingCode());
    }

    @PostMapping("/download")
    public ResponseEntity<Resource> downloadAttachment(
            @Valid @RequestBody DownloadRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = httpRequest.getRemoteAddr();

        rateLimiterService.checkDownloadLimit(ip);

        if (request.getAttachmentId() == null || request.getTrackingCode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados inválidos");
        }

        return reportService.downloadAttachmentSecure(
                request.getAttachmentId(),
                request.getTrackingCode()
        );
    }
}
