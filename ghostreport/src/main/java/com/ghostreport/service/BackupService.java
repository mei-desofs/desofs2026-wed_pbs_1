package com.ghostreport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostreport.dto.BackupFileResponse;
import com.ghostreport.dto.BackupManifestSummaryResponse;
import com.ghostreport.dto.BackupOperationResponse;
import com.ghostreport.dto.BackupRestoreResponse;
import com.ghostreport.dto.BackupVerificationResponse;
import com.ghostreport.model.Attachment;
import com.ghostreport.model.AuditLog;
import com.ghostreport.model.CaseReview;
import com.ghostreport.model.Report;
import com.ghostreport.model.SecurityAlert;
import com.ghostreport.model.User;
import com.ghostreport.repository.AttachmentRepository;
import com.ghostreport.repository.AuditLogRepository;
import com.ghostreport.repository.CaseReviewRepository;
import com.ghostreport.repository.ReportRepository;
import com.ghostreport.repository.SecurityAlertRepository;
import com.ghostreport.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {

    private static final String BACKUP_PREFIX = "ghostreport-backup-";
    private static final String BACKUP_SUFFIX = ".zip";
    private static final Pattern SAFE_BACKUP_NAME = Pattern.compile("^ghostreport-backup-\\d{8}-\\d{6}\\.zip$");
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String FORMAT_VERSION = "1";

    private final ReportRepository reportRepository;
    private final AttachmentRepository attachmentRepository;
    private final CaseReviewRepository caseReviewRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final SecurityAlertRepository securityAlertRepository;
    private final AuditLogService auditLogService;
    private final SecurityMonitoringService securityMonitoringService;
    private final ObjectMapper objectMapper;
    private final Path backupDir;
    private final Path uploadDir;

    @Value("${ghostreport.backup-enabled:true}")
    private boolean backupEnabled;

    public BackupService(
            ReportRepository reportRepository,
            AttachmentRepository attachmentRepository,
            CaseReviewRepository caseReviewRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            SecurityAlertRepository securityAlertRepository,
            AuditLogService auditLogService,
            SecurityMonitoringService securityMonitoringService,
            ObjectMapper objectMapper,
            @Value("${ghostreport.backup-dir:backups}") String backupDir,
            @Value("${app.upload-dir:uploads}") String uploadDir
    ) {
        this.reportRepository = reportRepository;
        this.attachmentRepository = attachmentRepository;
        this.caseReviewRepository = caseReviewRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.securityAlertRepository = securityAlertRepository;
        this.auditLogService = auditLogService;
        this.securityMonitoringService = securityMonitoringService;
        this.objectMapper = objectMapper;
        this.backupDir = Paths.get(backupDir).toAbsolutePath().normalize();
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void initialize() throws IOException {
        if (backupDir.equals(uploadDir)) {
            throw new IllegalStateException("ghostreport.backup-dir cannot be the same as app.upload-dir");
        }
        Files.createDirectories(backupDir);
        Files.createDirectories(uploadDir);
    }

    public BackupOperationResponse createBackup() {
        if (!backupEnabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Backups are disabled");
        }

        String filename = BACKUP_PREFIX + LocalDateTime.now().format(FILE_TIMESTAMP) + BACKUP_SUFFIX;
        Path target = backupDir.resolve(filename).normalize();

        try {
            ensureInsideBackupDir(target);
            BackupWriteResult result = writeBackupZip(target);
            String zipHash = sha256(target);
            Files.writeString(sidecarHashPath(target), zipHash);

            auditLogService.log(
                    "BACKUP_CREATED",
                    "BACKUP",
                    null,
                    "Backup created: " + filename + " sha256=" + zipHash
            );

            return new BackupOperationResponse(filename, Files.size(target), zipHash, result.totalFiles(), "Backup created");
        } catch (Exception e) {
            auditLogService.log("BACKUP_ERROR", "BACKUP", null, "Backup creation failed");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Backup creation failed");
        }
    }

    public List<BackupFileResponse> listBackups() {
        try (Stream<Path> stream = Files.list(backupDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> SAFE_BACKUP_NAME.matcher(path.getFileName().toString()).matches())
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .map(this::toBackupFileResponse)
                    .toList();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not list backups");
        }
    }

    public Resource getBackupResource(String filename) {
        Path path = resolveBackup(filename);
        if (!Files.exists(path)) {
            securityMonitoringService.recordMissingBackupDownload(filename);
            auditLogService.log("BACKUP_ERROR", "BACKUP", null, "Backup download failed: missing file");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Backup not found");
        }

        try {
            auditLogService.log("BACKUP_DOWNLOADED", "BACKUP", null, "Backup downloaded: " + filename);
            return new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            auditLogService.log("BACKUP_ERROR", "BACKUP", null, "Backup download failed");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not download backup");
        }
    }

    public BackupVerificationResponse verifyBackup(String filename) {
        Path path = resolveExistingBackup(filename);
        try {
            VerificationResult result = verifyBackupFile(path);
            auditLogService.log("BACKUP_VALIDATED", "BACKUP", null, "Backup validated: " + filename);
            return new BackupVerificationResponse(filename, true, result.zipSha256(), result.checkedFiles(), "Backup is valid");
        } catch (Exception e) {
            securityMonitoringService.recordBackupIntegrityFailure(filename);
            auditLogService.log("BACKUP_ERROR", "BACKUP", null, "Backup validation failed: " + filename);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Backup integrity validation failed");
        }
    }

    public BackupManifestSummaryResponse getBackupManifestSummary(String filename) {
        Path path = resolveExistingBackup(filename);
        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            ZipEntry manifestEntry = zipFile.getEntry("manifest.json");
            if (manifestEntry == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Backup manifest not found");
            }

            JsonNode manifest;
            try (InputStream input = zipFile.getInputStream(manifestEntry)) {
                manifest = objectMapper.readTree(input);
            }

            Map<String, Integer> databaseExports = new LinkedHashMap<>();
            JsonNode exports = manifest.get("databaseExports");
            if (exports != null && exports.isObject()) {
                exports.fields().forEachRemaining(entry -> databaseExports.put(entry.getKey(), entry.getValue().asInt()));
            }

            return new BackupManifestSummaryResponse(
                    filename,
                    manifest.path("formatVersion").asText(null),
                    manifest.path("createdAt").toString(),
                    manifest.path("totalFiles").asInt(0),
                    databaseExports
            );
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            securityMonitoringService.recordBackupIntegrityFailure(filename);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read backup manifest");
        }
    }

    public BackupRestoreResponse restoreBackup(String filename) {
        Path path = resolveExistingBackup(filename);
        try {
            verifyBackupFile(path);

            Path restoreRoot = backupDir
                    .resolve("restore-staging")
                    .resolve(filename.replace(BACKUP_SUFFIX, ""))
                    .resolve(LocalDateTime.now().format(FILE_TIMESTAMP))
                    .normalize();
            ensureInsideBackupDir(restoreRoot);
            Files.createDirectories(restoreRoot);

            try (ZipFile zipFile = new ZipFile(path.toFile())) {
                var entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) {
                        continue;
                    }
                    ensureSafeZipEntry(entry.getName());
                    Path output = restoreRoot.resolve(entry.getName()).normalize();
                    if (!output.startsWith(restoreRoot)) {
                        throw new IOException("Unsafe restore entry");
                    }
                    Files.createDirectories(output.getParent());
                    try (InputStream input = zipFile.getInputStream(entry)) {
                        Files.copy(input, output);
                    }
                }
            }

            auditLogService.log(
                    "BACKUP_RESTORE_STAGED",
                    "BACKUP",
                    null,
                    "Backup extracted to restore staging area: " + filename
            );
            return new BackupRestoreResponse(
                    filename,
                    true,
                    restoreRoot.toString(),
                    "Backup validated and extracted to controlled staging directory. Live DB/uploads were not overwritten."
            );
        } catch (Exception e) {
            securityMonitoringService.recordBackupIntegrityFailure(filename);
            auditLogService.log("BACKUP_ERROR", "BACKUP", null, "Backup restore failed: " + filename);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Backup restore failed");
        }
    }

    @Scheduled(cron = "${ghostreport.backup-schedule-cron:0 0 2 * * *}")
    public void createScheduledBackup() {
        if (!backupEnabled) {
            return;
        }

        try {
            createBackup();
        } catch (RuntimeException e) {
            auditLogService.log("BACKUP_ERROR", "BACKUP", null, "Scheduled backup failed");
        }
    }

    private BackupWriteResult writeBackupZip(Path target) throws IOException {
        List<ManifestFileEntry> manifestEntries = new ArrayList<>();
        Map<String, Integer> exportedData = new LinkedHashMap<>();

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            writeJson(zip, manifestEntries, exportedData, "db/reports.json", exportReports(), "reports");
            writeJson(zip, manifestEntries, exportedData, "db/attachments.json", exportAttachments(), "attachments");
            writeJson(zip, manifestEntries, exportedData, "db/case-reviews.json", exportCaseReviews(), "caseReviews");
            writeJson(zip, manifestEntries, exportedData, "db/users.json", exportUsers(), "users");
            writeJson(zip, manifestEntries, exportedData, "db/audit-logs.json", exportAuditLogs(), "auditLogs");
            writeJson(zip, manifestEntries, exportedData, "db/security-alerts.json", exportSecurityAlerts(), "securityAlerts");
            writeUploadedFiles(zip, manifestEntries);

            Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("createdAt", LocalDateTime.now());
            manifest.put("formatVersion", FORMAT_VERSION);
            manifest.put("files", manifestEntries);
            manifest.put("totalFiles", manifestEntries.size());
            manifest.put("databaseExports", exportedData);

            byte[] manifestBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest);
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(manifestBytes);
            zip.closeEntry();
        }

        return new BackupWriteResult(manifestEntries.size());
    }

    private void writeJson(
            ZipOutputStream zip,
            List<ManifestFileEntry> manifestEntries,
            Map<String, Integer> exportedData,
            String entryName,
            List<Map<String, Object>> rows,
            String exportName
    ) throws IOException {
        byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(rows);
        writeZipEntry(zip, manifestEntries, entryName, bytes);
        exportedData.put(exportName, rows.size());
    }

    private void writeUploadedFiles(ZipOutputStream zip, List<ManifestFileEntry> manifestEntries) throws IOException {
        if (!Files.exists(uploadDir)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(uploadDir)) {
            for (Path file : stream.filter(Files::isRegularFile).toList()) {
                Path normalized = file.toAbsolutePath().normalize();
                if (!normalized.startsWith(uploadDir)) {
                    continue;
                }
                Path relative = uploadDir.relativize(normalized);
                String entryName = "files/" + relative.toString().replace('\\', '/');
                ensureSafeZipEntry(entryName);
                writeZipEntry(zip, manifestEntries, entryName, Files.readAllBytes(normalized));
            }
        }
    }

    private void writeZipEntry(
            ZipOutputStream zip,
            List<ManifestFileEntry> manifestEntries,
            String entryName,
            byte[] bytes
    ) throws IOException {
        ensureSafeZipEntry(entryName);
        zip.putNextEntry(new ZipEntry(entryName));
        zip.write(bytes);
        zip.closeEntry();
        manifestEntries.add(new ManifestFileEntry(entryName, bytes.length, sha256(bytes)));
    }

    private VerificationResult verifyBackupFile(Path path) throws Exception {
        String zipSha256 = sha256(path);

        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            ZipEntry manifestEntry = zipFile.getEntry("manifest.json");
            if (manifestEntry == null) {
                throw new IOException("Missing manifest");
            }

            JsonNode manifest;
            try (InputStream input = zipFile.getInputStream(manifestEntry)) {
                manifest = objectMapper.readTree(input);
            }

            JsonNode files = manifest.get("files");
            if (files == null || !files.isArray()) {
                throw new IOException("Invalid manifest files");
            }

            int checked = 0;
            for (JsonNode fileNode : files) {
                String entryName = fileNode.path("path").asText();
                String expectedHash = fileNode.path("sha256").asText();
                ensureSafeZipEntry(entryName);

                ZipEntry entry = zipFile.getEntry(entryName);
                if (entry == null) {
                    throw new IOException("Missing backup entry");
                }

                byte[] bytes;
                try (InputStream input = zipFile.getInputStream(entry)) {
                    bytes = input.readAllBytes();
                }

                if (!sha256(bytes).equals(expectedHash)) {
                    throw new IOException("Hash mismatch");
                }
                checked++;
            }

            if (manifest.path("totalFiles").asInt(-1) != checked) {
                throw new IOException("Manifest total mismatch");
            }

            Path sidecar = sidecarHashPath(path);
            if (Files.exists(sidecar)) {
                String expectedZipHash = Files.readString(sidecar).trim();
                if (!expectedZipHash.equals(zipSha256)) {
                    throw new IOException("ZIP sidecar hash mismatch");
                }
            }

            return new VerificationResult(zipSha256, checked);
        }
    }

    private Path resolveExistingBackup(String filename) {
        Path path = resolveBackup(filename);
        if (!Files.exists(path)) {
            securityMonitoringService.recordMissingBackupDownload(filename);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Backup not found");
        }
        return path;
    }

    private Path resolveBackup(String filename) {
        if (filename == null || !SAFE_BACKUP_NAME.matcher(filename).matches()) {
            securityMonitoringService.recordBackupPathTraversalAttempt(filename);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid backup filename");
        }

        Path resolved = backupDir.resolve(filename).normalize();
        ensureInsideBackupDir(resolved);
        return resolved;
    }

    private void ensureInsideBackupDir(Path path) {
        if (!path.toAbsolutePath().normalize().startsWith(backupDir)) {
            securityMonitoringService.recordBackupPathTraversalAttempt(path.toString());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid backup path");
        }
    }

    private void ensureSafeZipEntry(String entryName) throws IOException {
        if (entryName == null || entryName.isBlank() || entryName.startsWith("/") || entryName.contains("\\")
                || entryName.contains("..") || Paths.get(entryName).normalize().startsWith("..")) {
            securityMonitoringService.recordBackupPathTraversalAttempt(entryName);
            throw new IOException("Unsafe ZIP entry");
        }
    }

    private BackupFileResponse toBackupFileResponse(Path path) {
        try {
            return new BackupFileResponse(
                    path.getFileName().toString(),
                    Files.size(path),
                    sha256(path),
                    LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), java.time.ZoneId.systemDefault())
            );
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read backup metadata");
        }
    }

    private Path sidecarHashPath(Path zipPath) {
        return zipPath.resolveSibling(zipPath.getFileName().toString() + ".sha256");
    }

    private String sha256(Path file) {
        try {
            return sha256(Files.readAllBytes(file));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not hash file");
        }
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private List<Map<String, Object>> exportReports() {
        return reportRepository.findAll().stream().map(this::reportToMap).toList();
    }

    private List<Map<String, Object>> exportAttachments() {
        return attachmentRepository.findAll().stream().map(this::attachmentToMap).toList();
    }

    private List<Map<String, Object>> exportCaseReviews() {
        return caseReviewRepository.findAll().stream().map(this::caseReviewToMap).toList();
    }

    private List<Map<String, Object>> exportUsers() {
        return userRepository.findAll().stream().map(this::userToMap).toList();
    }

    private List<Map<String, Object>> exportAuditLogs() {
        return auditLogRepository.findAll().stream().map(this::auditLogToMap).toList();
    }

    private List<Map<String, Object>> exportSecurityAlerts() {
        return securityAlertRepository.findAll().stream().map(this::securityAlertToMap).toList();
    }

    private Map<String, Object> reportToMap(Report report) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", report.getId());
        row.put("title", report.getTitle());
        row.put("description", report.getDescription());
        row.put("category", report.getCategory());
        row.put("status", report.getStatus() != null ? report.getStatus().name() : null);
        row.put("trackingCodeHash", report.getTrackingCodeHash());
        row.put("createdAt", report.getCreatedAt());
        return row;
    }

    private Map<String, Object> attachmentToMap(Attachment attachment) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", attachment.getId());
        row.put("reportId", attachment.getReport() != null ? attachment.getReport().getId() : null);
        row.put("originalName", attachment.getOriginalName());
        row.put("storedName", attachment.getStoredName());
        row.put("mimeType", attachment.getMimeType());
        row.put("size", attachment.getSize());
        row.put("hash", attachment.getHash());
        row.put("storagePath", attachment.getStoragePath());
        row.put("fileReference", attachment.getFileReference());
        return row;
    }

    private Map<String, Object> caseReviewToMap(CaseReview caseReview) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", caseReview.getId());
        row.put("reportId", caseReview.getReport() != null ? caseReview.getReport().getId() : null);
        row.put("assignedAnalystId", caseReview.getAssignedAnalyst() != null ? caseReview.getAssignedAnalyst().getId() : null);
        row.put("assignedAnalystUsername", caseReview.getAssignedAnalyst() != null ? caseReview.getAssignedAnalyst().getUsername() : null);
        row.put("notes", caseReview.getNotes());
        row.put("priority", caseReview.getPriority() != null ? caseReview.getPriority().name() : null);
        row.put("updatedAt", caseReview.getUpdatedAt());
        return row;
    }

    private Map<String, Object> userToMap(User user) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", user.getId());
        row.put("username", user.getUsername());
        row.put("email", user.getEmail());
        row.put("passwordHash", user.getPasswordHash());
        row.put("role", user.getRole() != null ? user.getRole().name() : null);
        row.put("active", user.isActive());
        row.put("createdAt", user.getCreatedAt());
        return row;
    }

    private Map<String, Object> auditLogToMap(AuditLog auditLog) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", auditLog.getId());
        row.put("timestamp", auditLog.getTimestamp());
        row.put("actor", auditLog.getActor());
        row.put("action", auditLog.getAction());
        row.put("targetType", auditLog.getTargetType());
        row.put("targetId", auditLog.getTargetId());
        row.put("details", auditLog.getDetails());
        return row;
    }

    private Map<String, Object> securityAlertToMap(SecurityAlert alert) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", alert.getId());
        row.put("timestamp", alert.getTimestamp());
        row.put("alertType", alert.getAlertType());
        row.put("severity", alert.getSeverity());
        row.put("actor", alert.getActor());
        row.put("targetType", alert.getTargetType());
        row.put("targetId", alert.getTargetId());
        row.put("description", alert.getDescription());
        return row;
    }

    private record ManifestFileEntry(String path, long size, String sha256) {
    }

    private record BackupWriteResult(int totalFiles) {
    }

    private record VerificationResult(String zipSha256, int checkedFiles) {
    }
}
