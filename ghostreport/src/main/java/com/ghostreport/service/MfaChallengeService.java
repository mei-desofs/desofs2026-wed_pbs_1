package com.ghostreport.service;

import com.ghostreport.config.MfaProperties;
import com.ghostreport.model.User;
import com.ghostreport.model.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MfaChallengeService {

    private static final Logger logger = LoggerFactory.getLogger(MfaChallengeService.class);
    private static final int CODE_BOUND = 1_000_000;

    private final MfaProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();
    private final Map<String, String> exposedCodesForTesting = new ConcurrentHashMap<>();

    public MfaChallengeService(
            MfaProperties properties,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService,
            Clock clock
    ) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    public boolean isMfaRequiredFor(UserRole role) {
        return properties.isEnabled() && properties.getRequiredRoles().contains(role);
    }

    public MfaChallenge createChallenge(User user) {
        purgeExpired();

        String challengeId = UUID.randomUUID().toString();
        String code = "%06d".formatted(secureRandom.nextInt(CODE_BOUND));
        Instant expiresAt = clock.instant().plusSeconds(Math.max(1, properties.getCodeTtlSeconds()));

        challenges.put(challengeId, new Challenge(
                user.getId(),
                user.getUsername(),
                passwordEncoder.encode(challengeId + ":" + code),
                expiresAt
        ));
        if (properties.isExposeCode()) {
            exposedCodesForTesting.put(challengeId, code);
        }

        auditLogService.log("MFA_CHALLENGE_CREATED", "USER", user.getId(), "MFA challenge created");
        logger.info("MFA challenge created for user id={}, role={}, expiresAt={}", user.getId(), user.getRole(), expiresAt);
        if (properties.isExposeCode()) {
            logger.info("DEV MFA code for {}: {}", user.getEmail(), code);
        }

        return new MfaChallenge(challengeId);
    }

    public String verifyChallenge(String challengeId, String code) {
        Challenge challenge = challenges.get(challengeId);
        if (challenge == null) {
            auditLogService.log("MFA_VERIFY_REJECTED", "USER", null, "Missing MFA challenge");
            throw new IllegalArgumentException("Invalid or expired verification code");
        }

        if (!challenge.expiresAt().isAfter(clock.instant())) {
            challenges.remove(challengeId);
            exposedCodesForTesting.remove(challengeId);
            auditLogService.log("MFA_VERIFY_EXPIRED", "USER", challenge.userId(), "Expired MFA challenge");
            throw new IllegalArgumentException("Invalid or expired verification code");
        }

        if (!passwordEncoder.matches(challengeId + ":" + code, challenge.codeHash())) {
            auditLogService.log("MFA_VERIFY_REJECTED", "USER", challenge.userId(), "Invalid MFA code");
            throw new IllegalArgumentException("Invalid or expired verification code");
        }

        challenges.remove(challengeId);
        exposedCodesForTesting.remove(challengeId);
        auditLogService.log("MFA_VERIFY_SUCCESS", "USER", challenge.userId(), "MFA completed");
        return challenge.username();
    }

    public String getExposedCodeForTesting(String challengeId) {
        if (!properties.isExposeCode()) {
            throw new IllegalStateException("MFA codes are not exposed in this profile");
        }
        return exposedCodesForTesting.get(challengeId);
    }

    public void expireChallengeForTesting(String challengeId) {
        if (!properties.isExposeCode()) {
            throw new IllegalStateException("MFA challenge mutation is only available in test/dev exposure mode");
        }
        challenges.computeIfPresent(challengeId, (id, challenge) -> new Challenge(
                challenge.userId(),
                challenge.username(),
                challenge.codeHash(),
                clock.instant().minusSeconds(1)
        ));
    }

    private void purgeExpired() {
        Instant now = clock.instant();
        challenges.entrySet().removeIf(entry -> {
            boolean expired = !entry.getValue().expiresAt().isAfter(now);
            if (expired) {
                exposedCodesForTesting.remove(entry.getKey());
            }
            return expired;
        });
    }

    public record MfaChallenge(String challengeId) {
    }

    private record Challenge(Long userId, String username, String codeHash, Instant expiresAt) {
    }
}
