package com.ghostreport.dto;

import java.time.LocalDateTime;

public record SecurityAlertResponse(
        Long id,
        LocalDateTime timestamp,
        String correlationId,
        String alertType,
        String severity,
        String actor,
        String targetType,
        Long targetId,
        String description,
        String integrityHash
) {
}
