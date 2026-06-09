package com.ghostreport.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest
    @ValueSource(strings = {
            "../secret.txt",
            "..\\secret.txt",
            "reports/1/attachments/../../../../secret.txt"
    })
    void loadFileRejectsTraversalPath(String storagePath) throws Exception {
        Path uploadDir = tempDir.resolve("uploads");
        Files.createDirectories(uploadDir);
        Files.writeString(tempDir.resolve("secret.txt"), "secret");

        SecurityMonitoringService monitoringService = mock(SecurityMonitoringService.class);
        FileStorageService service = new FileStorageService(uploadDir.toString(), monitoringService);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.loadFileAsResource(storagePath)
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Invalid file path", exception.getReason());
        verify(monitoringService).recordPathTraversalAttempt(storagePath);
    }

    @Test
    void loadFileRejectsAbsolutePathOutsideBase() throws Exception {
        Path uploadDir = tempDir.resolve("uploads");
        Files.createDirectories(uploadDir);
        Path outsideFile = tempDir.resolve("outside.txt");
        Files.writeString(outsideFile, "outside");

        FileStorageService service = new FileStorageService(uploadDir.toString());
        String outsideStoragePath = outsideFile.toString();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.loadFileAsResource(outsideStoragePath)
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Invalid file path", exception.getReason());
    }

    @Test
    void loadFileRejectsAbsolutePathInsideBase() throws Exception {
        Path uploadDir = tempDir.resolve("uploads");
        Path attachment = uploadDir.resolve("reports/1/attachments/file.txt");
        Files.createDirectories(attachment.getParent());
        Files.writeString(attachment, "inside");

        FileStorageService service = new FileStorageService(uploadDir.toString());
        String absoluteAttachmentPath = attachment.toString();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.loadFileAsResource(absoluteAttachmentPath)
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Invalid file path", exception.getReason());
    }

    @Test
    void storeAttachmentUsesServerGeneratedNameForValidFilename() {
        Path uploadDir = tempDir.resolve("uploads");
        FileStorageService service = new FileStorageService(uploadDir.toString());

        FileStorageService.StoredFileInfo stored = service.storeAttachment(
                1L,
                new MockMultipartFile(
                        "files",
                        "evidence.txt",
                        "text/plain",
                        "valid text evidence".getBytes(StandardCharsets.UTF_8)
                )
        );

        assertEquals("evidence.txt", stored.originalName());
        assertTrue(stored.storedName().matches("[0-9a-f-]{36}\\.txt"));
        assertTrue(stored.storagePath().matches("reports/1/attachments/[0-9a-f-]{36}\\.txt"));
        assertTrue(Files.exists(uploadDir.resolve(stored.storagePath())));
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
        MockMultipartFile fakePdf = new MockMultipartFile(
                "files",
                "evidence.pdf",
                "application/pdf",
                "MZ executable".getBytes(StandardCharsets.UTF_8)
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.storeAttachment(1L, fakePdf)
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("File signature does not match type", exception.getReason());
    }

    @Test
    void storeAttachmentRejectsExtensionThatDoesNotMatchMimeType() {
        FileStorageService service = new FileStorageService(tempDir.resolve("uploads").toString());
        MockMultipartFile mismatchedExtension = new MockMultipartFile(
                "files",
                "evidence.pdf",
                "text/plain",
                "plain text evidence".getBytes(StandardCharsets.UTF_8)
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.storeAttachment(1L, mismatchedExtension)
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("File extension does not match type", exception.getReason());
    }

    @Test
    void storeAttachmentRejectsExecutableRenamedToPdf() {
        FileStorageService service = new FileStorageService(tempDir.resolve("uploads").toString());
        MockMultipartFile executable = new MockMultipartFile(
                "files",
                "payload.pdf",
                "application/pdf",
                new byte[]{0x4D, 0x5A, 0x00, 0x00}
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.storeAttachment(1L, executable)
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("File signature does not match type", exception.getReason());
    }

    @Test
    void storeAttachmentRejectsScannerMalwareFindingAndQuarantinesFile() throws Exception {
        Path uploadDir = tempDir.resolve("uploads");
        SecurityMonitoringService monitoringService = mock(SecurityMonitoringService.class);
        MalwareScanner scanner = (inputStream, originalFilename, contentType) ->
                MalwareScanner.ScanResult.malicious("test scanner finding");
        FileStorageService service = new FileStorageService(uploadDir.toString(), monitoringService, scanner);

        MockMultipartFile malware = new MockMultipartFile(
                "files",
                "evidence.txt",
                "text/plain",
                "valid text with suspicious scanner result".getBytes(StandardCharsets.UTF_8)
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.storeAttachment(1L, malware)
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("File rejected by malware scanner", exception.getReason());
        assertEquals(0, countRegularFiles(uploadDir.resolve("reports/1/attachments")));
        assertEquals(1, countRegularFiles(uploadDir.resolve("quarantine/reports/1")));
        verify(monitoringService).recordMalwareUploadRejected(1L);
    }

    @Test
    void storeAttachmentRejectsMissingFilename() {
        FileStorageService service = new FileStorageService(tempDir.resolve("uploads").toString());
        MockMultipartFile missingFilename = new MockMultipartFile(
                "files",
                "",
                "text/plain",
                "text".getBytes(StandardCharsets.UTF_8)
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.storeAttachment(1L, missingFilename)
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Invalid filename", exception.getReason());
    }

    @Test
    void storeAttachmentRejectsPathTraversalFilename() {
        FileStorageService service = new FileStorageService(tempDir.resolve("uploads").toString());
        MockMultipartFile traversalFilename = new MockMultipartFile(
                "files",
                "../evidence.txt",
                "text/plain",
                "text".getBytes(StandardCharsets.UTF_8)
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.storeAttachment(1L, traversalFilename)
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Invalid filename", exception.getReason());
    }

    @Test
    void storeAttachmentRejectsOversizedFileUsingProductionLimit() {
        FileStorageService service = new FileStorageService(tempDir.resolve("uploads").toString());
        byte[] content = new byte[(10 * 1024 * 1024) + 1];
        MockMultipartFile oversized = new MockMultipartFile(
                "files",
                "large.pdf",
                "application/pdf",
                content
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.storeAttachment(1L, oversized)
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
        String outsideStoragePath = outsideFile.toString();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.loadFileAsResource(outsideStoragePath)
        );

        assertEquals("Invalid file path", exception.getReason());
        assertFalse(
                exception.getMessage().contains(tempDir.toString()),
                "Error message should not expose internal filesystem paths"
        );
    }

    private long countRegularFiles(Path path) throws Exception {
        if (path == null || !Files.exists(path)) {
            return 0;
        }

        try (var files = Files.walk(path)) {
            return files.filter(Files::isRegularFile).count();
        }
    }
}
