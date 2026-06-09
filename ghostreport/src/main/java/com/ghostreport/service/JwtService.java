package com.ghostreport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.regex.Pattern;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String ISSUER = "ghostreport";
    private static final String AUDIENCE = "ghostreport-api";
    private static final String DEFAULT_ACTIVE_KEY_ID = "primary";
    private static final Pattern KEY_ID_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,80}");
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final RevokedTokenStore revokedTokenStore;
    private final Map<String, byte[]> signingKeys;
    private final String activeKeyId;
    private final long expirationSeconds;

    @Autowired
    public JwtService(
            ObjectMapper objectMapper,
            RevokedTokenStore revokedTokenStore,
            @Value("${ghostreport.jwt.secret}") String secret,
            @Value("${ghostreport.jwt.active-key-id:primary}") String activeKeyId,
            @Value("${ghostreport.jwt.previous-secrets:}") String previousSecrets,
            @Value("${ghostreport.jwt.expiration-seconds:3600}") long expirationSeconds
    ) {
        if (expirationSeconds < 1) {
            throw new IllegalStateException("ghostreport.jwt.expiration-seconds must be positive");
        }
        this.objectMapper = objectMapper;
        this.revokedTokenStore = revokedTokenStore;
        this.activeKeyId = normalizeKeyId(activeKeyId);
        this.signingKeys = buildSigningKeys(secret, this.activeKeyId, previousSecrets);
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(UserDetails userDetails) {
        long now = Instant.now().getEpochSecond();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        header.put("kid", activeKeyId);

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
        return unsignedToken + "." + sign(unsignedToken, signingKeys.get(activeKeyId));
    }

    public String extractUsername(String token) {
        return readVerifiedClaims(token).subject();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public String getActiveKeyId() {
        return activeKeyId;
    }

    public void revokeToken(String token) {
        JwtClaims claims = readVerifiedClaims(token);
        revokedTokenStore.revoke(
                claims.tokenId(),
                claims.subject(),
                claims.keyId(),
                Instant.ofEpochSecond(claims.expiration())
        );
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

            JwtHeader header = readHeader(parts[0]);
            byte[] signingKey = signingKeys.get(header.keyId());
            if (signingKey == null) {
                throw new IllegalArgumentException("Unknown JWT key id");
            }

            if (!signatureMatches(parts[0] + "." + parts[1], parts[2], signingKey)) {
                throw new IllegalArgumentException("Invalid JWT signature");
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
            return new JwtClaims(username, authorityRole, jti, header.keyId(), exp.longValue());
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid JWT", e);
        }
    }

    private JwtHeader readHeader(String encodedHeader) throws IOException {
        Map<String, Object> header = objectMapper.readValue(
                BASE64_URL_DECODER.decode(encodedHeader),
                new TypeReference<>() {
                }
        );
        Object algorithm = header.get("alg");
        Object type = header.get("typ");
        Object keyId = header.get("kid");
        if (!"HS256".equals(algorithm)
                || !"JWT".equals(type)
                || !(keyId instanceof String kid)
                || !KEY_ID_PATTERN.matcher(kid).matches()) {
            throw new IllegalArgumentException("Invalid JWT header");
        }
        return new JwtHeader(kid);
    }

    private boolean isTokenRevoked(JwtClaims claims) {
        return revokedTokenStore.isRevoked(claims.tokenId(), Instant.now());
    }

    private boolean signatureMatches(String unsignedToken, String suppliedSignature, byte[] signingKey) {
        byte[] expected = sign(unsignedToken, signingKey).getBytes(StandardCharsets.US_ASCII);
        byte[] supplied = suppliedSignature.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, supplied);
    }

    private String sign(String value, byte[] signingKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not sign JWT", e);
        }
    }

    private Map<String, byte[]> buildSigningKeys(String secret, String activeKid, String previousSecrets) {
        Map<String, byte[]> keys = new LinkedHashMap<>();
        keys.put(activeKid, validateSecret("ghostreport.jwt.secret", secret));

        if (previousSecrets != null && !previousSecrets.isBlank()) {
            for (String entry : previousSecrets.split(",")) {
                if (entry.isBlank()) {
                    continue;
                }
                String[] parts = entry.split(":", 2);
                if (parts.length != 2) {
                    throw new IllegalStateException("ghostreport.jwt.previous-secrets entries must use kid:secret format");
                }
                String kid = normalizeKeyId(parts[0]);
                if (keys.containsKey(kid)) {
                    throw new IllegalStateException("Duplicate JWT key id: " + kid);
                }
                keys.put(kid, validateSecret("ghostreport.jwt.previous-secrets", parts[1]));
            }
        }

        return Map.copyOf(keys);
    }

    private String normalizeKeyId(String keyId) {
        String value = keyId == null || keyId.isBlank() ? DEFAULT_ACTIVE_KEY_ID : keyId.trim();
        if (!KEY_ID_PATTERN.matcher(value).matches()) {
            throw new IllegalStateException("JWT key id must match " + KEY_ID_PATTERN.pattern());
        }
        return value;
    }

    private byte[] validateSecret(String propertyName, String value) {
        if (value == null || value.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(propertyName + " must be configured with at least 32 characters");
        }
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private record JwtHeader(String keyId) {
    }

    private record JwtClaims(String subject, String role, String tokenId, String keyId, long expiration) {
    }
}
