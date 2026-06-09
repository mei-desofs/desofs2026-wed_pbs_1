package com.ghostreport.service;

import java.time.Instant;

public interface RevokedTokenStore {
    void revoke(String tokenId, String subject, String keyId, Instant expiresAt);
    boolean isRevoked(String tokenId, Instant now);
    void purgeExpired(Instant now);
}
