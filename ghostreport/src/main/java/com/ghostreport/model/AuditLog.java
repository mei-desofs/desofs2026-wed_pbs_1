package com.ghostreport.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 80)
    private String correlationId;

    @Column(nullable = false, length = 120)
    private String actor;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(length = 80)
    private String targetType;

    private Long targetId;

    @Column(length = 500)
    private String details;

    @Column(nullable = false, length = 64)
    private String integrityHash;

    public AuditLog() {
    }

    @PrePersist
    public void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        }
    }

    public Long getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getCorrelationId() { return correlationId; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getDetails() { return details; }
    public String getIntegrityHash() { return integrityHash; }

    public void setId(Long id) { this.id = id; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public void setActor(String actor) { this.actor = actor; }
    public void setAction(String action) { this.action = action; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public void setDetails(String details) { this.details = details; }
    public void setIntegrityHash(String integrityHash) { this.integrityHash = integrityHash; }
}
