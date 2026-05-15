package com.ghostreport.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void loadFileRejectsRelativeTraversal() throws Exception {
        Path uploadDir = tempDir.resolve("uploads");
        Files.createDirectories(uploadDir);
        Files.writeString(tempDir.resolve("secret.txt"), "secret");

        SecurityMonitoringService monitoringService = mock(SecurityMonitoringService.class);
        FileStorageService service = new FileStorageService(uploadDir.toString(), monitoringService);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.loadFileAsResource("../secret.txt")
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Invalid file path", exception.getReason());
        verify(monitoringService).recordPathTraversalAttempt("../secret.txt");
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
    void storeAttachmentAcceptsValidPdfPngAndJpegSignatures() {
        Path uploadDir = tempDir.resolve("uploads");
        FileStorageService service = new FileStorageService(uploadDir.toString());

        FileStorageService.StoredFileInfo pdf = service.storeAttachment(
                1L,
                new MockMultipartFile(
                        "files",
                        "evidence.pdf",
                        "application/pdf",
                        "%PDF-1.4\nbody".getBytes(StandardCharsets.UTF_8)
                )
        );

        FileStorageService.StoredFileInfo png = service.storeAttachment(
                1L,
                new MockMultipartFile(
                        "files",
                        "image.png",
                        "image/png",
                        new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00}
                )
        );

        FileStorageService.StoredFileInfo jpeg = service.storeAttachment(
                1L,
                new MockMultipartFile(
                        "files",
                        "photo.jpg",
                        "image/jpeg",
                        new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01}
                )
        );

        assertTrue(Files.exists(uploadDir.resolve(pdf.storagePath())));
        assertTrue(Files.exists(uploadDir.resolve(png.storagePath())));
        assertTrue(Files.exists(uploadDir.resolve(jpeg.storagePath())));
    }

    @Test
    void storeAttachmentRejectsFakePdfMagicBytes() {
        FileStorageService service = new FileStorageService(tempDir.resolve("uploads").toString());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.storeAttachment(
                        1L,
                        new MockMultipartFile(
                                "files",
                                "evidence.pdf",
                                "application/pdf",
                                "MZ executable".getBytes(StandardCharsets.UTF_8)
                        )
                )
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("File signature does not match type", exception.getReason());
    }

    @Test
    void storeAttachmentRejectsExecutableRenamedToPdf() {
        FileStorageService service = new FileStorageService(tempDir.resolve("uploads").toString());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.storeAttachment(
                        1L,
                        new MockMultipartFile(
                                "files",
                                "payload.pdf",
                                "application/pdf",
                                new byte[]{0x4D, 0x5A, 0x00, 0x00}
                        )
                )
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("File signature does not match type", exception.getReason());
    }

    @Test
    void storeAttachmentRejectsMissingFilename() {
        FileStorageService service = new FileStorageService(tempDir.resolve("uploads").toString());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.storeAttachment(
                        1L,
                        new MockMultipartFile(
                                "files",
                                "",
                                "text/plain",
                                "text".getBytes(StandardCharsets.UTF_8)
                        )
                )
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Invalid filename", exception.getReason());
    }

    @Test
    void storeAttachmentRejectsPathTraversalFilename() {
        FileStorageService service = new FileStorageService(tempDir.resolve("uploads").toString());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.storeAttachment(
                        1L,
                        new MockMultipartFile(
                                "files",
                                "../evidence.txt",
                                "text/plain",
                                "text".getBytes(StandardCharsets.UTF_8)
                        )
                )
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Invalid filename", exception.getReason());
    }

    @Test
    void storeAttachmentRejectsOversizedFileUsingProductionLimit() {
        FileStorageService service = new FileStorageService(tempDir.resolve("uploads").toString());
        byte[] content = new byte[(10 * 1024 * 1024) + 1];

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.storeAttachment(
                        1L,
                        new MockMultipartFile(
                                "files",
                                "large.pdf",
                                "application/pdf",
                                content
                        )
                )
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Ficheiro demasiado grande", exception.getReason());
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
        try (var input = resource.getInputStream()) {
            assertEquals(
                    "allowed",
                    new String(input.readAllBytes(), StandardCharsets.UTF_8)
            );
        }
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
