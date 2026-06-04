package com.ghostreport.controller;

import com.ghostreport.dto.AuthResponse;
import com.ghostreport.dto.LoginRequest;
import com.ghostreport.service.AuditLogService;
import com.ghostreport.service.AuthService;
import com.ghostreport.service.JwtService;
import com.ghostreport.service.RateLimiterService;
import com.ghostreport.service.SecurityMonitoringService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;
    private final SecurityMonitoringService securityMonitoringService;
    private final AuditLogService auditLogService;
    private final JwtService jwtService;

    public AuthController(
            AuthService authService,
            RateLimiterService rateLimiterService,
            SecurityMonitoringService securityMonitoringService,
            AuditLogService auditLogService,
            JwtService jwtService
    ) {
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;
        this.securityMonitoringService = securityMonitoringService;
        this.auditLogService = auditLogService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientKey = loginRateLimitKey(httpRequest, request.getUsername());
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

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, Authentication authentication) {
        String token = bearerToken(request);
        jwtService.revokeToken(token);
        String username = authentication != null ? authentication.getName() : "unknown";
        auditLogService.log("LOGOUT", "AUTHENTICATION", null, "User logged out: " + username);
        return ResponseEntity.noContent().build();
    }

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing bearer token");
        }
        return authorization.substring("Bearer ".length());
    }

    private String loginRateLimitKey(HttpServletRequest request, String username) {
        String normalizedUsername = username == null
                ? "unknown"
                : username.trim().toLowerCase(Locale.ROOT);
        return normalizedUsername + "@" + request.getRemoteAddr();
    }
}
