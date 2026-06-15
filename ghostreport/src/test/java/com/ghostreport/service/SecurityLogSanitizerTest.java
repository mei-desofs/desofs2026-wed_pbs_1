package com.ghostreport.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class SecurityLogSanitizerTest {

    private final SecurityLogSanitizer sanitizer = new SecurityLogSanitizer();

    @Test
    void nullValueRemainsNull() {
        assertNull(sanitizer.sanitize(null));
    }

    @Test
    void removesLineBreaksControlCharactersAndTrimsValue() {
        String sanitized = sanitizer.sanitize("  first\r\nsecond\u0000third\u007F  ");

        assertEquals("first  secondthird", sanitized);
    }

    @Test
    void redactsAuthorizationBearerHeader() {
        String sanitized = sanitizer.sanitize(
                "Authorization: Bearer abc.def.ghi"
        );

        assertEquals("Authorization: Bearer [REDACTED]", sanitized);
        assertFalse(sanitized.contains("abc.def.ghi"));
    }

    @Test
    void redactsStandaloneBearerToken() {
        String sanitized = sanitizer.sanitize(
                "token Bearer abcdefghijklmnop.qrstuvwxyz1234.abcdefghijkl"
        );

        assertEquals("token Bearer [REDACTED]", sanitized);
    }

    @Test
    void redactsJsonSecretFields() {
        String sanitized = sanitizer.sanitize(
                "{\"password\":\"secret\",\"trackingCode\":\"GR-abcdefghijklmnopqrst\"}"
        );

        assertEquals(
                "{\"password\":\"[REDACTED]\",\"trackingCode\":\"[REDACTED]\"}",
                sanitized
        );
    }

    @Test
    void redactsKeyValueSecretFields() {
        String sanitized = sanitizer.sanitize(
                "user=analyst password=secret token=abc123 trackingCode=GR-abcdefghijklmnopqrst"
        );

        assertEquals(
                "user=analyst password=[REDACTED] token=[REDACTED] trackingCode=[REDACTED]",
                sanitized
        );
    }

    @Test
    void redactsJwtLikeTokenEvenWithoutBearerPrefix() {
        String sanitized = sanitizer.sanitize(
                "jwt abcdefghij.klmnopqrst.uvwxyz12345 accepted"
        );

        assertEquals("jwt [REDACTED] accepted", sanitized);
    }

    @Test
    void truncatesLongSanitizedMessages() {
        String sanitized = sanitizer.sanitize("a".repeat(600));

        assertEquals(500, sanitized.length());
    }

    @Test
    void keepsMessagesWithExactlyFiveHundredCharacters() {
        String value = "a".repeat(500);

        String sanitized = sanitizer.sanitize(value);

        assertEquals(value, sanitized);
    }
}
