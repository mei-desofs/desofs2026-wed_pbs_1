package com.ghostreport.service;

import com.ghostreport.dto.*;
import com.ghostreport.model.Attachment;
import com.ghostreport.model.CaseReview;
import com.ghostreport.model.Report;
import com.ghostreport.model.ReportStatus;
import com.ghostreport.repository.AttachmentRepository;
import com.ghostreport.repository.CaseReviewRepository;
import com.ghostreport.repository.ReportRepository;
import com.ghostreport.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;

@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ReportRepository reportRepository;
    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final CaseReviewRepository caseReviewRepository;
    private final AuditLogService auditLogService;
    private final SecurityMonitoringService securityMonitoringService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ReportService(
            ReportRepository reportRepository,
            AttachmentRepository attachmentRepository,
            FileStorageService fileStorageService,
            CaseReviewRepository caseReviewRepository,
            AuditLogService auditLogService,
            SecurityMonitoringService securityMonitoringService
    ) {
        this.reportRepository = reportRepository;
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService = fileStorageService;
        this.caseReviewRepository = caseReviewRepository;
        this.auditLogService = auditLogService;
        this.securityMonitoringService = securityMonitoringService;
    }

    public CreateReportResponse createReport(CreateReportRequest request) {

        String trackingCode = generateTrackingCode();
        String trackingCodeHash = passwordEncoder.encode(trackingCode);

        Report report = new Report();
        report.setDescription(request.getDescription());
        report.setCategory(request.getCategory());
        report.setStatus(ReportStatus.SUBMITTED);
        report.setTrackingCodeHash(trackingCodeHash);

        Report saved = reportRepository.save(report);

        logger.info("Report created with id={}", saved.getId());
        auditLogService.log("REPORT_CREATED", "REPORT", saved.getId(), "Anonymous report created");

        return new CreateReportResponse(
                saved.getId(),
                saved.getStatus().name(),
                trackingCode
        );
    }

    public ReportResponse verifyTrackingCodeOnly(String trackingCode) {

        if (trackingCode == null || trackingCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tracking code is required");
        }

        trackingCode = trackingCode.trim();

        List<Report> reports = reportRepository.findAll();

        for (Report report : reports) {
            String stored = report.getTrackingCodeHash();

            boolean matches = passwordEncoder.matches(trackingCode, stored);

            if (matches) {
                auditLogService.log(
                        "TRACKING_CODE_VERIFIED",
                        "REPORT",
                        report.getId(),
                        "Tracking code verified successfully"
                );

                return toReportResponse(report);
            }
        }

        securityMonitoringService.recordFailedTrackingCode(null);

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Denúncia não encontrada");
    }

    public List<ReportResponse> getAllReports() {
        return reportRepository.findAll().stream()
                .map(this::toReportResponse)
                .toList();
    }

    public ReportResponse updateReportStatus(Long id, UpdateReportStatusRequest request) {

        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        checkInternalAccessToReport(id);

        try {
            report.setStatus(ReportStatus.valueOf(request.getStatus().toUpperCase()));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }

        Report saved = reportRepository.save(report);
        return toReportResponse(saved);
    }

    public AttachmentResponse uploadAttachment(Long reportId, MultipartFile file) {

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        try {
            FileStorageService.StoredFileInfo stored = fileStorageService.storeAttachment(reportId, file);

            Attachment attachment = new Attachment();
            attachment.setOriginalName(stored.originalName());
            attachment.setStoredName(stored.storedName());
            attachment.setFileReference(stored.fileReference());
            attachment.setStoragePath(stored.storagePath());
            attachment.setMimeType(stored.mimeType());
            attachment.setSize(stored.size());
            attachment.setHash(stored.hash());
            attachment.setReport(report);

            Attachment saved = attachmentRepository.save(attachment);

            logger.info("Attachment saved id={} for report={}", saved.getId(), reportId);

            return new AttachmentResponse(
                    saved.getId(),
                    saved.getOriginalName(),
                    saved.getMimeType(),
                    saved.getSize()
            );

        } catch (Exception e) {
            e.printStackTrace(); // 🔥 MOSTRA ERRO REAL
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao guardar ficheiro");
        }
    }

    public List<AttachmentListResponse> listAttachments(Long reportId) {

        checkInternalAccessToReport(reportId);

        return attachmentRepository.findByReportId(reportId).stream()
                .map(a -> new AttachmentListResponse(
                        a.getId(),
                        a.getOriginalName(),
                        a.getMimeType(),
                        a.getSize()
                ))
                .toList();
    }

    public ResponseEntity<Resource> downloadAttachment(Long attachmentId) {

        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Resource resource = fileStorageService.loadFileAsResource(attachment.getStoragePath());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(attachment.getOriginalName(), StandardCharsets.UTF_8)
                                .build().toString())
                .body(resource);
    }

    private void checkInternalAccessToReport(Long reportId) {

        if (SecurityUtils.hasRole("ADMIN")) return;

        CaseReview caseReview = caseReviewRepository.findByReportId(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));

        String currentUser = SecurityUtils.getCurrentUsername();

        if (caseReview.getAssignedAnalyst() == null ||
                !caseReview.getAssignedAnalyst().getUsername().equals(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private ReportResponse toReportResponse(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getStatus().name(),
                report.getCategory(),
                report.getDescription()
        );
    }

    private String generateTrackingCode() {
        int length = 16;
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < length; i++) {
            code.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }

        return code.toString();
    }
}