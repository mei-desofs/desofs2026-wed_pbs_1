package com.ghostreport.domain;

import java.security.SecureRandom;
import java.util.Base64;

public final class TrackingCode {

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private final String value;

    private TrackingCode(String value) {

        if (value == null ||
                !value.matches("GR-[A-Za-z0-9_-]{20,}")) {

            throw new IllegalArgumentException(
                    "Invalid tracking code"
            );
        }

        this.value = value;
    }

    public static TrackingCode generate() {

        byte[] bytes = new byte[18];

        RANDOM.nextBytes(bytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        return new TrackingCode("GR-" + token);
    }

    public static TrackingCode from(String value) {
        return new TrackingCode(value);
    }

    public String value() {
        return value;
    }
}