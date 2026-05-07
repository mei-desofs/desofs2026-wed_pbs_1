package com.ghostreport.dto;

public record BackupVerificationResponse(
        String filename,
        boolean valid,
        String sha256,
        int checkedFiles,
        String message
) {
}
