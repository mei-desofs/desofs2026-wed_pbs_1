package com.ghostreport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.rate-limit")
public class RateLimitProperties {

    private Limit tracking = new Limit(10, 60);
    private Limit upload = new Limit(10, 60);
    private Limit download = new Limit(10, 60);

    public Limit getTracking() {
        return tracking;
    }

    public void setTracking(Limit tracking) {
        this.tracking = tracking;
    }

    public Limit getUpload() {
        return upload;
    }

    public void setUpload(Limit upload) {
        this.upload = upload;
    }

    public Limit getDownload() {
        return download;
    }

    public void setDownload(Limit download) {
        this.download = download;
    }

    public static class Limit {
        private int maxAttempts;
        private long windowSeconds;

        public Limit() {
            this(10, 60);
        }

        public Limit(int maxAttempts, long windowSeconds) {
            this.maxAttempts = maxAttempts;
            this.windowSeconds = windowSeconds;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(long windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }
}
