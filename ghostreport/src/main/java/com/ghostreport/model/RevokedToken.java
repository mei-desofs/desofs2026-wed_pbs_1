package com.ghostreport.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "revoked_tokens",
        indexes = {
                @Index(name = "idx_revoked_tokens_jti", columnList = "token_id", unique = true),
                @Index(name = "idx_revoked_tokens_expires_at", columnList = "expires_at")
        }
)
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_id", nullable = false, unique = true, length = 80)
    private String tokenId;

    @Column(name = "subject", nullable = false, length = 120)
    private String subject;

    @Column(name = "key_id", nullable = false, length = 80)
    private String keyId;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public RevokedToken() {
    }

    public RevokedToken(String tokenId, String subject, String keyId, Instant revokedAt, Instant expiresAt) {
        this.tokenId = tokenId;
        this.subject = subject;
        this.keyId = keyId;
        this.revokedAt = revokedAt;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    public void prePersist() {
        if (revokedAt == null) {
            revokedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getTokenId() {
        return tokenId;
    }

    public String getSubject() {
        return subject;
    }

    public String getKeyId() {
        return keyId;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
