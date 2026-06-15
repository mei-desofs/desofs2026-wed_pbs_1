package com.ghostreport.config;

import com.ghostreport.model.UserRole;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumSet;
import java.util.Set;

@ConfigurationProperties(prefix = "ghostreport.mfa")
public class MfaProperties {

    private boolean enabled = false;
    private Set<UserRole> requiredRoles = EnumSet.allOf(UserRole.class);
    private long codeTtlSeconds = 300;
    private int maxAttempts = 5;
    private boolean exposeCode = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<UserRole> getRequiredRoles() {
        return requiredRoles;
    }

    public void setRequiredRoles(Set<UserRole> requiredRoles) {
        this.requiredRoles = requiredRoles == null || requiredRoles.isEmpty()
                ? EnumSet.noneOf(UserRole.class)
                : EnumSet.copyOf(requiredRoles);
    }

    public long getCodeTtlSeconds() {
        return codeTtlSeconds;
    }

    public void setCodeTtlSeconds(long codeTtlSeconds) {
        this.codeTtlSeconds = codeTtlSeconds;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public boolean isExposeCode() {
        return exposeCode;
    }

    public void setExposeCode(boolean exposeCode) {
        this.exposeCode = exposeCode;
    }
}
