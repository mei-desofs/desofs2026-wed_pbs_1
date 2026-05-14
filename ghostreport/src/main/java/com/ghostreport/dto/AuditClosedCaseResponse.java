package com.ghostreport.dto;

import java.time.LocalDateTime;

public record AuditClosedCaseResponse(
        Long reportId,
        Long caseReviewId,
        String status,
        String category,
        String priority,
        String assignedAnalyst,
        int attachmentCount,
        LocalDateTime reportCreatedAt,
        LocalDateTime caseUpdatedAt
) {
}
