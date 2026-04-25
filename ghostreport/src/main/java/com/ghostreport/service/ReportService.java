package com.ghostreport.service;

import com.ghostreport.dto.AttachmentResponse;
import com.ghostreport.dto.CreateReportRequest;
import com.ghostreport.dto.CreateReportResponse;
import com.ghostreport.dto.ReportResponse;
import com.ghostreport.dto.UpdateReportStatusRequest;
import com.ghostreport.model.Attachment;
import com.ghostreport.model.Report;
import com.ghostreport.model.ReportStatus;
import com.ghostreport.repository.AttachmentRepository;
import com.ghostreport.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.ghostreport.model.CaseReview;
import com.ghostreport.repository.CaseReviewRepository;
import com.ghostreport.security.SecurityUtils;

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
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ReportService(
            ReportRepository reportRepository,
            AttachmentRepository attachmentRepository,
            FileStorageService fileStorageService,
            CaseReviewRepository caseReviewRepository
    ) {
        this.reportRepository = reportRepository;
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService = fileStorageService;
        this.caseReviewRepository = caseReviewRepository;
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

        return new CreateReportResponse(
                saved.getId(),
                saved.getStatus().name(),
                trackingCode
        );
    }

    public ReportResponse getReport(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        return new ReportResponse(
                report.getId(),
                report.getDescription(),
                report.getCategory(),
                report.getStatus().name()
        );
    }

    public ReportResponse verifyTrackingCode(Long id, String trackingCode) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        boolean matches = passwordEncoder.matches(trackingCode, report.getTrackingCodeHash());

        if (!matches) {
            logger.warn("Invalid tracking code attempt for report id={}", id);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid tracking code");
        }

        logger.info("Tracking code verified successfully for report id={}", report.getId());

        return new ReportResponse(
                report.getId(),
                report.getDescription(),
                report.getCategory(),
                report.getStatus().name()
        );
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

        return new AttachmentResponse(
                saved.getId(),
                saved.getOriginalName(),
                saved.getMimeType(),
                saved.getSize()
        );
    }

    public List<ReportResponse> getAllReports() {
        return reportRepository.findAll().stream()
                .map(report -> new ReportResponse(
                        report.getId(),
                        report.getDescription(),
                        report.getCategory(),
                        report.getStatus().name()
                ))
                .toList();
    }

    public ReportResponse updateReportStatus(Long id, UpdateReportStatusRequest request) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        if (!SecurityUtils.hasRole("ADMIN")) {
            CaseReview caseReview = caseReviewRepository.findByReportId(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No case review assigned"));

            String currentUsername = SecurityUtils.getCurrentUsername();

            if (caseReview.getAssignedAnalyst() == null ||
                    !caseReview.getAssignedAnalyst().getUsername().equals(currentUsername)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this case");
            }
        }

        try {
            ReportStatus newStatus = ReportStatus.valueOf(request.getStatus().toUpperCase());
            report.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }

        Report saved = reportRepository.save(report);

        logger.info("Report id={} status updated to {}", saved.getId(), saved.getStatus());

        return new ReportResponse(
                saved.getId(),
                saved.getDescription(),
                saved.getCategory(),
                saved.getStatus().name()
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