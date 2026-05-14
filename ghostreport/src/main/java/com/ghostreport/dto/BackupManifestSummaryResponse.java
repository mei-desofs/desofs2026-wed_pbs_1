package com.ghostreport.dto;

import java.util.Map;

public record BackupManifestSummaryResponse(
        String filename,
        String formatVersion,
        String createdAt,
        int totalFiles,
        Map<String, Integer> databaseExports
) {
}
