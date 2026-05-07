package com.ghostreport.dto;

public record BackupRestoreResponse(
        String filename,
        boolean restored,
        String restorePath,
        String message
) {
}
