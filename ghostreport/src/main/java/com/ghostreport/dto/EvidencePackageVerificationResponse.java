package com.ghostreport.dto;

import java.util.List;

public record EvidencePackageVerificationResponse(
        Long reportId,
        String status,
        boolean valid,
        int checkedFiles,
        List<EvidencePackageFileCheckResponse> files,
        String message
) {
}
