package com.ghostreport.security;

import java.util.UUID;

public final class CorrelationId {

    public static final String HEADER = "X-Correlation-ID";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CorrelationId() {
    }

    public static String current() {
        String value = CURRENT.get();
        if (value == null || value.isBlank()) {
            value = UUID.randomUUID().toString();
            CURRENT.set(value);
        }
        return value;
    }

    public static void set(String value) {
        CURRENT.set(isSafe(value) ? value.trim() : UUID.randomUUID().toString());
    }

    public static void clear() {
        CURRENT.remove();
    }

    private static boolean isSafe(String value) {
        return value != null
                && !value.isBlank()
                && value.length() <= 80
                && value.matches("[A-Za-z0-9._:-]+");
    }
}
