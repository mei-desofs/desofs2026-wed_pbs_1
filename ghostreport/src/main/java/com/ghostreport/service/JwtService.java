package com.ghostreport.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${ghostreport.jwt.secret:change-this-development-secret}") String secret,
            @Value("${ghostreport.jwt.expiration-seconds:3600}") long expirationSeconds
    ) {
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
        return readPayload(token).get("sub").toString();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Map<String, Object> payload = readPayload(token);
            String username = payload.get("sub").toString();
            long exp = ((Number) payload.get("exp")).longValue();

            return username.equals(userDetails.getUsername())
                    && exp > Instant.now().getEpochSecond()
                    && signatureMatches(token);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private String encodeJson(Map<String, Object> values) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(values));
        } catch (Exception e) {
            throw new IllegalStateException("Could not encode JWT", e);
        }
    }

    private Map<String, Object> readPayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT structure");
            }

            byte[] payloadBytes = BASE64_URL_DECODER.decode(parts[1]);
            return objectMapper.readValue(payloadBytes, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT", e);
        }
    }

    private boolean signatureMatches(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        return sign(parts[0] + "." + parts[1]).equals(parts[2]);
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign JWT", e);
        }
    }
}
