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
import java.util.ArrayList;
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

        auditLogService.log("REPORT_CREATED", "REPORT", saved.getId(), "Anonymous report created");

        return new CreateReportResponse(
                saved.getId(),
                saved.getStatus().name(),
                trackingCode
        );
    }

    public ReportResponse verifyTrackingCodeOnly(String trackingCode) {

        if (trackingCode == null || trackingCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido");
        }

        trackingCode = trackingCode.trim();

        for (Report report : reportRepository.findAll()) {
            if (passwordEncoder.matches(trackingCode, report.getTrackingCodeHash())) {
                return toReportResponse(report);
            }
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Denúncia não encontrada");
    }

    public List<ReportResponse> getAllReports() {
        return reportRepository.findAll()
                .stream()
                .map(this::toReportResponse)
                .toList();
    }

    public ReportResponse updateReportStatus(Long id, UpdateReportStatusRequest request) {

        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        checkInternalAccessToReport(id);

        report.setStatus(ReportStatus.valueOf(request.getStatus().toUpperCase()));

        Report saved = reportRepository.save(report);

        return toReportResponse(saved);
    }

    public AttachmentResponse uploadAttachment(Long reportId, MultipartFile file) {
        return uploadMultipleAttachments(reportId, new MultipartFile[]{file}).get(0);
    }

    public List<AttachmentResponse> uploadMultipleAttachments(Long reportId, MultipartFile[] files) {

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<AttachmentResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {

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

                responses.add(new AttachmentResponse(
                        saved.getId(),
                        saved.getOriginalName(),
                        saved.getMimeType(),
                        saved.getSize()
                ));

            } catch (Exception e) {
                logger.error("Erro ao guardar ficheiro", e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao guardar ficheiro");
            }
        }

        return responses;
    }

    public List<AttachmentListResponse> listAttachments(Long reportId) {

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

    public ResponseEntity<Resource> downloadAttachmentSecure(Long attachmentId, String trackingCode) {

        if (trackingCode == null || trackingCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(trackingCode, attachment.getReport().getTrackingCodeHash())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

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
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            code.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }
}