package com.ghostreport.controller;

import com.ghostreport.dto.AuditClosedCaseResponse;
import com.ghostreport.dto.BackupFileResponse;
import com.ghostreport.dto.BackupManifestSummaryResponse;
import com.ghostreport.dto.BackupVerificationResponse;
import com.ghostreport.dto.EvidencePackageVerificationResponse;
import com.ghostreport.model.AuditLog;
import com.ghostreport.model.SecurityAlert;
import com.ghostreport.repository.AuditLogRepository;
import com.ghostreport.repository.SecurityAlertRepository;
import com.ghostreport.service.AuditReadService;
import com.ghostreport.service.BackupService;
import com.ghostreport.service.CasePackageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final SecurityAlertRepository securityAlertRepository;
    private final AuditReadService auditReadService;
    private final CasePackageService casePackageService;
    private final BackupService backupService;

    public AuditController(
            AuditLogRepository auditLogRepository,
            SecurityAlertRepository securityAlertRepository,
            AuditReadService auditReadService,
            CasePackageService casePackageService,
            BackupService backupService
    ) {
        this.auditLogRepository = auditLogRepository;
        this.securityAlertRepository = securityAlertRepository;
        this.auditReadService = auditReadService;
        this.casePackageService = casePackageService;
        this.backupService = backupService;
    }

    @GetMapping("/logs")
    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAll();
    }

    @GetMapping("/security-alerts")
    public List<SecurityAlert> getSecurityAlerts() {
        return securityAlertRepository.findAll();
    }

    @GetMapping("/cases/closed")
    public List<AuditClosedCaseResponse> getClosedCaseHistory() {
        return auditReadService.getClosedCaseHistory();
    }

    @GetMapping("/cases/{reportId}/evidence-package/verify")
    public EvidencePackageVerificationResponse verifyEvidencePackage(@PathVariable Long reportId) {
        return casePackageService.verifyCasePackage(reportId);
    }

    @GetMapping("/backups")
    public List<BackupFileResponse> listBackups() {
        return backupService.listBackups();
    }

    @GetMapping("/backups/{filename}/verify")
    public BackupVerificationResponse verifyBackup(@PathVariable String filename) {
        return backupService.verifyBackup(filename);
    }

    @GetMapping("/backups/{filename}/manifest")
    public BackupManifestSummaryResponse getBackupManifest(@PathVariable String filename) {
        return backupService.getBackupManifestSummary(filename);
    }
}
