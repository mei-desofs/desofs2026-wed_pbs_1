package com.ghostreport.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String ISSUER = "ghostreport";
    private static final String AUDIENCE = "ghostreport-api";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;
    private final Map<String, Long> revokedTokenExpirations = new ConcurrentHashMap<>();

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${ghostreport.jwt.secret}") String secret,
            @Value("${ghostreport.jwt.expiration-seconds:3600}") long expirationSeconds
    ) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("ghostreport.jwt.secret must be configured with at least 32 characters");
        }
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(UserDetails userDetails) {
        long now = Instant.now().getEpochSecond();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", userDetails.getUsername());
        payload.put("iss", ISSUER);
        payload.put("aud", AUDIENCE);
        payload.put("jti", UUID.randomUUID().toString());
        payload.put("role", userDetails.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .orElse(""));
        payload.put("iat", now);
        payload.put("exp", now + expirationSeconds);

        String unsignedToken = encodeJson(header) + "." + encodeJson(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public String extractUsername(String token) {
        return readVerifiedClaims(token).subject();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public void revokeToken(String token) {
        JwtClaims claims = readVerifiedClaims(token);
        revokedTokenExpirations.put(claims.tokenId(), claims.expiration());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            JwtClaims claims = readVerifiedClaims(token);
            List<String> expectedRoles = userDetails.getAuthorities().stream()
                    .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                    .toList();

            return userDetails.isEnabled()
                    && userDetails.isAccountNonExpired()
                    && userDetails.isAccountNonLocked()
                    && userDetails.isCredentialsNonExpired()
                    && !isTokenRevoked(claims)
                    && claims.subject().equals(userDetails.getUsername())
                    && expectedRoles.contains(claims.role());
        } catch (RuntimeException e) {
            return false;
        }
    }

    private String encodeJson(Map<String, Object> values) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(values));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not encode JWT", e);
        }
    }

    private JwtClaims readVerifiedClaims(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT structure");
            }

            if (!signatureMatches(token)) {
                throw new IllegalArgumentException("Invalid JWT signature");
            }

            Map<String, Object> header = objectMapper.readValue(
                    BASE64_URL_DECODER.decode(parts[0]),
                    new TypeReference<>() {
                    }
            );
            if (!"HS256".equals(header.get("alg")) || !"JWT".equals(header.get("typ"))) {
                throw new IllegalArgumentException("Invalid JWT header");
            }

            byte[] payloadBytes = BASE64_URL_DECODER.decode(parts[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadBytes, new TypeReference<>() {
            });
            Object subject = payload.get("sub");
            Object issuer = payload.get("iss");
            Object audience = payload.get("aud");
            Object tokenId = payload.get("jti");
            Object role = payload.get("role");
            Object expiration = payload.get("exp");
            Object issuedAt = payload.get("iat");
            if (!(subject instanceof String username) || username.isBlank()
                    || !ISSUER.equals(issuer)
                    || !AUDIENCE.equals(audience)
                    || !(tokenId instanceof String jti) || jti.isBlank()
                    || !(role instanceof String authorityRole) || authorityRole.isBlank()
                    || !(expiration instanceof Number exp)
                    || !(issuedAt instanceof Number)) {
                throw new IllegalArgumentException("Invalid JWT claims");
            }
            if (exp.longValue() <= Instant.now().getEpochSecond()) {
                throw new IllegalArgumentException("Expired JWT");
            }
            return new JwtClaims(username, authorityRole, jti, exp.longValue());
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JWT", e);
        }
    }

    private boolean isTokenRevoked(JwtClaims claims) {
        purgeExpiredRevocations();
        Long revokedUntil = revokedTokenExpirations.get(claims.tokenId());
        return revokedUntil != null && revokedUntil > Instant.now().getEpochSecond();
    }

    private void purgeExpiredRevocations() {
        long now = Instant.now().getEpochSecond();
        revokedTokenExpirations.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private boolean signatureMatches(String token) {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            return false;
        }

        byte[] expected = sign(parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII);
        byte[] supplied = parts[2].getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, supplied);
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not sign JWT", e);
        }
    }

    private record JwtClaims(String subject, String role, String tokenId, long expiration) {
    }
}
