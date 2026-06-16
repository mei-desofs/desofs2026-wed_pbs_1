package com.ghostreport.dto;

public record CasePackageResponse(
        Long reportId,
        String status,
        int generatedFileCount,
        String message
) {
}
