package com.ghostreport.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostreport.service.JwtService;
import com.ghostreport.service.RevokedTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceSecurityTest {

    private static final String CURRENT_KID = "current-key";
    private static final String OLD_KID = "old-key";
    private static final String SECRET = "test-secret-with-more-than-32-characters";
    private static final String OLD_SECRET = "old-test-secret-with-more-than-32-characters";

    private ObjectMapper objectMapper;
    private RecordingRevokedTokenStore revokedTokenStore;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        revokedTokenStore = new RecordingRevokedTokenStore();
    }

    @Test
    void validTokenIsAcceptedForMatchingUserAndRole() {
        JwtService jwtService = jwtService(3600);
        UserDetails user = user("analyst", "ANALYST");

        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void generatedTokenContainsIssuerAudienceUniqueIdentifierAndKeyId() throws Exception {
        JwtService jwtService = jwtService(3600);

        String token = jwtService.generateToken(user("analyst", "ANALYST"));
        Map<String, Object> header = header(token);
        Map<String, Object> payload = payload(token);

        assertThat(header)
                .containsEntry("alg", "HS256")
                .containsEntry("typ", "JWT")
                .containsEntry("kid", CURRENT_KID);
        assertThat(payload)
                .containsEntry("iss", "ghostreport")
                .containsEntry("aud", "ghostreport-api");
        assertThat(payload.get("jti")).isInstanceOf(String.class);
        assertThat((String) payload.get("jti")).isNotBlank();
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwtService = jwtService(3600);
        UserDetails user = user("analyst", "ANALYST");
        String token = jwtService.generateToken(user);

        String[] parts = token.split("\\.", -1);
        String tamperedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("""
                        {"sub":"analyst","role":"ADMIN","iat":1,"exp":9999999999}
                        """.getBytes(StandardCharsets.UTF_8));
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertFalse(jwtService.isTokenValid(tamperedToken, user));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwtService = jwtService(3600);
        UserDetails user = user("analyst", "ANALYST");
        long now = Instant.now().getEpochSecond();
        String token = signedToken(CURRENT_KID, SECRET, claims("analyst", "ANALYST", now - 3600, now - 1, "expired-jti"));

        assertFalse(jwtService.isTokenValid(token, user));
    }

    @Test
    void tokenRoleMustMatchCurrentUserAuthorities() {
        JwtService jwtService = jwtService(3600);
        String adminToken = jwtService.generateToken(user("person", "ADMIN"));

        assertFalse(jwtService.isTokenValid(adminToken, user("person", "ANALYST")));
    }

    @Test
    void revokedTokenIsRejected() {
        JwtService jwtService = jwtService(3600);
        UserDetails user = user("analyst", "ANALYST");
        String token = jwtService.generateToken(user);

        jwtService.revokeToken(token);

        assertFalse(jwtService.isTokenValid(token, user));
        assertThat(revokedTokenStore.revocations).hasSize(1);
    }

    @Test
    void replayOfRevokedTokenIsRejectedRepeatedly() {
        JwtService jwtService = jwtService(3600);
        UserDetails user = user("analyst", "ANALYST");
        String token = jwtService.generateToken(user);

        jwtService.revokeToken(token);

        assertFalse(jwtService.isTokenValid(token, user));
        assertFalse(jwtService.isTokenValid(token, user));
    }

    @Test
    void tokenWithUnknownKidIsRejected() {
        JwtService jwtService = jwtService(3600);
        UserDetails user = user("analyst", "ANALYST");
        long now = Instant.now().getEpochSecond();
        String token = signedToken("unknown-key", SECRET, claims("analyst", "ANALYST", now, now + 3600, "unknown-kid-jti"));

        assertFalse(jwtService.isTokenValid(token, user));
    }

    @Test
    void tokenSignedWithPreviousKeyIsAcceptedDuringRotation() throws Exception {
        JwtService jwtService = new JwtService(
                objectMapper,
                revokedTokenStore,
                SECRET,
                CURRENT_KID,
                OLD_KID + ":" + OLD_SECRET,
                3600
        );
        UserDetails user = user("analyst", "ANALYST");
        long now = Instant.now().getEpochSecond();
        String oldToken = signedToken(OLD_KID, OLD_SECRET, claims("analyst", "ANALYST", now, now + 3600, "old-jti"));

        assertTrue(jwtService.isTokenValid(oldToken, user));
        assertThat(header(jwtService.generateToken(user))).containsEntry("kid", CURRENT_KID);
    }

    @Test
    void tokenWithInvalidIssuerIsRejectedEvenWhenSignatureIsValid() {
        JwtService jwtService = jwtService(3600);
        UserDetails user = user("analyst", "ANALYST");
        long now = Instant.now().getEpochSecond();
        Map<String, Object> claims = claims("analyst", "ANALYST", now, now + 3600, "bad-issuer-jti");
        claims.put("iss", "evil-issuer");

        String token = signedToken(CURRENT_KID, SECRET, claims);

        assertFalse(jwtService.isTokenValid(token, user));
    }

    @Test
    void tokenWithInvalidAudienceIsRejectedEvenWhenSignatureIsValid() {
        JwtService jwtService = jwtService(3600);
        UserDetails user = user("analyst", "ANALYST");
        long now = Instant.now().getEpochSecond();
        Map<String, Object> claims = claims("analyst", "ANALYST", now, now + 3600, "bad-audience-jti");
        claims.put("aud", "other-api");

        String token = signedToken(CURRENT_KID, SECRET, claims);

        assertFalse(jwtService.isTokenValid(token, user));
    }

    private JwtService jwtService(long expirationSeconds) {
        return new JwtService(objectMapper, revokedTokenStore, SECRET, CURRENT_KID, "", expirationSeconds);
    }

    private Map<String, Object> header(String token) throws IOException {
        String[] parts = token.split("\\.", -1);
        byte[] header = Base64.getUrlDecoder().decode(parts[0]);
        return objectMapper.readValue(header, Map.class);
    }

    private Map<String, Object> payload(String token) throws IOException {
        String[] parts = token.split("\\.", -1);
        byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
        return objectMapper.readValue(payload, Map.class);
    }

    private Map<String, Object> claims(String subject, String role, long issuedAt, long expiresAt, String tokenId) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", subject);
        claims.put("iss", "ghostreport");
        claims.put("aud", "ghostreport-api");
        claims.put("jti", tokenId);
        claims.put("role", role);
        claims.put("iat", issuedAt);
        claims.put("exp", expiresAt);
        return claims;
    }

    private String signedToken(String kid, String secret, Map<String, Object> claims) {
        try {
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            header.put("kid", kid);

            String unsigned = encode(header) + "." + encode(claims);
            return unsigned + "." + sign(unsigned, secret);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String encode(Map<String, Object> values) throws IOException {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(values));
    }

    private String sign(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private UserDetails user(String username, String role) {
        return User.withUsername(username)
                .password("unused")
                .roles(role)
                .build();
    }

    private static final class RecordingRevokedTokenStore implements RevokedTokenStore {
        private final Map<String, Instant> revocations = new ConcurrentHashMap<>();

        @Override
        public void revoke(String tokenId, String subject, String keyId, Instant expiresAt) {
            revocations.put(tokenId, expiresAt);
        }

        @Override
        public boolean isRevoked(String tokenId, Instant now) {
            Instant expiresAt = revocations.get(tokenId);
            return expiresAt != null && expiresAt.isAfter(now);
        }

        @Override
        public void purgeExpired(Instant now) {
            revocations.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
        }
    }
}
