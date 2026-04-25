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
            @Value("${app.upload-dir}") String uploadDir,
            SecurityMonitoringService securityMonitoringService
    ) {
        this.baseStoragePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.securityMonitoringService = securityMonitoringService;
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

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
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
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public DocumentInfo generateReportDocument(Long reportId, String description, String category, String status) {
        try {
            Path documentsDir = getSafeReportDirectory(reportId, "documents");
            Files.createDirectories(documentsDir);

            String fileReference = UUID.randomUUID().toString();
            String storedName = "report-summary-" + fileReference + ".txt";

            Path targetLocation = documentsDir.resolve(storedName).normalize();
            ensureInsideBasePath(targetLocation);

            String content = """
                    GhostReport - Internal Report Document
                    
                    Report ID: %d
                    Category: %s
                    Status: %s
                    
                    Description:
                    %s
                    """.formatted(reportId, sanitizeText(category), sanitizeText(status), sanitizeText(description));

            Files.writeString(targetLocation, content, StandardOpenOption.CREATE_NEW);

            String hash = calculateSha256(targetLocation);

            return new DocumentInfo(
                    storedName,
                    fileReference,
                    targetLocation.toString(),
                    "text/plain",
                    Files.size(targetLocation),
                    hash
            );

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate report document", e);
        }
    }

    public Resource loadFileAsResource(String storagePath) {
        try {
            Path filePath = Paths.get(storagePath).toAbsolutePath().normalize();
            ensureInsideBasePath(filePath);

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not found or not readable");
            }

            return resource;

        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid file path", e);
        }
    }

    private Path getSafeReportDirectory(Long reportId, String subDirectory) {
        if (reportId == null || reportId <= 0) {
            throw new RuntimeException("Invalid report id");
        }

        if (!subDirectory.equals("attachments") && !subDirectory.equals("documents")) {
            securityMonitoringService.recordPathTraversalAttempt(subDirectory);
            throw new RuntimeException("Invalid storage directory");
        }

        Path directory = baseStoragePath
                .resolve("reports")
                .resolve(String.valueOf(reportId))
                .resolve(subDirectory)
                .normalize();

        ensureInsideBasePath(directory);
        return directory;
    }

    private void ensureInsideBasePath(Path path) {
        Path normalizedBase = baseStoragePath.toAbsolutePath().normalize();
        Path normalizedPath = path.toAbsolutePath().normalize();

        if (!normalizedPath.startsWith(normalizedBase)) {
            securityMonitoringService.recordPathTraversalAttempt(normalizedPath.toString());
            throw new RuntimeException("Invalid file path");
        }
    }

    private void validateFile(MultipartFile file, Long reportId) {
        if (file.isEmpty()) {
            securityMonitoringService.recordRejectedUpload(reportId, "Empty file");
            throw new RuntimeException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            securityMonitoringService.recordRejectedUpload(reportId, "File exceeds maximum allowed size");
            throw new RuntimeException("File exceeds maximum allowed size");
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            securityMonitoringService.recordRejectedUpload(reportId, "File type not allowed");
            throw new RuntimeException("File type not allowed");
        }

        String originalName = file.getOriginalFilename();
        if (originalName != null && (originalName.contains("..") || originalName.contains("/") || originalName.contains("\\"))) {
            securityMonitoringService.recordPathTraversalAttempt(originalName);
            throw new RuntimeException("Invalid file name");
        }
    }

    private String sanitizeOriginalName(String originalFilename) {
        String safeName = Path.of(originalFilename == null ? "unknown" : originalFilename)
                .getFileName()
                .toString();

        return safeName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extractExtension(String filename) {
        int index = filename.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        return filename.substring(index).toLowerCase();
    }

    private String sanitizeText(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\u0000", "")
                .replaceAll("[\\r\\n]{3,}", "\n\n");
    }

    private String calculateSha256(Path filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(filePath);
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Failed to calculate file hash", e);
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
    ) {
    }

    public record DocumentInfo(
            String storedName,
            String fileReference,
            String storagePath,
            String mimeType,
            long size,
            String hash
    ) {
    }
}