package com.ghostreport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ghostreport.mfa")
public class MfaProperties {

    private boolean enabled = false;
    private boolean adminRequired = true;
    private long codeTtlSeconds = 300;
    private boolean exposeCode = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAdminRequired() {
        return adminRequired;
    }

    public void setAdminRequired(boolean adminRequired) {
        this.adminRequired = adminRequired;
    }

    public long getCodeTtlSeconds() {
        return codeTtlSeconds;
    }

    public void setCodeTtlSeconds(long codeTtlSeconds) {
        this.codeTtlSeconds = codeTtlSeconds;
    }

    public boolean isExposeCode() {
        return exposeCode;
    }

    public void setExposeCode(boolean exposeCode) {
        this.exposeCode = exposeCode;
    }
}
