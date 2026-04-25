package com.ghostreport.service;

import com.ghostreport.dto.AttachmentListResponse;
import com.ghostreport.dto.AttachmentResponse;
import com.ghostreport.dto.CreateReportRequest;
import com.ghostreport.dto.CreateReportResponse;
import com.ghostreport.dto.ReportResponse;
import com.ghostreport.dto.UpdateReportStatusRequest;
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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ReportService(
            ReportRepository reportRepository,
            AttachmentRepository attachmentRepository,
            FileStorageService fileStorageService,
            CaseReviewRepository caseReviewRepository,
            AuditLogService auditLogService
    ) {
        this.reportRepository = reportRepository;
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService = fileStorageService;
        this.caseReviewRepository = caseReviewRepository;
        this.auditLogService = auditLogService;
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

        fileStorageService.generateReportDocument(
                saved.getId(),
                saved.getDescription(),
                saved.getCategory(),
                saved.getStatus().name()
        );

        logger.info("Report created with id={}", saved.getId());
        auditLogService.log("REPORT_CREATED", "REPORT", saved.getId(), "Anonymous report created");

        return new CreateReportResponse(
                saved.getId(),
                saved.getStatus().name(),
                trackingCode
        );
    }

    public ReportResponse getReport(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        return toReportResponse(report);
    }

    public ReportResponse verifyTrackingCode(Long id, String trackingCode) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        boolean matches = passwordEncoder.matches(trackingCode, report.getTrackingCodeHash());

        if (!matches) {
            logger.warn("Invalid tracking code attempt for report id={}", id);
            auditLogService.log("TRACKING_CODE_FAILED", "REPORT", id, "Invalid tracking code attempt");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid tracking code");
        }

        logger.info("Tracking code verified successfully for report id={}", report.getId());
        auditLogService.log("TRACKING_CODE_VERIFIED", "REPORT", report.getId(), "Tracking code verified successfully");

        return toReportResponse(report);
    }

    public AttachmentResponse uploadAttachment(Long reportId, MultipartFile file) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

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

        logger.info("Attachment uploaded for report id={}, attachment id={}", reportId, saved.getId());
        auditLogService.log("ATTACHMENT_UPLOADED", "ATTACHMENT", saved.getId(), "Attachment uploaded for report " + reportId);

        return new AttachmentResponse(
                saved.getId(),
                saved.getOriginalName(),
                saved.getMimeType(),
                saved.getSize()
        );
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
            ReportStatus newStatus = ReportStatus.valueOf(request.getStatus().toUpperCase());
            report.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }

        Report saved = reportRepository.save(report);

        logger.info("Report id={} status updated to {}", saved.getId(), saved.getStatus());
        auditLogService.log("REPORT_STATUS_UPDATED", "REPORT", saved.getId(), "Status updated to " + saved.getStatus());

        return toReportResponse(saved);
    }

    public List<AttachmentListResponse> listAttachments(Long reportId) {
        checkInternalAccessToReport(reportId);

        return attachmentRepository.findByReportId(reportId).stream()
                .map(attachment -> new AttachmentListResponse(
                        attachment.getId(),
                        attachment.getOriginalName(),
                        attachment.getMimeType(),
                        attachment.getSize()
                ))
                .toList();
    }

    public ResponseEntity<Resource> downloadAttachment(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));

        Long reportId = attachment.getReport().getId();

        checkInternalAccessToReport(reportId);

        Resource resource = fileStorageService.loadFileAsResource(attachment.getStoragePath());

        logger.info("Attachment id={} downloaded for report id={}", attachmentId, reportId);
        auditLogService.log("ATTACHMENT_DOWNLOADED", "ATTACHMENT", attachmentId, "Attachment downloaded for report " + reportId);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(attachment.getOriginalName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
    }

    private void checkInternalAccessToReport(Long reportId) {
        if (SecurityUtils.hasRole("ADMIN")) {
            return;
        }

        CaseReview caseReview = caseReviewRepository.findByReportId(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No case review assigned"));

        String currentUsername = SecurityUtils.getCurrentUsername();

        if (caseReview.getAssignedAnalyst() == null ||
                !caseReview.getAssignedAnalyst().getUsername().equals(currentUsername)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this case");
        }
    }

    private ReportResponse toReportResponse(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getDescription(),
                report.getCategory(),
                report.getStatus().name()
        );
    }

    private String generateTrackingCode() {
        int length = 16;
        StringBuilder code = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            code.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }

        return code.toString();
    }
}