package com.ghostreport.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 120)
    private String actor;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(length = 80)
    private String targetType;

    private Long targetId;

    @Column(length = 500)
    private String details;

    public AuditLog() {
    }

    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getDetails() { return details; }

    public void setId(Long id) { this.id = id; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setActor(String actor) { this.actor = actor; }
    public void setAction(String action) { this.action = action; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public void setDetails(String details) { this.details = details; }
}