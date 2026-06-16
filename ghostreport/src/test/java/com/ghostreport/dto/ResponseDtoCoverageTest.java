package com.ghostreport.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseDtoCoverageTest {

    @Test
    void caseReviewResponseExposesConstructorAndSetterValues() {
        CaseReviewResponse response =
                new CaseReviewResponse(1L, 2L, "analyst", "HIGH", "notes", "IN_REVIEW");

        assertEquals(1L, response.getReportId());
        assertEquals(2L, response.getCaseReviewId());
        assertEquals("analyst", response.getAssignedAnalystUsername());
        assertEquals("HIGH", response.getPriority());
        assertEquals("notes", response.getNotes());
        assertEquals("IN_REVIEW", response.getReportStatus());

        response.setReportId(3L);
        response.setCaseReviewId(4L);
        response.setAssignedAnalystUsername("new-analyst");
        response.setPriority("LOW");
        response.setNotes("updated");
        response.setReportStatus("CLOSED");

        assertEquals(3L, response.getReportId());
        assertEquals(4L, response.getCaseReviewId());
        assertEquals("new-analyst", response.getAssignedAnalystUsername());
        assertEquals("LOW", response.getPriority());
        assertEquals("updated", response.getNotes());
        assertEquals("CLOSED", response.getReportStatus());
    }

    @Test
    void userResponseExposesConstructorAndSetterValues() {
        UserResponse response =
                new UserResponse(1L, "admin", "admin@example.test", "ADMIN", true);

        assertEquals(1L, response.getId());
        assertEquals("admin", response.getUsername());
        assertEquals("admin@example.test", response.getEmail());
        assertEquals("ADMIN", response.getRole());
        assertTrue(response.isActive());

        response.setId(2L);
        response.setUsername("auditor");
        response.setEmail("auditor@example.test");
        response.setRole("AUDITOR");
        response.setActive(false);

        assertEquals(2L, response.getId());
        assertEquals("auditor", response.getUsername());
        assertEquals("auditor@example.test", response.getEmail());
        assertEquals("AUDITOR", response.getRole());
        assertFalse(response.isActive());
    }

    @Test
    void attachmentResponsesExposeConstructorAndSetterValues() {
        AttachmentResponse response =
                new AttachmentResponse(1L, "evidence.pdf", "application/pdf", 128L);
        AttachmentListResponse listResponse =
                new AttachmentListResponse(2L, "image.png", "image/png", 256L);
        AttachmentSummaryResponse summaryResponse =
                new AttachmentSummaryResponse(5L);

        assertEquals(1L, response.getId());
        assertEquals("evidence.pdf", response.getOriginalName());
        assertEquals("application/pdf", response.getMimeType());
        assertEquals(128L, response.getSize());
        assertEquals(2L, listResponse.getId());
        assertEquals("image.png", listResponse.getOriginalName());
        assertEquals("image/png", listResponse.getMimeType());
        assertEquals(256L, listResponse.getSize());
        assertEquals(5L, summaryResponse.getAttachmentCount());

        response.setId(3L);
        response.setOriginalName("notes.txt");
        response.setMimeType("text/plain");
        response.setSize(64L);
        listResponse.setId(4L);
        listResponse.setOriginalName("audio.wav");
        listResponse.setMimeType("audio/wav");
        listResponse.setSize(512L);
        summaryResponse.setAttachmentCount(6L);

        assertEquals(3L, response.getId());
        assertEquals("notes.txt", response.getOriginalName());
        assertEquals("text/plain", response.getMimeType());
        assertEquals(64L, response.getSize());
        assertEquals(4L, listResponse.getId());
        assertEquals("audio.wav", listResponse.getOriginalName());
        assertEquals("audio/wav", listResponse.getMimeType());
        assertEquals(512L, listResponse.getSize());
        assertEquals(6L, summaryResponse.getAttachmentCount());
    }

    @Test
    void reportResponseAndAuditRecordsExposeTheirValues() {
        LocalDateTime timestamp =
                LocalDateTime.of(2026, Month.JUNE, 15, 1, 30);
        ReportResponse report =
                new ReportResponse(1L, "Title", "SUBMITTED", "Fraud", "Description");
        AuditLogResponse audit =
                new AuditLogResponse(2L, timestamp, "cid", "admin", "LOGIN", "USER", 3L, "details", "hash");
        SecurityAlertResponse alert =
                new SecurityAlertResponse(4L, timestamp, "cid", "BRUTE_FORCE", "HIGH", "system", "AUTH", 5L, "desc", "hash2");

        assertEquals(1L, report.getId());
        assertEquals("Title", report.getTitle());
        assertEquals("SUBMITTED", report.getStatus());
        assertEquals("Fraud", report.getCategory());
        assertEquals("Description", report.getDescription());
        assertEquals(0L, report.getAttachmentCount());
        assertEquals(2L, audit.id());
        assertEquals(timestamp, audit.timestamp());
        assertEquals("cid", audit.correlationId());
        assertEquals("admin", audit.actor());
        assertEquals("LOGIN", audit.action());
        assertEquals("USER", audit.targetType());
        assertEquals(3L, audit.targetId());
        assertEquals("details", audit.details());
        assertEquals("hash", audit.integrityHash());
        assertEquals(4L, alert.id());
        assertEquals(timestamp, alert.timestamp());
        assertEquals("BRUTE_FORCE", alert.alertType());
        assertEquals("HIGH", alert.severity());
        assertEquals("system", alert.actor());
        assertEquals("AUTH", alert.targetType());
        assertEquals(5L, alert.targetId());
        assertEquals("desc", alert.description());
        assertEquals("hash2", alert.integrityHash());
    }
}
