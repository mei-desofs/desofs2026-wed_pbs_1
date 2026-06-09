package com.ghostreport.repository;

import com.ghostreport.model.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {
    boolean existsByTokenIdAndExpiresAtAfter(String tokenId, Instant now);
    Optional<RevokedToken> findByTokenId(String tokenId);
    long deleteByExpiresAtLessThanEqual(Instant now);
}
