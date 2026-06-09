package com.ghostreport.service;

import com.ghostreport.dto.PasswordResetRequestResponse;
import com.ghostreport.model.PasswordResetToken;
import com.ghostreport.model.User;
import com.ghostreport.repository.PasswordResetTokenRepository;
import com.ghostreport.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final AuditLogService auditLogService;
    private final Clock clock;
    private final long tokenTtlMinutes;
    private final boolean exposeResetToken;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyService passwordPolicyService,
            AuditLogService auditLogService,
            Clock clock,
            @Value("${ghostreport.password-reset.token-ttl-minutes:30}") long tokenTtlMinutes,
            @Value("${ghostreport.password-reset.expose-token:false}") boolean exposeResetToken
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
        this.auditLogService = auditLogService;
        this.clock = clock;
        this.tokenTtlMinutes = tokenTtlMinutes;
        this.exposeResetToken = exposeResetToken;
    }

    @Transactional
    public PasswordResetRequestResponse requestReset(String usernameOrEmail) {
        Optional<User> user = findUser(usernameOrEmail);
        String rawToken = null;

        if (user.isPresent() && user.get().isActive()) {
            rawToken = createResetToken(user.get());
            auditLogService.log(
                    "PASSWORD_RESET_REQUESTED",
                    "USER",
                    user.get().getId(),
                    "Password reset token issued"
            );
        }

        return new PasswordResetRequestResponse(
                "If the account exists, a password reset token has been issued",
                exposeResetToken ? rawToken : null
        );
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token"));

        LocalDateTime now = LocalDateTime.now(clock);
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(now)) {
            auditLogService.log("PASSWORD_RESET_REJECTED", "USER", token.getUser().getId(), "Reset token invalid or expired");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token");
        }

        User user = token.getUser();
        passwordPolicyService.validateNewPassword(user, newPassword);
        String newHash = passwordEncoder.encode(newPassword);
        user.setPasswordHash(newHash);
        token.setUsedAt(now);
        passwordPolicyService.rememberPassword(user, newHash);

        userRepository.save(user);
        tokenRepository.save(token);
        auditLogService.log("PASSWORD_RESET_COMPLETED", "USER", user.getId(), "Password reset completed");
    }

    private String createResetToken(User user) {
        tokenRepository.deleteByUserAndUsedAtIsNull(user);
        String rawToken = generateToken();

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(LocalDateTime.now(clock).plusMinutes(tokenTtlMinutes));
        tokenRepository.save(token);

        return rawToken;
    }

    private Optional<User> findUser(String usernameOrEmail) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            return Optional.empty();
        }

        String lookup = usernameOrEmail.trim();
        Optional<User> byUsername = userRepository.findByUsername(lookup);
        if (byUsername.isPresent()) {
            return byUsername;
        }
        return userRepository.findByEmail(lookup.toLowerCase(Locale.ROOT));
    }

    private String generateToken() {
        byte[] token = new byte[32];
        SECURE_RANDOM.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
