package com.ghostreport.service;

import com.ghostreport.domain.ReportDescription;
import com.ghostreport.domain.TrackingCode;
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
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportService {

    private static final Logger logger =
            LoggerFactory.getLogger(ReportService.class);

    private final ReportRepository reportRepository;
    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final CaseReviewRepository caseReviewRepository;
    private final AuditLogService auditLogService;
    private final SecurityMonitoringService securityMonitoringService;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

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

        TrackingCode trackingCode =
                TrackingCode.generate();

        String trackingCodeHash =
                passwordEncoder.encode(
                        trackingCode.value()
                );

        ReportDescription description =
                new ReportDescription(
                        request.getDescription()
                );

        Report report = new Report();

        report.setTitle(
                request.getTitle().trim()
        );

        report.setDescription(
                description.value()
        );

        report.setCategory(
                request.getCategory()
        );

        report.setStatus(
                ReportStatus.SUBMITTED
        );

        report.setTrackingCodeHash(
                trackingCodeHash
        );

        Report saved = reportRepository.save(report);

        fileStorageService.generateReportDocument(
                saved.getId(),
                saved.getDescription(),
                saved.getCategory(),
                saved.getStatus().name()
        );

        auditLogService.log(
                "REPORT_CREATED",
                "REPORT",
                saved.getId(),
                "Anonymous report created"
        );

        return new CreateReportResponse(
                saved.getId(),
                saved.getStatus().name(),
                trackingCode.value()
        );
    }

    public ReportResponse verifyTrackingCodeOnly(String trackingCode) {

        if (trackingCode == null || trackingCode.isBlank()) {
            securityMonitoringService.recordFailedTrackingCode(null);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Código inválido"
            );
        }

        trackingCode = trackingCode.trim();
        try {
            trackingCode = TrackingCode.from(trackingCode).value();
        } catch (IllegalArgumentException e) {
            securityMonitoringService.recordFailedTrackingCode(null);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Código inválido"
            );
        }

        for (Report report : reportRepository.findAll()) {

            if (passwordEncoder.matches(
                    trackingCode,
                    report.getTrackingCodeHash()
            )) {

                return toReportResponse(report);
            }
        }

        securityMonitoringService.recordFailedTrackingCode(null);

        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Denúncia não encontrada"
        );
    }

    public List<ReportResponse> getAllReports() {

        if (SecurityUtils.hasRole("ANALYST") && !SecurityUtils.hasRole("ADMIN")) {
            String currentUsername = SecurityUtils.getCurrentUsername();

            return reportRepository.findVisibleToAnalyst(currentUsername)
                    .stream()
                    .map(this::toReportResponse)
                    .toList();
        }

        return reportRepository.findAll()
                .stream()
                .map(this::toReportResponse)
                .toList();
    }

    public ReportResponse updateReportStatus(
            Long id,
            UpdateReportStatusRequest request
    ) {

        Report report = reportRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND
                        )
                );

        checkInternalAccessToReport(id);

        report.setStatus(
                ReportStatus.valueOf(
                        request.getStatus().toUpperCase()
                )
        );

        Report saved = reportRepository.save(report);

        return toReportResponse(saved);
    }

    public AttachmentResponse uploadAttachment(
            Long reportId,
            MultipartFile file,
            String trackingCode
    ) {

        logger.info("Attachment upload requested for report id={}", reportId);

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() ->
                {
                    recordUploadRejected(reportId, "Upload authorization failed");
                    return new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Upload not authorized"
                    );
                }
                );

        validateTrackingCodeForReport(report, trackingCode);

        try {

            FileStorageService.StoredFileInfo stored =
                    fileStorageService.storeAttachment(
                            reportId,
                            file
                    );

            Attachment attachment = new Attachment();

            attachment.setOriginalName(
                    stored.originalName()
            );

            attachment.setStoredName(
                    stored.storedName()
            );

            attachment.setFileReference(
                    stored.fileReference()
            );

            attachment.setStoragePath(
                    stored.storagePath()
            );

            attachment.setMimeType(
                    stored.mimeType()
            );

            attachment.setSize(
                    stored.size()
            );

            attachment.setHash(
                    stored.hash()
            );

            attachment.setReport(report);

            Attachment saved =
                    attachmentRepository.save(
                            attachment
                    );

            logger.info(
                    "Attachment saved id={} for report={}",
                    saved.getId(),
                    reportId
            );

            return new AttachmentResponse(
                    saved.getId(),
                    saved.getOriginalName(),
                    saved.getMimeType(),
                    saved.getSize()
            );

        } catch (ResponseStatusException e) {

            recordUploadRejected(reportId, e.getReason());
            throw e;

        } catch (Exception e) {

            logger.warn("Attachment upload failed for report id={}", reportId);

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro ao guardar ficheiro"
            );
        }
    }

    public List<AttachmentResponse> uploadMultipleAttachments(
            Long reportId,
            MultipartFile[] files,
            String trackingCode
    ) {

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() ->
                {
                    recordUploadRejected(reportId, "Upload authorization failed");
                    return new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Upload not authorized"
                    );
                }
                );

        validateTrackingCodeForReport(report, trackingCode);

        List<AttachmentResponse> responses =
                new ArrayList<>();

        for (MultipartFile file : files) {

            try {

                FileStorageService.StoredFileInfo stored =
                        fileStorageService.storeAttachment(
                                reportId,
                                file
                        );

                Attachment attachment = new Attachment();

                attachment.setOriginalName(
                        stored.originalName()
                );

                attachment.setStoredName(
                        stored.storedName()
                );

                attachment.setFileReference(
                        stored.fileReference()
                );

                attachment.setStoragePath(
                        stored.storagePath()
                );

                attachment.setMimeType(
                        stored.mimeType()
                );

                attachment.setSize(
                        stored.size()
                );

                attachment.setHash(
                        stored.hash()
                );

                attachment.setReport(report);

                Attachment saved =
                        attachmentRepository.save(
                                attachment
                        );

                responses.add(
                        new AttachmentResponse(
                                saved.getId(),
                                saved.getOriginalName(),
                                saved.getMimeType(),
                                saved.getSize()
                        )
                );

            } catch (ResponseStatusException e) {

                recordUploadRejected(reportId, e.getReason());
                throw e;

            } catch (Exception e) {

                logger.warn("Attachment upload failed for report id={}", reportId);

                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Erro ao guardar ficheiro"
                );
            }
        }

        return responses;
    }

    public List<AttachmentListResponse> listAttachments(
            Long reportId
    ) {

        checkInternalAccessToReport(reportId);

        return attachmentRepository.findByReportId(reportId)
                .stream()
                .map(a -> new AttachmentListResponse(
                        a.getId(),
                        a.getOriginalName(),
                        a.getMimeType(),
                        a.getSize()
                ))
                .toList();
    }

    public ResponseEntity<Resource> downloadAttachment(
            Long attachmentId
    ) {

        Attachment attachment =
                attachmentRepository.findById(
                        attachmentId
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND
                        )
                );

        checkInternalAccessToReport(attachment.getReport().getId());

        Resource resource =
                fileStorageService.loadFileAsResource(
                        attachment.getStoragePath()
                );

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                attachment.getMimeType()
                        )
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(
                                        attachment.getOriginalName(),
                                        StandardCharsets.UTF_8
                                )
                                .build()
                                .toString()
                )
                .body(resource);
    }

    public ResponseEntity<Resource> downloadAttachmentSecure(
            Long attachmentId,
            String trackingCode
    ) {

        if (trackingCode == null || trackingCode.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST
            );
        }

        Attachment attachment =
                attachmentRepository.findById(
                        attachmentId
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND
                        )
                );

        if (!passwordEncoder.matches(
                trackingCode,
                attachment.getReport().getTrackingCodeHash()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN
            );
        }

        Resource resource =
                fileStorageService.loadFileAsResource(
                        attachment.getStoragePath()
                );

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                attachment.getMimeType()
                        )
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(
                                        attachment.getOriginalName(),
                                        StandardCharsets.UTF_8
                                )
                                .build()
                                .toString()
                )
                .body(resource);
    }

    private void validateTrackingCodeForReport(Report report, String trackingCode) {
        if (trackingCode == null || trackingCode.isBlank()) {
            recordUploadRejected(report.getId(), "Missing tracking code");
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Upload not authorized"
            );
        }

        String normalizedTrackingCode;
        try {
            normalizedTrackingCode = TrackingCode.from(trackingCode.trim()).value();
        } catch (IllegalArgumentException e) {
            recordUploadRejected(report.getId(), "Invalid tracking code format");
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Upload not authorized"
            );
        }

        if (!passwordEncoder.matches(
                normalizedTrackingCode,
                report.getTrackingCodeHash()
        )) {
            recordUploadRejected(report.getId(), "Tracking code mismatch");
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Upload not authorized"
            );
        }
    }

    private void checkInternalAccessToReport(
            Long reportId
    ) {

        if (SecurityUtils.hasRole("ADMIN")) {
            return;
        }

        CaseReview caseReview =
                caseReviewRepository.findByReportId(
                        reportId
                ).orElseThrow(() ->
                {
                    auditLogService.log("ANALYST_ACCESS_DENIED", "REPORT", reportId, "Report has no assigned case review");
                    securityMonitoringService.recordUnauthorizedAnalystAccess(reportId);
                    return new ResponseStatusException(
                            HttpStatus.FORBIDDEN
                    );
                }
                );

        String currentUser =
                SecurityUtils.getCurrentUsername();

        if (caseReview.getAssignedAnalyst() == null ||
                !caseReview.getAssignedAnalyst()
                        .getUsername()
                        .equals(currentUser)) {

            auditLogService.log("ANALYST_ACCESS_DENIED", "REPORT", reportId, "Analyst attempted to access a report without ownership");
            securityMonitoringService.recordUnauthorizedAnalystAccess(reportId);

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private void recordUploadRejected(Long reportId, String reason) {
        String safeReason = reason == null || reason.isBlank()
                ? "Upload rejected"
                : reason;
        securityMonitoringService.recordRejectedUpload(reportId, safeReason);
        auditLogService.log("UPLOAD_REJECTED", "REPORT", reportId, "Upload rejected: " + safeReason);
    }

    private ReportResponse toReportResponse(
            Report report
    ) {

        return new ReportResponse(
                report.getId(),
                report.getTitle(),
                report.getStatus().name(),
                report.getCategory(),
                report.getDescription()
        );
    }
}
