package com.ghostreport.dto;

public record AuthResponse(
        String token,
        String tokenType,
        String username,
        String role,
        long expiresInSeconds,
        boolean mfaRequired,
        String mfaChallengeId,
        String devMfaCode
) {
    public AuthResponse(
            String token,
            String tokenType,
            String username,
            String role,
            long expiresInSeconds
    ) {
        this(token, tokenType, username, role, expiresInSeconds, false, null, null);
    }

    public static AuthResponse mfaRequired(String username, String role, String challengeId, String devMfaCode) {
        return new AuthResponse(null, null, username, role, 0, true, challengeId, devMfaCode);
    }
}
