package com.ghostreport.controller;

import com.ghostreport.security.CorrelationId;
import com.ghostreport.service.SecurityMonitoringService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class CspReportController {

    private final SecurityMonitoringService securityMonitoringService;

    public CspReportController(SecurityMonitoringService securityMonitoringService) {
        this.securityMonitoringService = securityMonitoringService;
    }

    @PostMapping(
            value = "/security/csp-report",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> receiveCspReport(@RequestBody(required = false) String report) {
        securityMonitoringService.recordCspViolation(report);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", HttpStatus.ACCEPTED.value());
        response.put("message", "Report accepted");
        response.put("correlationId", CorrelationId.current());
        return response;
    }
}
