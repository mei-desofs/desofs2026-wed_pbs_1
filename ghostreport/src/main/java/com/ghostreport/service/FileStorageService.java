package com.ghostreport.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "text/plain"
    );

    private final Path baseStoragePath;
    private final SecurityMonitoringService securityMonitoringService;

    public FileStorageService(
            @Value("${app.upload-dir:uploads}") String uploadDir,
            SecurityMonitoringService securityMonitoringService
    ) {
        this.baseStoragePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.securityMonitoringService = securityMonitoringService;

        try {
            Files.createDirectories(this.baseStoragePath); // 🔥 garante pasta
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public StoredFileInfo storeAttachment(Long reportId, MultipartFile file) {

        validateFile(file, reportId);

        try {
            Path attachmentsDir = getSafeReportDirectory(reportId, "attachments");
            Files.createDirectories(attachmentsDir);

            String originalName = sanitizeOriginalName(file.getOriginalFilename());
            String extension = extractExtension(originalName);

            String fileReference = UUID.randomUUID().toString();
            String storedName = fileReference + extension;

            Path targetLocation = attachmentsDir.resolve(storedName).normalize();
            ensureInsideBasePath(targetLocation);

            System.out.println("Saving file to: " + targetLocation); // DEBUG

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            if (!Files.exists(targetLocation)) {
                throw new RuntimeException("File was not saved!");
            }

            String hash = calculateSha256(targetLocation);

            return new StoredFileInfo(
                    originalName,
                    storedName,
                    fileReference,
                    targetLocation.toString(),
                    file.getContentType(),
                    file.getSize(),
                    hash
            );

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public Resource loadFileAsResource(String storagePath) {
        try {
            Path filePath = Paths.get(storagePath).toAbsolutePath().normalize();
            ensureInsideBasePath(filePath);

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not found");
            }

            return resource;

        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid path", e);
        }
    }

    private Path getSafeReportDirectory(Long reportId, String subDirectory) {

        Path directory = baseStoragePath
                .resolve("reports")
                .resolve(String.valueOf(reportId))
                .resolve(subDirectory)
                .normalize();

        ensureInsideBasePath(directory);
        return directory;
    }

    private void ensureInsideBasePath(Path path) {
        if (!path.toAbsolutePath().normalize().startsWith(baseStoragePath)) {
            throw new RuntimeException("Invalid path");
        }
    }

    private void validateFile(MultipartFile file, Long reportId) {

        if (file.isEmpty()) {
            throw new RuntimeException("Empty file");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File too large");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new RuntimeException("Invalid file type");
        }
    }

    private String sanitizeOriginalName(String name) {
        return name == null ? "file" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extractExtension(String filename) {
        int i = filename.lastIndexOf(".");
        return (i != -1) ? filename.substring(i) : "";
    }

    private String calculateSha256(Path filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(filePath);
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public record StoredFileInfo(
            String originalName,
            String storedName,
            String fileReference,
            String storagePath,
            String mimeType,
            long size,
            String hash
    ) {}
}