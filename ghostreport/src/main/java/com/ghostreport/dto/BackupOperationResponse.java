package com.ghostreport.dto;

public record BackupOperationResponse(
        String filename,
        long size,
        String sha256,
        int totalFiles,
        String message
) {
}
