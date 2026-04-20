package com.ghostreport.service;

import com.ghostreport.dto.CreateReportRequest;
import com.ghostreport.dto.ReportResponse;
import com.ghostreport.model.Report;
import com.ghostreport.model.ReportStatus;
import com.ghostreport.repository.ReportRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public ReportResponse createReport(CreateReportRequest request) {
        String trackingCode = generateTrackingCode();
        String trackingCodeHash = passwordEncoder.encode(trackingCode);

        Report report = new Report();
        report.setDescription(request.getDescription());
        report.setCategory(request.getCategory());
        report.setStatus(ReportStatus.SUBMITTED);
        report.setTrackingCodeHash(trackingCodeHash);

        Report saved = reportRepository.save(report);

        return new ReportResponse(saved.getId(), saved.getStatus().name(), trackingCode);
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