package com.ghostreport.service;

import com.ghostreport.model.RevokedToken;
import com.ghostreport.repository.RevokedTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PersistentRevokedTokenStore implements RevokedTokenStore {

    private final RevokedTokenRepository revokedTokenRepository;

    public PersistentRevokedTokenStore(RevokedTokenRepository revokedTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Override
    @Transactional
    public void revoke(String tokenId, String subject, String keyId, Instant expiresAt) {
        purgeExpired(Instant.now());
        revokedTokenRepository.findByTokenId(tokenId)
                .ifPresentOrElse(
                        existing -> {
                            existing.setSubject(subject);
                            existing.setKeyId(keyId);
                            existing.setExpiresAt(expiresAt);
                            revokedTokenRepository.save(existing);
                        },
                        () -> revokedTokenRepository.save(new RevokedToken(
                                tokenId,
                                subject,
                                keyId,
                                Instant.now(),
                                expiresAt
                        ))
                );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRevoked(String tokenId, Instant now) {
        return revokedTokenRepository.existsByTokenIdAndExpiresAtAfter(tokenId, now);
    }

    @Override
    @Transactional
    public void purgeExpired(Instant now) {
        revokedTokenRepository.deleteByExpiresAtLessThanEqual(now);
    }
}
