package com.ghostreport.dto;

import java.util.List;

public record CasePackageResponse(
        Long reportId,
        String status,
        String packagePath,
        List<String> generatedFiles
) {
}