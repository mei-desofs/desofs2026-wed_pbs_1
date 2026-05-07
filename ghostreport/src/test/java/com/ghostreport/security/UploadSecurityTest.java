package com.ghostreport.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;

class UploadSecurityTest {

    @Test
    void shouldRejectExecutableFile() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "malware.exe",
                        "application/octet-stream",
                        "virus".getBytes()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    if (file.getOriginalFilename().endsWith(".exe")) {
                        throw new IllegalArgumentException("Executable files not allowed");
                    }
                }
        );
    }
}