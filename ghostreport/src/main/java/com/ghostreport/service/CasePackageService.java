package com.ghostreport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostreport.dto.CasePackageResponse;
import com.ghostreport.dto.EvidencePackageFileCheckResponse;
import com.ghostreport.dto.EvidencePackageVerificationResponse;
import com.ghostreport.model.Attachment;
import com.ghostreport.model.CaseReview;
import com.ghostreport.model.ReportStatus;
import com.ghostreport.repository.AttachmentRepository;
import com.ghostreport.repository.CaseReviewRepository;
import com.ghostreport.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class CasePackageService {

    private final CaseReviewRepository caseReviewRepository;
    private final AttachmentRepository attachmentRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final Path basePath;

    public CasePackageService(
            CaseReviewRepository caseReviewRepository,
            AttachmentRepository attachmentRepository,
            AuditLogService auditLogService,
            ObjectMapper objectMapper,
            @Value("${app.upload-dir:uploads}") String uploadDir
    ) {
        this.caseReviewRepository = caseReviewRepository;
        this.attachmentRepository = attachmentRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
        this.basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public CasePackageResponse generateCasePackage(Long reportId) {
        CaseReview caseReview = caseReviewRepository.findByReportId(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case review not found"));

        if (!SecurityUtils.hasRole("ADMIN")) {
            String currentUsername = SecurityUtils.getCurrentUsername();

            if (caseReview.getAssignedAnalyst() == null ||
                    !caseReview.getAssignedAnalyst().getUsername().equals(currentUsername)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this case");
            }
        }

        ReportStatus status = caseReview.getReport().getStatus();

        if (status != ReportStatus.RESOLVED && status != ReportStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Case package can only be generated for closed cases");
        }

        try {
            Path reportPath = safeResolve(basePath, "reports/" + reportId);
            Path packagePath = safeResolve(reportPath, "case_package");
            Path packageAttachmentsPath = safeResolve(packagePath, "attachments");

            Files.createDirectories(packageAttachmentsPath);

            List<Attachment> attachments = attachmentRepository.findByReportId(reportId);

            StringBuilder manifest = new StringBuilder();
            StringBuilder hashes = new StringBuilder();

            manifest.append("{\n");
            manifest.append("  \"reportId\": ").append(reportId).append(",\n");
            manifest.append("  \"status\": \"").append(status.name()).append("\",\n");
            manifest.append("  \"attachments\": [\n");

            for (int i = 0; i < attachments.size(); i++) {
                Attachment attachment = attachments.get(i);

                Path source = safeResolve(basePath, attachment.getStoragePath());
                Path target = safeResolve(packageAttachmentsPath, attachment.getStoredName());

                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

                String hash = sha256(target);

                hashes.append(attachment.getOriginalName())
                        .append(" | ")
                        .append(hash)
                        .append(System.lineSeparator());

                manifest.append("    {\n");
                manifest.append("      \"originalName\": \"").append(escapeJson(attachment.getOriginalName())).append("\",\n");
                manifest.append("      \"storedName\": \"").append(escapeJson(attachment.getStoredName())).append("\",\n");
                manifest.append("      \"sha256\": \"").append(hash).append("\"\n");
                manifest.append("    }");

                if (i < attachments.size() - 1) {
                    manifest.append(",");
                }

                manifest.append("\n");
            }

            manifest.append("  ]\n");
            manifest.append("}\n");

            String summary = """
                    GhostReport - Case Package
                    
                    Report ID: %s
                    Status: %s
                    Category: %s
                    Description:
                    %s
                    
                    Internal Notes:
                    %s
                    """.formatted(
                    reportId,
                    status.name(),
                    caseReview.getReport().getCategory(),
                    caseReview.getReport().getDescription(),
                    caseReview.getNotes() == null ? "-" : caseReview.getNotes()
            );

            Files.writeString(safeResolve(packagePath, "case_summary.txt"), summary);
            Files.writeString(safeResolve(packagePath, "evidence_manifest.json"), manifest.toString());
            Files.writeString(safeResolve(packagePath, "integrity_hashes.txt"), hashes.toString());

            auditLogService.log(
                    "CASE_PACKAGE_GENERATED",
                    "REPORT",
                    reportId,
                    "Case package generated"
            );

            return new CasePackageResponse(
                    reportId,
                    status.name(),
                    packagePath.toString(),
                    List.of(
                            "case_summary.txt",
                            "evidence_manifest.json",
                            "integrity_hashes.txt",
                            "attachments/"
                    )
            );

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate case package");
        }
    }

    public EvidencePackageVerificationResponse verifyCasePackage(Long reportId) {
        CaseReview caseReview = caseReviewRepository.findByReportId(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case review not found"));

        ReportStatus status = caseReview.getReport().getStatus();
        if (status != ReportStatus.RESOLVED && status != ReportStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Evidence package is only available for closed cases");
        }

        try {
            Path reportPath = safeResolve(basePath, "reports/" + reportId);
            Path packagePath = safeResolve(reportPath, "case_package");
            Path manifestPath = safeResolve(packagePath, "evidence_manifest.json");

            if (!Files.exists(manifestPath)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence package manifest not found");
            }

            JsonNode manifest = objectMapper.readTree(Files.readString(manifestPath));
            if (manifest.path("reportId").asLong(-1) != reportId) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evidence package manifest does not match report");
            }

            JsonNode attachments = manifest.get("attachments");
            if (attachments == null || !attachments.isArray()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid evidence package manifest");
            }

            List<EvidencePackageFileCheckResponse> files = new ArrayList<>();
            int index = 0;
            for (JsonNode attachment : attachments) {
                String storedName = attachment.path("storedName").asText();
                String expectedSha256 = attachment.path("sha256").asText();

                if (storedName == null || storedName.isBlank() || storedName.contains("/") || storedName.contains("\\")
                        || storedName.contains("..") || expectedSha256 == null || expectedSha256.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid evidence package manifest");
                }

                Path file = safeResolve(packagePath, "attachments/" + storedName);
                if (!Files.exists(file)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evidence package file is missing");
                }

                String actualSha256 = sha256(file);
                boolean valid = expectedSha256.equals(actualSha256);
                if (!valid) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evidence package integrity validation failed");
                }

                files.add(new EvidencePackageFileCheckResponse(
                        index,
                        Files.size(file),
                        actualSha256,
                        true
                ));
                index++;
            }

            return new EvidencePackageVerificationResponse(
                    reportId,
                    status.name(),
                    true,
                    files.size(),
                    files,
                    "Evidence package is valid"
            );
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evidence package integrity validation failed");
        }
    }

    private Path safeResolve(Path base, String child) {
        Path normalizedBase = base.toAbsolutePath().normalize();
        Path resolved = normalizedBase.resolve(child).toAbsolutePath().normalize();

        if (!resolved.startsWith(normalizedBase)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
        }

        return resolved;
    }

    private String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (InputStream inputStream = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;

            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }

        return HexFormat.of().formatHex(digest.digest());
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
