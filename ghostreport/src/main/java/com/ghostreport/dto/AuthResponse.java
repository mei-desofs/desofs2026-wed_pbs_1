package com.ghostreport.dto;

public record AuthResponse(
        String token,
        String tokenType,
        String username,
        String role,
        long expiresInSeconds,
        boolean mfaRequired,
        String mfaChallengeId
) {
    public AuthResponse(
            String token,
            String tokenType,
            String username,
            String role,
            long expiresInSeconds
    ) {
        this(token, tokenType, username, role, expiresInSeconds, false, null);
    }

    public static AuthResponse mfaRequired(String username, String role, String challengeId) {
        return new AuthResponse(null, null, username, role, 0, true, challengeId);
    }
}
