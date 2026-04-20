package com.ghostreport.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "case_reviews")
public class CaseReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "report_id", nullable = false, unique = true)
    private Report report;

    @ManyToOne
    @JoinColumn(name = "assigned_analyst_id")
    private User assignedAnalyst;

    @Column(length = 4000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CasePriority priority;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public CaseReview() {
    }

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
        if (this.priority == null) {
            this.priority = CasePriority.MEDIUM;
        }
    }

    public Long getId() {
        return id;
    }

    public Report getReport() {
        return report;
    }

    public User getAssignedAnalyst() {
        return assignedAnalyst;
    }

    public String getNotes() {
        return notes;
    }

    public CasePriority getPriority() {
        return priority;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setReport(Report report) {
        this.report = report;
    }

    public void setAssignedAnalyst(User assignedAnalyst) {
        this.assignedAnalyst = assignedAnalyst;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setPriority(CasePriority priority) {
        this.priority = priority;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}