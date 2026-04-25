package com.ghostreport.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_alerts")
public class SecurityAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 80)
    private String alertType;

    @Column(nullable = false, length = 30)
    private String severity;

    @Column(length = 120)
    private String actor;

    @Column(length = 80)
    private String targetType;

    private Long targetId;

    @Column(length = 500)
    private String description;

    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getAlertType() { return alertType; }
    public String getSeverity() { return severity; }
    public String getActor() { return actor; }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getDescription() { return description; }

    public void setId(Long id) { this.id = id; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setActor(String actor) { this.actor = actor; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public void setDescription(String description) { this.description = description; }
}