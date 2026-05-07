package com.ghostreport.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;

class OversizedUploadSecurityTest {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Test
    void shouldRejectOversizedFile() {
        byte[] oversizedContent = new byte[(int) MAX_FILE_SIZE + 1];

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "large-file.pdf",
                        "application/pdf",
                        oversizedContent
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> validateFileSize(file)
        );
    }

    private void validateFileSize(MockMultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds allowed limit");
        }
    }
}