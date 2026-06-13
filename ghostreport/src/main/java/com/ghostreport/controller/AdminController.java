package com.ghostreport.controller;

import com.ghostreport.dto.CreateUserRequest;
import com.ghostreport.dto.AuditLogResponse;
import com.ghostreport.dto.SecurityAlertResponse;
import com.ghostreport.dto.UpdateUserRequest;
import com.ghostreport.dto.UserResponse;
import com.ghostreport.repository.AuditLogRepository;
import com.ghostreport.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import com.ghostreport.repository.SecurityAlertRepository;
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
    public List<AuditLogResponse> getAuditLogs() {
        return auditLogRepository.findAll().stream()
                .map(auditLog -> new AuditLogResponse(
                        auditLog.getId(),
                        auditLog.getTimestamp(),
                        auditLog.getCorrelationId(),
                        auditLog.getActor(),
                        auditLog.getAction(),
                        auditLog.getTargetType(),
                        auditLog.getTargetId(),
                        auditLog.getDetails(),
                        auditLog.getIntegrityHash()
                ))
                .toList();
    }

    @GetMapping("/security-alerts")
    public List<SecurityAlertResponse> getSecurityAlerts() {
        return securityAlertRepository.findAll().stream()
                .map(alert -> new SecurityAlertResponse(
                        alert.getId(),
                        alert.getTimestamp(),
                        alert.getCorrelationId(),
                        alert.getAlertType(),
                        alert.getSeverity(),
                        alert.getActor(),
                        alert.getTargetType(),
                        alert.getTargetId(),
                        alert.getDescription(),
                        alert.getIntegrityHash()
                ))
                .toList();
    }

    @PostMapping(
            value = "/users",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @PutMapping(
            value = "/users/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request);
    }

    @PatchMapping("/users/{id}/activate")
    public UserResponse activateUser(@PathVariable Long id) {
        return userService.setActive(id, true);
    }

    @PatchMapping("/users/{id}/deactivate")
    public UserResponse deactivateUser(@PathVariable Long id) {
        return userService.setActive(id, false);
    }

    @DeleteMapping("/users/{id}")
    public UserResponse removeUser(@PathVariable Long id) {
        return userService.setActive(id, false);
    }
}
