package com.ghostreport.domain;

public record SafeFilename(String value) {

    public SafeFilename {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }

        if (value.contains("..")) {
            throw new IllegalArgumentException("Path traversal detected");
        }

        if (value.contains("/") || value.contains("\\")) {
            throw new IllegalArgumentException("Invalid path separator");
        }

        String lower = value.toLowerCase();

        if (lower.endsWith(".exe")
                || lower.endsWith(".bat")
                || lower.endsWith(".cmd")
                || lower.endsWith(".sh")) {

            throw new IllegalArgumentException("Executable files are not allowed");
        }

        if (value.length() > 255) {
            throw new IllegalArgumentException("Filename too long");
        }
    }
}