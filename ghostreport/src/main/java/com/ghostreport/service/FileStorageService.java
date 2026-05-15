package com.ghostreport.service;

import com.ghostreport.domain.SafeFilename;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final Path baseStoragePath;
    private final SecurityMonitoringService securityMonitoringService;

    @Autowired
    public FileStorageService(
            @Value("${app.upload-dir:uploads}") String uploadDir,
            SecurityMonitoringService securityMonitoringService
    ) {
        this.baseStoragePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.securityMonitoringService = securityMonitoringService;

        try {
            Files.createDirectories(this.baseStoragePath);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar pasta uploads", e);
        }
    }

    FileStorageService(String uploadDir) {
        this(uploadDir, null);
    }

    public StoredFileInfo storeAttachment(Long reportId, MultipartFile file) {

        validateFile(file);

        try {
            Path attachmentsDir = baseStoragePath
                    .resolve("reports")
                    .resolve(String.valueOf(reportId))
                    .resolve("attachments")
                    .toAbsolutePath()
                    .normalize();

            ensureInsideBase(attachmentsDir);

            Files.createDirectories(attachmentsDir);
            ensureRealPathInsideBase(attachmentsDir);

            String originalName = sanitizeFilename(file.getOriginalFilename());
            String extension = getExtension(originalName);

            String fileRef = UUID.randomUUID().toString();
            String storedName = fileRef + extension;

            Path target = attachmentsDir.resolve(storedName)
                    .toAbsolutePath()
                    .normalize();

            ensureInsideBase(target);

            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }

            String hash = sha256(target);

            return new StoredFileInfo(
                    originalName,
                    storedName,
                    fileRef,
                    baseStoragePath.relativize(target).toString().replace('\\', '/'),
                    file.getContentType(),
                    file.getSize(),
                    hash
            );

        } catch (IOException e) {
            throw new RuntimeException("Erro ao guardar ficheiro", e);
        }
    }

    public void generateReportDocument(Long reportId, String description, String category, String status) {

        try {
            Path dir = baseStoragePath
                    .resolve("reports")
                    .resolve(String.valueOf(reportId))
                    .resolve("documents")
                    .toAbsolutePath()
                    .normalize();

            ensureInsideBase(dir);

            Files.createDirectories(dir);
            ensureRealPathInsideBase(dir);

            Path file = dir.resolve("report_" + reportId + ".txt")
                    .toAbsolutePath()
                    .normalize();

            ensureInsideBase(file);

            String content = """
                    === DENÚNCIA ===

                    ID: %d
                    Categoria: %s
                    Estado: %s

                    Descrição:
                    %s
                    """.formatted(reportId, category, status, description);

            Files.writeString(file, content,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            logger.info("Report document generated for report id={}", reportId);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao gerar documento", e);
        }
    }

    public Resource loadFileAsResource(String path) {
        try {
            Path file = resolveStoredPath(path);
            return new UrlResource(file.toUri());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Erro ao carregar ficheiro", e);
        }
    }

    private Path resolveStoredPath(String path) {
        if (path == null || path.isBlank()) {
            return rejectInvalidPath(path);
        }

        Path rawPath;

        try {
            rawPath = Paths.get(path);
        } catch (InvalidPathException e) {
            return rejectInvalidPath(path);
        }

        if (rawPath.isAbsolute()) {
            return rejectInvalidPath(path);
        }

        if (rawPath.normalize().startsWith("..") || path.contains("..")) {
            return rejectInvalidPath(path);
        }

        Path resolved = baseStoragePath.resolve(rawPath).toAbsolutePath().normalize();

        if (!Files.exists(resolved)) {
            Path legacyRelativePath = rawPath.toAbsolutePath().normalize();
            if (legacyRelativePath.startsWith(baseStoragePath)) {
                resolved = legacyRelativePath;
            }
        }

        ensureInsideBase(resolved);

        try {
            Path realBase = baseStoragePath.toRealPath();
            Path realFile = resolved.toRealPath();

            if (!realFile.startsWith(realBase) || !Files.isRegularFile(realFile)) {
                return rejectInvalidPath(path);
            }

            return realFile;
        } catch (NoSuchFileException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        } catch (IOException e) {
            return rejectInvalidPath(path);
        }
    }

    private void ensureInsideBase(Path path) {
        Path normalized = path.toAbsolutePath().normalize();

        if (!normalized.startsWith(baseStoragePath)) {
            rejectInvalidPath(path.toString());
        }
    }

    private void ensureRealPathInsideBase(Path path) throws IOException {
        Path realBase = baseStoragePath.toRealPath();
        Path realPath = path.toRealPath();

        if (!realPath.startsWith(realBase)) {
            rejectInvalidPath(path.toString());
        }
    }

    private void validateFile(MultipartFile file) {
        logger.info("Attachment upload validation started");

        if (file == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ficheiro vazio");
        }

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ficheiro vazio");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ficheiro demasiado grande");
        }

        String contentType = file.getContentType();
        String originalName = sanitizeFilename(file.getOriginalFilename());
        String extension = getExtension(originalName).toLowerCase(Locale.ROOT);

        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid file type"
            );
        }

        validateExtensionForContentType(extension, contentType);
        validateMagicBytes(file, extension, contentType);
    }

    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
        }

        String normalized = name.trim();

        try {
            new SafeFilename(normalized);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
        }

        return normalized.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String getExtension(String name) {
        int i = name.lastIndexOf(".");
        return (i != -1) ? name.substring(i) : "";
    }

    private String sha256(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(file);
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void validateExtensionForContentType(String extension, String contentType) {
        boolean valid = switch (contentType) {
            case "application/pdf" -> ".pdf".equals(extension);
            case "image/png" -> ".png".equals(extension);
            case "image/jpeg" -> ".jpg".equals(extension) || ".jpeg".equals(extension);
            case "text/plain" -> ".txt".equals(extension);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx".equals(extension);
            default -> false;
        };

        if (!valid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File extension does not match type");
        }
    }

    private void validateMagicBytes(MultipartFile file, String extension, String contentType) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(8192);
            boolean valid = switch (contentType) {
                case "application/pdf" -> startsWith(header, new byte[]{0x25, 0x50, 0x44, 0x46});
                case "image/png" -> startsWith(header, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
                case "image/jpeg" -> startsWith(header, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                        ".docx".equals(extension) && startsWith(header, new byte[]{0x50, 0x4B, 0x03, 0x04});
                case "text/plain" -> isTextLike(header);
                default -> false;
            };

            if (!valid) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File signature does not match type");
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not validate file");
        }
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }

        for (int i = 0; i < prefix.length; i++) {
            if (value[i] != prefix[i]) {
                return false;
            }
        }

        return true;
    }

    private boolean isTextLike(byte[] bytes) {
        for (byte value : bytes) {
            int unsigned = value & 0xFF;
            if (unsigned == 0) {
                return false;
            }
            if (unsigned < 0x20 && unsigned != '\n' && unsigned != '\r' && unsigned != '\t') {
                return false;
            }
        }
        return true;
    }

    private Path rejectInvalidPath(String input) {
        if (securityMonitoringService != null) {
            securityMonitoringService.recordPathTraversalAttempt(input);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
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
