package com.ghostreport.domain;

import java.util.Set;
import java.util.UUID;

public final class SafeFilename {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "png", "jpg", "jpeg", "txt");

    private final String value;

    public SafeFilename(String originalName) {

        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("Filename required");
        }

        String clean = originalName
                .replace("\\", "/");

        clean = clean.substring(
                clean.lastIndexOf("/") + 1
        );

        if (clean.contains("..")) {
            throw new IllegalArgumentException(
                    "Invalid filename"
            );
        }

        String extension = getExtension(clean);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Extension not allowed"
            );
        }

        this.value =
                UUID.randomUUID() + "_" + clean;
    }

    private String getExtension(String name) {

        int index = name.lastIndexOf(".");

        if (index < 0) {
            throw new IllegalArgumentException(
                    "File extension required"
            );
        }

        return name.substring(index + 1)
                .toLowerCase();
    }

    public String value() {
        return value;
    }
}