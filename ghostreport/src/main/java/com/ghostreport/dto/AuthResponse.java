package com.ghostreport.dto;

public record AuthResponse(
        String token,
        String tokenType,
        String username,
        String role,
        long expiresInSeconds
) {
}
