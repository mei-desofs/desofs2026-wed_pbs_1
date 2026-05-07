package com.ghostreport.domain;

public final class ReportDescription {

    private final String value;

    public ReportDescription(String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }

        String normalized = value.trim();

        if (normalized.length() < 10) {
            throw new IllegalArgumentException("Description too short");
        }

        if (normalized.length() > 3000) {
            throw new IllegalArgumentException("Description too long");
        }

        this.value = normalized;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}