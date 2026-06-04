# OWASP ASVS Level 2 Evidence

This file complements `docs/ASVS_EVIDENCE.md` with a Sprint 2 presentation
mapping focused on Level 2 evidence. Status values are: Implemented, Partial,
Evidence mode or Future work.

| ASVS area | Requirement focus | Evidence in GhostReport | Relevant files | Status | Improvements needed |
| --- | --- | --- | --- | --- | --- |
| V2 Authentication | Password storage and login abuse protection | BCrypt, inactive-user login block, login rate limiting, brute-force alerting | `SecurityConfig`, `AuthService`, `RateLimiterService`, `LoginRateLimitSecurityTest`, `AdminUserManagementSecurityTest` | Implemented | Add MFA for privileged roles. |
| V2 Authentication | Secure credentials handling | Passwords validated on create and never returned in user DTOs | `CreateUserRequest`, `UserResponse`, `UserService` | Implemented | Add password reset/change flows if scope expands. |
| V3 Session Management | Stateless authenticated sessions | JWT tokens signed with HS256, expiry checked, role claim validated | `JwtService`, `JwtAuthenticationFilter`, `JwtServiceSecurityTest` | Implemented | Add token revocation/rotation for production. |
| V3 Session Management | Session invalidation | Inactive users cannot authenticate and tokens are invalid if user is disabled when checked | `AuthService`, `JwtService`, `CustomUserDetailsService` | Partial | Add explicit revocation list for issued tokens. |
| V4 Access Control | Centralized RBAC | `ADMIN`, `ANALYST`, `AUDITOR` route rules | `SecurityConfig`, `RbacAuthorizationMatrixTest` | Implemented | Keep matrix updated for new endpoints. |
| V4 Access Control | Object-level authorization | Analysts can only access assigned/eligible cases | `ReportService`, `CaseReviewService`, `AnalystCaseOwnershipTest` | Implemented | Add more negative tests when workflows expand. |
| V5 Validation | Request validation | Bean Validation on request DTOs | `CreateReportRequest`, `CreateUserRequest`, `Update*Request`, controllers | Implemented | Convert more mutable DTO classes to records where framework-compatible. |
| V5 Validation | Domain invariants | Tracking code format, safe filenames, report description length/content | `TrackingCode`, `SafeFilename`, `ReportDescription`, domain tests | Implemented | Move more entity state transitions behind domain methods. |
| V5 Validation | File upload safety | Extension, MIME, magic bytes, path normalization, upload count limit | `FileStorageService`, `ReportControllerAttachmentUploadTest`, `SafeFilenameSecurityTest` | Implemented baseline | Add malware scanning and storage quotas. |
| V7 Error Handling | Generic API errors | Stack traces disabled, controlled security errors with correlation IDs | `GlobalExceptionHandler`, `SecurityConfig`, `ErrorHandlingSecurityTest` | Implemented | Propagate correlation IDs into structured logs. |
| V7 Error Handling | Information disclosure prevention | Tests check unauthorized/malformed responses do not leak internals | `ErrorHandlingSecurityTest` | Implemented | Continue adding tests for new endpoints. |
| V8 Data Protection | Secrets outside code | Env vars, `.env.example`, Gitleaks, GitHub secrets | `application.yaml`, `.gitleaks.toml`, `secret-scan-gitleaks.yml` | Implemented | Add operational secret rotation runbook. |
| V8 Data Protection | Sensitive data in logs | Audit/security details are sanitized and avoid passwords/tokens | `AuditLogService`, `SecurityMonitoringService`, `RuntimeSecurityEventLoggingTest` | Implemented baseline | Add tamper-proof log storage. |
| V9 Communication Security | HTTPS/TLS posture | HSTS configured by Spring Security for secure deployments | `SecurityConfig` | Partial | Production TLS termination is operational and not in repo. |
| V10 Malicious Code Prevention | Static/security analysis | SpotBugs and CodeQL workflows produce evidence | `sast-spotbugs.yml`, `sast-codeql.yml` | Evidence mode | Triage findings and raise gates over time. |
| V10 Malicious Code Prevention | Dependency risk | Dependency-Check and CycloneDX SBOM | `sca-dependency-check.yml`, `sbom-cyclonedx.yml`, `pom.xml` | Evidence mode | Review CVEs and document accepted risks. |
| V10 Malicious Code Prevention | Secret scanning | Gitleaks scans repository root | `secret-scan-gitleaks.yml`, `.gitleaks.toml` | Implemented | Rotate any real secret if ever found. |
| V11 Business Logic | Controlled state changes | Closed cases cannot be modified; assignments avoid conflicts | `CaseReviewService`, `ClosedCaseSecurityTest`, `AnalystCaseOwnershipTest` | Implemented | Add workflow diagrams for state transitions. |
| V13 API Security | DTO responses | API uses DTOs/records instead of directly exposing entities | `dto` package, controllers | Implemented | Keep this as a PR review checklist item. |
| V13 API Security | API abuse controls | Rate limits for login, tracking, upload and download | `RateLimiterService`, security tests | Implemented baseline | Move limiter to shared store for multi-instance deployment. |
| V13 API Security | DAST | ZAP baseline against live local app in GitHub Actions | `dast-zap.yml` | Evidence mode | Add authenticated scans for role-specific surfaces. |

## Level 2 Claim Boundaries

GhostReport can defend a coursework-level ASVS Level 2 baseline for implemented
features. It should not claim complete production ASVS Level 2 coverage because
MFA, authenticated DAST, tamper-proof audit logs, malware scanning, distributed
rate limiting and operational TLS deployment are not fully implemented.
