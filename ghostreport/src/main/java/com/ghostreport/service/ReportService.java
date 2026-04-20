package com.ghostreport.service;

import com.ghostreport.dto.AttachmentResponse;
import com.ghostreport.dto.CreateReportRequest;
import com.ghostreport.dto.CreateReportResponse;
import com.ghostreport.dto.ReportResponse;
import com.ghostreport.model.Attachment;
import com.ghostreport.model.Report;
import com.ghostreport.model.ReportStatus;
import com.ghostreport.repository.AttachmentRepository;
import com.ghostreport.repository.ReportRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public ReportService(
            ReportRepository reportRepository,
            AttachmentRepository attachmentRepository,
            FileStorageService fileStorageService
    ) {
        this.reportRepository = reportRepository;
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService = fileStorageService;
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid tracking code");
        }

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

        FileStorageService.StoredFileInfo stored = fileStorageService.storeFile(file);

        Attachment attachment = new Attachment();
        attachment.setOriginalName(stored.originalName());
        attachment.setStoredName(stored.storedName());
        attachment.setMimeType(stored.mimeType());
        attachment.setSize(stored.size());
        attachment.setHash(stored.hash());
        attachment.setReport(report);

        Attachment saved = attachmentRepository.save(attachment);

        return new AttachmentResponse(
                saved.getId(),
                saved.getOriginalName(),
                saved.getMimeType(),
                saved.getSize()
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