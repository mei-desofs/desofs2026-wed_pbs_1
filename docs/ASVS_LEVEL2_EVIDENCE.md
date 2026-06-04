# OWASP ASVS Level 2 Evidence

This file focuses on Level 2 evidence for the implemented GhostReport features.
Each claim below is tied to code, tests, configuration or GitHub Actions
artifacts.

| ASVS area | Requirement focus | Evidence in GhostReport | Relevant files | Status |
| --- | --- | --- | --- | --- |
| V2 Authentication | Password storage and login abuse protection | BCrypt, inactive-user login block, login rate limiting and brute-force alerts | `SecurityConfig`, `AuthService`, `RateLimiterService`, `LoginRateLimitSecurityTest`, `AdminUserManagementSecurityTest` | Implemented |
| V2 Authentication | Credential exposure prevention | Passwords are accepted through request DTOs and omitted from responses/log tests | `CreateUserRequest`, `UserResponse`, `UserService`, `RuntimeSecurityEventLoggingTest` | Implemented |
| V3 Session Management | Stateless API session | JWT signed with HS256, expiry checked, role claim validated | `JwtService`, `JwtAuthenticationFilter`, `JwtServiceSecurityTest` | Implemented |
| V4 Access Control | Role-based access control | Centralized ADMIN/ANALYST/AUDITOR route rules and authorization matrix tests | `SecurityConfig`, `RbacAuthorizationMatrixTest` | Implemented |
| V4 Access Control | Object-level authorization | Analysts can access only assigned/eligible cases | `ReportService`, `CaseReviewService`, `AnalystCaseOwnershipTest` | Implemented |
| V5 Validation | Request validation | Bean Validation on request DTOs | `CreateReportRequest`, `CreateUserRequest`, `Update*Request`, controllers | Implemented |
| V5 Validation | Domain invariants | Tracking code, safe filename and report description primitives | `TrackingCode`, `SafeFilename`, `ReportDescription`, domain tests | Implemented |
| V5 Validation | Upload validation | Size, extension, MIME, magic-byte and safe-path validation | `FileStorageService`, `ReportControllerAttachmentUploadTest`, `SafeFilenameSecurityTest` | Implemented |
| V7 Error Handling | Controlled errors | Generic errors, no stack traces and correlation IDs | `GlobalExceptionHandler`, `SecurityConfig`, `ErrorHandlingSecurityTest` | Implemented |
| V8 Data Protection | Secret management | Env-var configuration, fail-fast JWT secret validation and Gitleaks scan | `application.yaml`, `.env.example`, `.gitleaks.toml`, `SecurityConfigurationValidatorTest` | Implemented |
| V8 Data Protection | Evidence integrity | SHA-256 hashes for attachments, evidence packages and backup manifests | `FileStorageService`, `CasePackageService`, `BackupService` | Implemented |
| V9 Communication Security | Browser-facing secure headers | HSTS, CSP, frame protection, referrer policy and permissions policy | `SecurityConfig`, `SecurityHeadersTest` | Implemented |
| V10 Malicious Code Prevention | Static/security analysis | SpotBugs and CodeQL workflows | `.github/workflows/sast-spotbugs.yml`, `.github/workflows/sast-codeql.yml` | Evidence review |
| V10 Malicious Code Prevention | Dependency risk | Dependency-Check and CycloneDX SBOM workflows | `.github/workflows/sca-dependency-check.yml`, `.github/workflows/sbom-cyclonedx.yml` | Evidence review |
| V10 Malicious Code Prevention | Runtime security evidence | IAST/runtime security workflow and OWASP ZAP baseline | `.github/workflows/iast-runtime.yml`, `.github/workflows/dast-zap.yml` | Evidence review |
| V11 Business Logic | Controlled case state changes | Closed-case modification protection and assignment conflict checks | `CaseReviewService`, `ClosedCaseSecurityTest`, `AnalystCaseOwnershipTest` | Implemented |
| V13 API Security | DTO API contracts | Controllers return DTOs/records, including audit and security alert responses | `dto` package, controllers | Implemented |
| V13 API Security | Abuse controls | Rate limits for login, tracking, upload and download flows | `RateLimiterService`, security tests | Implemented |

## Scope Boundaries

The Level 2 evidence is scoped to implemented application features and automated
pipeline evidence. Advanced production operations such as external SIEM/WORM
storage, token revocation, privileged-user MFA and authenticated DAST contexts
are treated as operational hardening beyond the current sprint scope.
