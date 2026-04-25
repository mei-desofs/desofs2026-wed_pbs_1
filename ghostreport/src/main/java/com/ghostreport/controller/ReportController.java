package com.ghostreport.controller;

import com.ghostreport.dto.*;
import com.ghostreport.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateReportResponse createReport(@Valid @RequestBody CreateReportRequest request) {
        return reportService.createReport(request);
    }

    @PostMapping("/verify")
    public ReportResponse verifyTrackingCodeOnly(@Valid @RequestBody VerifyTrackingCodeRequest request) {
        return reportService.verifyTrackingCodeOnly(request.getTrackingCode());
    }
}