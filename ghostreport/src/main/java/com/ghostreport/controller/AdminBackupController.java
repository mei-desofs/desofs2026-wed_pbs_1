package com.ghostreport.controller;

import com.ghostreport.dto.BackupFileResponse;
import com.ghostreport.dto.BackupOperationResponse;
import com.ghostreport.dto.BackupRestoreResponse;
import com.ghostreport.dto.BackupVerificationResponse;
import com.ghostreport.service.BackupService;
import com.ghostreport.service.ReauthenticationService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/admin/backups")
public class AdminBackupController {

    private final BackupService backupService;
    private final ReauthenticationService reauthenticationService;

    public AdminBackupController(BackupService backupService, ReauthenticationService reauthenticationService) {
        this.backupService = backupService;
        this.reauthenticationService = reauthenticationService;
    }

    @PostMapping
    public BackupOperationResponse createBackup() {
        return backupService.createBackup();
    }

    @GetMapping
    public List<BackupFileResponse> listBackups() {
        return backupService.listBackups();
    }

    @GetMapping("/{filename}/download")
    public ResponseEntity<Resource> downloadBackup(@PathVariable String filename) {
        Resource resource = backupService.getBackupResource(filename);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename, StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(resource);
    }

    @PostMapping("/{filename}/verify")
    public BackupVerificationResponse verifyBackup(@PathVariable String filename) {
        return backupService.verifyBackup(filename);
    }

    @PostMapping("/{filename}/restore")
    public BackupRestoreResponse restoreBackup(
            @PathVariable String filename,
            @RequestHeader(value = "X-Reauth-Password", required = false) String password,
            Authentication authentication
    ) {
        reauthenticationService.requireCurrentPassword(authentication, password, "BACKUP_RESTORE");
        return backupService.restoreBackup(filename);
    }
}
