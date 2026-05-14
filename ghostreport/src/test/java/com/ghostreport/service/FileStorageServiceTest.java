package com.ghostreport.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void loadFileRejectsRelativeTraversal() throws Exception {
        Path uploadDir = tempDir.resolve("uploads");
        Files.createDirectories(uploadDir);
        Files.writeString(tempDir.resolve("secret.txt"), "secret");

        FileStorageService service = new FileStorageService(uploadDir.toString());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.loadFileAsResource("../secret.txt")
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Invalid file path", exception.getReason());
    }

    @Test
    void loadFileRejectsAbsolutePathOutsideBase() throws Exception {
        Path uploadDir = tempDir.resolve("uploads");
        Files.createDirectories(uploadDir);
        Path outsideFile = tempDir.resolve("outside.txt");
        Files.writeString(outsideFile, "outside");

        FileStorageService service = new FileStorageService(uploadDir.toString());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.loadFileAsResource(outsideFile.toString())
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Invalid file path", exception.getReason());
    }

    @Test
    void loadFileAllowsValidPathInsideBase() throws Exception {
        Path uploadDir = tempDir.resolve("uploads");
        Path attachment = uploadDir.resolve("reports/1/attachments/file.txt");
        Files.createDirectories(attachment.getParent());
        Files.writeString(attachment, "allowed");

        FileStorageService service = new FileStorageService(uploadDir.toString());

        Resource resource = service.loadFileAsResource("reports/1/attachments/file.txt");

        assertTrue(resource.exists(), "Expected resource inside upload directory to exist");
        assertEquals(
                "allowed",
                new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
        );
    }

    @Test
    void loadFileDoesNotExposeInternalPathOnError() throws Exception {
        Path uploadDir = tempDir.resolve("uploads");
        Files.createDirectories(uploadDir);
        Path outsideFile = tempDir.resolve("outside.txt");
        Files.writeString(outsideFile, "outside");

        FileStorageService service = new FileStorageService(uploadDir.toString());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.loadFileAsResource(outsideFile.toString())
        );

        assertEquals("Invalid file path", exception.getReason());
        assertFalse(
                exception.getMessage().contains(tempDir.toString()),
                "Error message should not expose internal filesystem paths"
        );
    }
}
