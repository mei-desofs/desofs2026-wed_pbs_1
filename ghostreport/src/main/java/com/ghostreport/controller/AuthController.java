package com.ghostreport.controller;

import com.ghostreport.dto.AuthResponse;
import com.ghostreport.dto.LoginRequest;
import com.ghostreport.service.AuditLogService;
import com.ghostreport.service.AuthService;
import com.ghostreport.service.RateLimiterService;
import com.ghostreport.service.SecurityMonitoringService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;
    private final SecurityMonitoringService securityMonitoringService;
    private final AuditLogService auditLogService;

    public AuthController(
            AuthService authService,
            RateLimiterService rateLimiterService,
            SecurityMonitoringService securityMonitoringService,
            AuditLogService auditLogService
    ) {
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;
        this.securityMonitoringService = securityMonitoringService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientKey = httpRequest.getRemoteAddr();
        rateLimiterService.checkLoginAllowed(clientKey);
        try {
            AuthResponse response = authService.login(request);
            rateLimiterService.clearLoginFailures(clientKey);
            return response;
        } catch (AuthenticationException ex) {
            if (rateLimiterService.recordLoginFailure(clientKey)) {
                securityMonitoringService.recordBruteForceLoginAttempt();
            }
            auditLogService.log("LOGIN_FAILED", "AUTHENTICATION", null, "Login failed");
            throw ex;
        }
    }
}
