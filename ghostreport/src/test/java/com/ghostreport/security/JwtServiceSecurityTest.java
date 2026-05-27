package com.ghostreport.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostreport.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceSecurityTest {

    private static final String SECRET = "test-secret-with-more-than-32-characters";

    @Test
    void validTokenIsAcceptedForMatchingUserAndRole() {
        JwtService jwtService = new JwtService(new ObjectMapper(), SECRET, 3600);
        UserDetails user = user("analyst", "ANALYST");

        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwtService = new JwtService(new ObjectMapper(), SECRET, 3600);
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
        JwtService jwtService = new JwtService(new ObjectMapper(), SECRET, -1);
        UserDetails user = user("analyst", "ANALYST");

        String token = jwtService.generateToken(user);

        assertFalse(jwtService.isTokenValid(token, user));
    }

    @Test
    void tokenRoleMustMatchCurrentUserAuthorities() {
        JwtService jwtService = new JwtService(new ObjectMapper(), SECRET, 3600);
        String adminToken = jwtService.generateToken(user("person", "ADMIN"));

        assertFalse(jwtService.isTokenValid(adminToken, user("person", "ANALYST")));
    }

    private UserDetails user(String username, String role) {
        return User.withUsername(username)
                .password("unused")
                .roles(role)
                .build();
    }
}
