package com.ghostreport.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SecurityLogSanitizer {

    private static final String REDACTED = "[REDACTED]";

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x1F\\x7F]");
    private static final Pattern LINE_BREAKS = Pattern.compile("[\\r\\n]");
    private static final Pattern AUTHORIZATION_HEADER = Pattern.compile(
            "(?i)(Authorization\\s*[:=]\\s*Bearer\\s+)[A-Za-z0-9._~+/=-]+"
    );
    private static final Pattern BEARER_TOKEN = Pattern.compile(
            "(?i)(Bearer\\s+)[A-Za-z0-9._~+/=-]+"
    );
    private static final Pattern JSON_SECRET_FIELD = Pattern.compile(
            "(?i)(\"(?:password|currentPassword|newPassword|token|trackingCode|authorization)\"\\s*:\\s*\")[^\"]*(\")"
    );
    private static final Pattern KEY_VALUE_SECRET_FIELD = Pattern.compile(
            "(?i)\\b(password|currentPassword|newPassword|token|trackingCode|authorization)=([^\\s,;]+)"
    );
    private static final Pattern TRACKING_CODE = Pattern.compile("GR-[A-Za-z0-9_-]{20,}");
    private static final Pattern JWT_LIKE_TOKEN = Pattern.compile(
            "\\b[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\b"
    );

    public String sanitize(String value) {
        if (value == null) {
            return null;
        }

        String sanitized = LINE_BREAKS.matcher(value).replaceAll(" ");
        sanitized = CONTROL_CHARS.matcher(sanitized).replaceAll("");
        sanitized = AUTHORIZATION_HEADER.matcher(sanitized).replaceAll("$1" + REDACTED);
        sanitized = BEARER_TOKEN.matcher(sanitized).replaceAll("$1" + REDACTED);
        sanitized = JSON_SECRET_FIELD.matcher(sanitized).replaceAll("$1" + REDACTED + "$2");
        sanitized = KEY_VALUE_SECRET_FIELD.matcher(sanitized).replaceAll("$1=" + REDACTED);
        sanitized = TRACKING_CODE.matcher(sanitized).replaceAll(REDACTED);
        sanitized = JWT_LIKE_TOKEN.matcher(sanitized).replaceAll(REDACTED);

        if (sanitized.length() > 500) {
            sanitized = sanitized.substring(0, 500);
        }

        return sanitized.trim();
    }
}
