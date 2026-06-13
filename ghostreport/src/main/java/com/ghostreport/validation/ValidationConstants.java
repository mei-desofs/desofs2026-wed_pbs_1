package com.ghostreport.validation;

import java.util.Locale;

public final class ValidationConstants {

    public static final String REPORT_CATEGORY_ALLOWLIST =
            "^(Fraude|Fraud|Security|Privacy|Procurement|Ethics|Corruption|Harassment|Other)$";
    public static final String REPORT_STATUS_ALLOWLIST =
            "^(SUBMITTED|UNDER_REVIEW|MORE_INFO_REQUIRED|RESOLVED|REJECTED)$";
    public static final String CASE_PRIORITY_ALLOWLIST =
            "^(LOW|MEDIUM|HIGH|CRITICAL)$";
    public static final String USER_ROLE_ALLOWLIST =
            "^(ADMIN|ANALYST|AUDITOR)$";
    public static final String TRACKING_CODE_PATTERN =
            "^GR-[A-Za-z0-9_-]{20,64}$";
    public static final String USERNAME_PATTERN =
            "^[A-Za-z0-9._-]{3,120}$";

    private ValidationConstants() {
    }

    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    public static String upper(String value) {
        String trimmed = trim(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }
}
