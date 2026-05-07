package com.ghostreport.service;

import com.ghostreport.dto.CasePackageResponse;
import com.ghostreport.model.Attachment;
import com.ghostreport.model.CaseReview;
import com.ghostreport.model.ReportStatus;
import com.ghostreport.repository.AttachmentRepository;
import com.ghostreport.repository.CaseReviewRepository;
import com.ghostreport.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Service
public class CasePackageService {

    private final CaseReviewRepository caseReviewRepository;
    private final AttachmentRepository attachmentRepository;
    private final AuditLogService auditLogService;

    private final Path basePath = Paths.get("uploads").toAbsolutePath().normalize();

    public CasePackageService(
            CaseReviewRepository caseReviewRepository,
            AttachmentRepository attachmentRepository,
            AuditLogService auditLogService
    ) {
        this.caseReviewRepository = caseReviewRepository;
        this.attachmentRepository = attachmentRepository;
        this.auditLogService = auditLogService;
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

    private Path safeResolve(Path base, String child) {
        Path resolved = base.resolve(child).normalize();

        if (!resolved.startsWith(base)) {
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
