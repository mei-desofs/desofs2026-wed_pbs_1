package com.ghostreport.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelEntityCoverageTest {

    @Test
    void reportDefaultsStatusAndMaintainsAttachmentRelationship() {
        Report report = new Report();
        report.setId(1L);
        report.setVersion(2L);
        report.setTitle("Title");
        report.setDescription("Description");
        report.setCategory("Fraud");
        report.setTrackingCodeHash("tracking-hash");

        report.prePersist();

        assertEquals(1L, report.getId());
        assertEquals(2L, report.getVersion());
        assertEquals("Title", report.getTitle());
        assertEquals("Description", report.getDescription());
        assertEquals("Fraud", report.getCategory());
        assertEquals(ReportStatus.SUBMITTED, report.getStatus());
        assertEquals("tracking-hash", report.getTrackingCodeHash());
        assertNotNull(report.getCreatedAt());

        Attachment attachment = new Attachment();
        report.addAttachment(attachment);

        assertEquals(1, report.getAttachments().size());
        assertSame(report, attachment.getReport());

        report.removeAttachment(attachment);

        assertEquals(0, report.getAttachments().size());
        assertNull(attachment.getReport());
        List<Attachment> attachments = report.getAttachments();
        Attachment rejectedAttachment = new Attachment();
        assertThrows(
                UnsupportedOperationException.class,
                () -> attachments.add(rejectedAttachment)
        );
    }

    @Test
    void reportSetAttachmentsReplacesExistingRelationships() {
        Report report = new Report();
        Attachment first = new Attachment();
        Attachment second = new Attachment();

        report.setAttachments(List.of(first, second));

        assertEquals(2, report.getAttachments().size());
        assertSame(report, first.getReport());
        assertSame(report, second.getReport());

        report.setAttachments(null);

        assertEquals(0, report.getAttachments().size());
    }

    @Test
    void attachmentExposesAllPersistedMetadata() {
        Report report = new Report();
        Attachment attachment = new Attachment();

        attachment.setId(1L);
        attachment.setOriginalName("evidence.pdf");
        attachment.setStoredName("stored.bin");
        attachment.setMimeType("application/pdf");
        attachment.setSize(1024L);
        attachment.setHash("sha256");
        attachment.setReport(report);
        attachment.setStoragePath("reports/1/stored.bin");
        attachment.setFileReference("ref-1");

        assertEquals(1L, attachment.getId());
        assertEquals("evidence.pdf", attachment.getOriginalName());
        assertEquals("stored.bin", attachment.getStoredName());
        assertEquals("application/pdf", attachment.getMimeType());
        assertEquals(1024L, attachment.getSize());
        assertEquals("sha256", attachment.getHash());
        assertSame(report, attachment.getReport());
        assertEquals("reports/1/stored.bin", attachment.getStoragePath());
        assertEquals("ref-1", attachment.getFileReference());
    }

    @Test
    void caseReviewDefaultsPriorityAndTracksAssignedMetadata() {
        Report report = new Report();
        User analyst = new User();
        CaseReview review = new CaseReview();
        LocalDateTime manualTimestamp =
                LocalDateTime.of(2026, Month.JUNE, 15, 1, 40);

        review.setId(1L);
        review.setVersion(2L);
        review.setReport(report);
        review.setAssignedAnalyst(analyst);
        review.setNotes("analysis notes");
        review.setUpdatedAt(manualTimestamp);

        review.updateTimestamp();

        assertEquals(1L, review.getId());
        assertEquals(2L, review.getVersion());
        assertSame(report, review.getReport());
        assertSame(analyst, review.getAssignedAnalyst());
        assertEquals("analysis notes", review.getNotes());
        assertEquals(CasePriority.MEDIUM, review.getPriority());
        assertNotNull(review.getUpdatedAt());
    }

    @Test
    void userExposesIdentityAndAssignedCasesDefensively() {
        User user = new User();
        CaseReview review = new CaseReview();
        LocalDateTime createdAt =
                LocalDateTime.of(2026, Month.JUNE, 15, 1, 45);

        user.setId(1L);
        user.setUsername("analyst");
        user.setEmail("analyst@example.test");
        user.setPasswordHash("hash");
        user.setRole(UserRole.ANALYST);
        user.setActive(false);
        user.setCreatedAt(createdAt);
        user.setAssignedCases(List.of(review));

        assertEquals(1L, user.getId());
        assertEquals("analyst", user.getUsername());
        assertEquals("analyst@example.test", user.getEmail());
        assertEquals("hash", user.getPasswordHash());
        assertEquals(UserRole.ANALYST, user.getRole());
        assertFalse(user.isActive());
        assertEquals(createdAt, user.getCreatedAt());
        assertEquals(1, user.getAssignedCases().size());
        List<CaseReview> assignedCases = user.getAssignedCases();
        assertThrows(
                UnsupportedOperationException.class,
                assignedCases::clear
        );

        user.setAssignedCases(null);

        assertEquals(0, user.getAssignedCases().size());
    }

    @Test
    void revokedTokenExposesConstructorSetterAndPrePersistValues() {
        Instant revokedAt = Instant.parse("2026-06-15T00:00:00Z");
        Instant expiresAt = Instant.parse("2026-06-15T01:00:00Z");
        RevokedToken token =
                new RevokedToken("jti", "subject", "kid", revokedAt, expiresAt);

        token.setId(1L);

        assertEquals(1L, token.getId());
        assertEquals("jti", token.getTokenId());
        assertEquals("subject", token.getSubject());
        assertEquals("kid", token.getKeyId());
        assertEquals(revokedAt, token.getRevokedAt());
        assertEquals(expiresAt, token.getExpiresAt());

        RevokedToken defaulted = new RevokedToken();
        defaulted.setTokenId("jti-2");
        defaulted.setSubject("subject-2");
        defaulted.setKeyId("kid-2");
        defaulted.setExpiresAt(expiresAt);
        defaulted.prePersist();

        assertNotNull(defaulted.getRevokedAt());
        assertEquals("jti-2", defaulted.getTokenId());
        assertEquals("subject-2", defaulted.getSubject());
        assertEquals("kid-2", defaulted.getKeyId());
        assertEquals(expiresAt, defaulted.getExpiresAt());
    }
}
