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

    @Test
    void reportSubmissionLimitIsIndependentFromTrackingLimit() {
        RateLimiterService service = new RateLimiterService(properties(1, 60), new MutableClock());

        service.checkReportLimit("client-a");

        assertDoesNotThrow(() -> service.checkTrackingLimit("client-a"));
        assertThrows(ResponseStatusException.class, () -> service.checkReportLimit("client-a"));
    }

    @Test
    void defaultLimitUsesTrackingConfiguration() {
        RateLimiterService service = new RateLimiterService(properties(1, 60), new MutableClock());

        service.checkLimit("10.0.0.1");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.checkLimit("10.0.0.1")
        );

        assertEquals(429, exception.getStatusCode().value());
    }

    @Test
    void loginFailuresBlockUntilCleared() {
        RateLimiterService service = new RateLimiterService(properties(2, 60), new MutableClock());

        assertEquals(false, service.recordLoginFailure("client-a"));
        assertEquals(true, service.recordLoginFailure("client-a"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.checkLoginAllowed("client-a")
        );

        assertEquals(429, exception.getStatusCode().value());

        service.clearLoginFailures("client-a");

        assertDoesNotThrow(() -> service.checkLoginAllowed("client-a"));
    }

    @Test
    void loginFailuresResetAfterWindowExpires() {
        MutableClock clock = new MutableClock();
        RateLimiterService service = new RateLimiterService(properties(1, 5), clock);

        assertEquals(true, service.recordLoginFailure("client-a"));
        assertThrows(ResponseStatusException.class, () -> service.checkLoginAllowed("client-a"));

        clock.advance(Duration.ofSeconds(6));

        assertDoesNotThrow(() -> service.checkLoginAllowed("client-a"));
        assertEquals(true, service.recordLoginFailure("client-a"));
    }

    @Test
    void invalidAttemptLimitIsRejected() {
        RateLimiterService service = new RateLimiterService(properties(0, 60), new MutableClock());

        assertThrows(
                IllegalStateException.class,
                () -> service.checkTrackingLimit("client-a")
        );
    }

    @Test
    void invalidWindowLimitIsRejectedForLoginFlow() {
        RateLimiterService service = new RateLimiterService(properties(1, 0), new MutableClock());

        assertThrows(
                IllegalStateException.class,
                () -> service.recordLoginFailure("client-a")
        );
    }

    @Test
    void invalidAttemptLimitIsRejectedBeforeCheckingLoginAllowance() {
        RateLimiterService service = new RateLimiterService(properties(0, 60), new MutableClock());

        assertThrows(
                IllegalStateException.class,
                () -> service.checkLoginAllowed("client-a")
        );
    }

    @Test
    void cleanupKeepsUploadDownloadAndLoginCountersWithinTheirOwnRetentionWindows() {
        MutableClock clock = new MutableClock();
        RateLimitProperties properties = properties(1, 1);
        properties.setUpload(new RateLimitProperties.Limit(1, 100));
        properties.setDownload(new RateLimitProperties.Limit(1, 100));
        properties.setLogin(new RateLimitProperties.Limit(1, 100));
        RateLimiterService service = new RateLimiterService(properties, clock);

        service.checkUploadLimit("upload-client");
        service.checkDownloadLimit("download-client");
        assertEquals(true, service.recordLoginFailure("login-client"));

        clock.advance(Duration.ofSeconds(60));
        service.checkTrackingLimit("sweeper");

        assertThrows(
                ResponseStatusException.class,
                () -> service.checkUploadLimit("upload-client")
        );
        assertThrows(
                ResponseStatusException.class,
                () -> service.checkDownloadLimit("download-client")
        );
        assertThrows(
                ResponseStatusException.class,
                () -> service.checkLoginAllowed("login-client")
        );
    }

    private RateLimitProperties properties(int maxAttempts, long windowSeconds) {
        RateLimitProperties properties = new RateLimitProperties();
        RateLimitProperties.Limit limit = new RateLimitProperties.Limit(maxAttempts, windowSeconds);
        properties.setTracking(limit);
        properties.setReport(limit);
        properties.setUpload(limit);
        properties.setDownload(limit);
        properties.setLogin(limit);
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
