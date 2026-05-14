package com.ghostreport.service;

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
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final Path baseStoragePath;

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.baseStoragePath = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.baseStoragePath);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar pasta uploads", e);
        }
    }

    public StoredFileInfo storeAttachment(Long reportId, MultipartFile file) {

        validateFile(file);

        try {
            Path attachmentsDir = baseStoragePath
                    .resolve("reports")
                    .resolve(String.valueOf(reportId))
                    .resolve("attachments");

            Files.createDirectories(attachmentsDir);

            String originalName = sanitize(file.getOriginalFilename());
            String extension = getExtension(originalName);

            String fileRef = UUID.randomUUID().toString();
            String storedName = fileRef + extension;

            Path target = attachmentsDir.resolve(storedName);

            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }

            String hash = sha256(target);

            return new StoredFileInfo(
                    originalName,
                    storedName,
                    fileRef,
                    target.toString(),
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
                    .resolve("documents");

            Files.createDirectories(dir);

            Path file = dir.resolve("report_" + reportId + ".txt");

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
            Path file = Paths.get(path).toAbsolutePath();
            return new UrlResource(file.toUri());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Erro ao carregar ficheiro", e);
        }
    }

    private void validateFile(MultipartFile file) {
        logger.info("Attachment upload validation started");

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ficheiro vazio");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ficheiro demasiado grande");
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tipo de ficheiro inválido: " + contentType
            );
        }
    }

    private String sanitize(String name) {
        return name == null ? "file" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
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
