package com.ghostreport.service;

import com.ghostreport.config.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimiterServiceTest {

    @Test
    void requestsBelowLimitSucceedAndAboveLimitReturns429() {
        RateLimiterService service = new RateLimiterService(properties(2, 60), new MutableClock());

        assertDoesNotThrow(() -> service.checkTrackingLimit("client-a"));
        assertDoesNotThrow(() -> service.checkTrackingLimit("client-a"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.checkTrackingLimit("client-a")
        );

        assertEquals(429, exception.getStatusCode().value());
        assertEquals("Too many requests", exception.getReason());
    }

    @Test
    void counterResetsAfterWindowExpires() {
        MutableClock clock = new MutableClock();
        RateLimiterService service = new RateLimiterService(properties(1, 5), clock);

        service.checkUploadLimit("client-a");
        assertThrows(ResponseStatusException.class, () -> service.checkUploadLimit("client-a"));

        clock.advance(Duration.ofSeconds(6));

        assertDoesNotThrow(() -> service.checkUploadLimit("client-a"));
    }

    @Test
    void independentKeysDoNotAffectEachOther() {
        RateLimiterService service = new RateLimiterService(properties(1, 60), new MutableClock());

        service.checkDownloadLimit("client-a");

        assertDoesNotThrow(() -> service.checkDownloadLimit("client-b"));
        assertThrows(ResponseStatusException.class, () -> service.checkDownloadLimit("client-a"));
    }

    private RateLimitProperties properties(int maxAttempts, long windowSeconds) {
        RateLimitProperties properties = new RateLimitProperties();
        RateLimitProperties.Limit limit = new RateLimitProperties.Limit(maxAttempts, windowSeconds);
        properties.setTracking(limit);
        properties.setUpload(limit);
        properties.setDownload(limit);
        return properties;
    }

    private static class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-05-15T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
