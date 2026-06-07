package com.ghostreport.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostreport.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceSecurityTest {

    private static final String SECRET = "test-secret-with-more-than-32-characters";

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void validTokenIsAcceptedForMatchingUserAndRole() {
        JwtService jwtService = new JwtService(objectMapper, SECRET, 3600);
        UserDetails user = user("analyst", "ANALYST");

        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwtService = new JwtService(objectMapper, SECRET, 3600);
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
        JwtService jwtService = new JwtService(objectMapper, SECRET, -1);
        UserDetails user = user("analyst", "ANALYST");

        String token = jwtService.generateToken(user);

        assertFalse(jwtService.isTokenValid(token, user));
    }

    @Test
    void tokenRoleMustMatchCurrentUserAuthorities() {
        JwtService jwtService = new JwtService(objectMapper, SECRET, 3600);
        String adminToken = jwtService.generateToken(user("person", "ADMIN"));

        assertFalse(jwtService.isTokenValid(adminToken, user("person", "ANALYST")));
    }

    @Test
    void generatedTokenContainsIssuerAudienceAndUniqueIdentifier() throws Exception {
        JwtService jwtService = new JwtService(objectMapper, SECRET, 3600);

        String token = jwtService.generateToken(user("analyst", "ANALYST"));
        Map<String, Object> payload = payload(token);

        assertThat(payload)
                .containsEntry("iss", "ghostreport")
                .containsEntry("aud", "ghostreport-api");
        assertThat(payload.get("jti")).isInstanceOf(String.class);
        assertThat((String) payload.get("jti")).isNotBlank();
    }

    @Test
    void revokedTokenIsRejected() {
        JwtService jwtService = new JwtService(objectMapper, SECRET, 3600);
        UserDetails user = user("analyst", "ANALYST");
        String token = jwtService.generateToken(user);

        jwtService.revokeToken(token);

        assertFalse(jwtService.isTokenValid(token, user));
    }

    private Map<String, Object> payload(String token) throws IOException {
        String[] parts = token.split("\\.", -1);
        byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
        return objectMapper.readValue(payload, Map.class);
    }

    private UserDetails user(String username, String role) {
        return User.withUsername(username)
                .password("unused")
                .roles(role)
                .build();
    }
}
