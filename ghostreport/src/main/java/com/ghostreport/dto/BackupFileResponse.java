package com.ghostreport.dto;

import java.time.LocalDateTime;

public record BackupFileResponse(
        String filename,
        long size,
        String sha256,
        LocalDateTime createdAt
) {
}
