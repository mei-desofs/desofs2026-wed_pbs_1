package com.ghostreport.controller;

import com.ghostreport.dto.CreateUserRequest;
import com.ghostreport.dto.UserResponse;
import com.ghostreport.repository.AuditLogRepository;
import com.ghostreport.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.ghostreport.model.SecurityAlert;
import com.ghostreport.repository.SecurityAlertRepository;
import com.ghostreport.model.AuditLog;
import java.util.List;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final SecurityAlertRepository securityAlertRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminController(UserService userService, SecurityAlertRepository securityAlertRepository, AuditLogRepository auditLogRepository) {
        this.userService = userService;
        this.securityAlertRepository = securityAlertRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/panel")
    public String adminPanel() {
        return "Access granted: ADMIN";
    }

    @GetMapping("/users")
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/audit-logs")
    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAll();
    }

    @GetMapping("/security-alerts")
    public List<SecurityAlert> getSecurityAlerts() {
        return securityAlertRepository.findAll();
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }
}