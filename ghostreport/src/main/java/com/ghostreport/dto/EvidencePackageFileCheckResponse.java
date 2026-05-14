package com.ghostreport.dto;

public record EvidencePackageFileCheckResponse(
        int index,
        long size,
        String sha256,
        boolean valid
) {
}
