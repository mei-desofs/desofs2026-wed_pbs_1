package com.ghostreport.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        LocalDateTime timestamp,
        String actor,
        String action,
        String targetType,
        Long targetId,
        String details
) {
}
