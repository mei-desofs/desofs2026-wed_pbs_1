package com.ghostreport.controller;

import com.ghostreport.dto.ReportResponse;
import com.ghostreport.dto.UpdateReportStatusRequest;
import com.ghostreport.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/analyst")
public class AnalystController {

    private final ReportService reportService;

    public AnalystController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/panel")
    public String analystPanel() {
        return "Access granted: ANALYST or ADMIN";
    }

    @GetMapping("/reports")
    public List<ReportResponse> getAllReports() {
        return reportService.getAllReports();
    }

    @PatchMapping("/reports/{id}/status")
    public ReportResponse updateReportStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReportStatusRequest request
    ) {
        return reportService.updateReportStatus(id, request);
    }
}