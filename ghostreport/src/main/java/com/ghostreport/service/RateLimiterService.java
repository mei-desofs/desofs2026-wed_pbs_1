package com.ghostreport.service;

import com.ghostreport.config.RateLimitProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final Map<String, WindowCounter> attempts = new ConcurrentHashMap<>();
    private final RateLimitProperties properties;
    private final Clock clock;

    @Autowired
    public RateLimiterService(RateLimitProperties properties) {
        this(properties, Clock.systemUTC());
    }

    RateLimiterService(RateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void checkTrackingLimit(String key) {
        check("tracking:" + key, properties.getTracking());
    }

    public void checkUploadLimit(String key) {
        check("upload:" + key, properties.getUpload());
    }

    public void checkDownloadLimit(String key) {
        check("download:" + key, properties.getDownload());
    }

    public void checkLimit(String ip) {
        check("default:" + ip, properties.getTracking());
    }

    private void check(String key, RateLimitProperties.Limit limit) {
        validateLimit(limit);

        Instant now = Instant.now(clock);
        Duration window = Duration.ofSeconds(limit.getWindowSeconds());

        WindowCounter counter = attempts.compute(key, (ignored, existing) -> {
            if (existing == null || !now.isBefore(existing.windowStart.plus(window))) {
                return new WindowCounter(1, now);
            }

            existing.count++;
            return existing;
        });

        cleanupExpired(now);

        if (counter.count > limit.getMaxAttempts()) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests"
            );
        }
    }

    private void validateLimit(RateLimitProperties.Limit limit) {
        if (limit.getMaxAttempts() < 1 || limit.getWindowSeconds() < 1) {
            throw new IllegalStateException("Rate limit configuration must be positive");
        }
    }

    private void cleanupExpired(Instant now) {
        attempts.entrySet().removeIf(entry -> {
            RateLimitProperties.Limit limit = resolveLimit(entry.getKey());
            return !now.isBefore(entry.getValue().windowStart.plusSeconds(limit.getWindowSeconds() * 2));
        });
    }

    private RateLimitProperties.Limit resolveLimit(String key) {
        if (key.startsWith("upload:")) {
            return properties.getUpload();
        }
        if (key.startsWith("download:")) {
            return properties.getDownload();
        }
        return properties.getTracking();
    }

    private static class WindowCounter {
        private int count;
        private final Instant windowStart;

        private WindowCounter(int count, Instant windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }
    }
}
