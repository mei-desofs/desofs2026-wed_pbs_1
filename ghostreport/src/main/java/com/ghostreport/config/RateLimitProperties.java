package com.ghostreport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.rate-limit")
public class RateLimitProperties {

    private Limit tracking = new Limit(10, 60);
    private Limit upload = new Limit(10, 60);
    private Limit download = new Limit(10, 60);
    private Limit login = new Limit(5, 600);

    public Limit getTracking() {
        return new Limit(tracking);
    }

    public void setTracking(Limit tracking) {
        this.tracking = new Limit(tracking);
    }

    public Limit getUpload() {
        return new Limit(upload);
    }

    public void setUpload(Limit upload) {
        this.upload = new Limit(upload);
    }

    public Limit getDownload() {
        return new Limit(download);
    }

    public void setDownload(Limit download) {
        this.download = new Limit(download);
    }

    public Limit getLogin() {
        return new Limit(login);
    }

    public void setLogin(Limit login) {
        this.login = new Limit(login);
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

        private Limit(Limit source) {
            this(
                    source == null ? 10 : source.maxAttempts,
                    source == null ? 60 : source.windowSeconds
            );
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
