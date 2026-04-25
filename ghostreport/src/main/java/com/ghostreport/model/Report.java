package com.ghostreport.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportStatus status;

    @Column(name = "tracking_code_hash", nullable = false, unique = true, length = 255)
    private String trackingCodeHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();

    @OneToOne(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private CaseReview caseReview;

    public Report() {}

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = ReportStatus.SUBMITTED;
        }
    }

    // GETTERS

    public Long getId() { return id; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public ReportStatus getStatus() { return status; }
    public String getTrackingCodeHash() { return trackingCodeHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Attachment> getAttachments() { return attachments; }
    public CaseReview getCaseReview() { return caseReview; }

    // SETTERS

    public void setId(Long id) { this.id = id; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setStatus(ReportStatus status) { this.status = status; }
    public void setTrackingCodeHash(String trackingCodeHash) { this.trackingCodeHash = trackingCodeHash; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setAttachments(List<Attachment> attachments) { this.attachments = attachments; }
    public void setCaseReview(CaseReview caseReview) { this.caseReview = caseReview; }
}