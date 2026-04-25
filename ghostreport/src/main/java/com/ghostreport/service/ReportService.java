package com.ghostreport.service;

import com.ghostreport.dto.*;
import com.ghostreport.model.Report;
import com.ghostreport.model.ReportStatus;
import com.ghostreport.repository.ReportRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public CreateReportResponse createReport(CreateReportRequest request) {

        Report report = new Report();
        report.setDescription(request.getDescription());
        report.setCategory(request.getCategory());

        String trackingCode = generateTrackingCode();

        String hash = hashTrackingCode(trackingCode);

        report.setTrackingCodeHash(hash);

        Report saved = reportRepository.save(report);

        return new CreateReportResponse(
                saved.getId(),
                saved.getStatus().name(),
                trackingCode
        );
    }

    public List<ReportResponse> getAllReports() {
        return reportRepository.findAll().stream()
                .map(r -> new ReportResponse(
                        r.getId(),
                        r.getStatus().name(),
                        r.getCategory(),
                        r.getDescription()
                ))
                .toList();
    }

    public ReportResponse updateReportStatus(Long id, UpdateReportStatusRequest request) {

        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report não encontrado"));

        try {
            report.setStatus(ReportStatus.valueOf(request.getStatus()));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status inválido");
        }

        Report saved = reportRepository.save(report);

        return new ReportResponse(
                saved.getId(),
                saved.getStatus().name(),
                saved.getCategory(),
                saved.getDescription()
        );
    }

    public List<AttachmentListResponse> listAttachments(Long reportId) {

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report não encontrado"));

        return report.getAttachments().stream()
                .map(a -> new AttachmentListResponse(
                        a.getId(),
                        a.getOriginalName(),
                        a.getMimeType(),
                        a.getSize()
                ))
                .toList();
    }

    public ResponseEntity<Resource> downloadAttachment(Long attachmentId) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Download ainda não implementado");
    }

    private String generateTrackingCode() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String hashTrackingCode(String trackingCode) {
        return Integer.toHexString(trackingCode.hashCode());
    }
}