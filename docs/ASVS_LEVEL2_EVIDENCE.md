# OWASP ASVS Level 2 Evidence

This file focuses on Level 2 evidence for the implemented GhostReport features.
Each claim below is tied to code, tests, configuration or GitHub Actions
artifacts.

| ASVS area | Requirement focus | Evidence in GhostReport | Relevant files | Status |
| --- | --- | --- | --- | --- |
| V2 Authentication | Password storage and login abuse protection | BCrypt, inactive-user login block, login rate limiting and brute-force alerts | `SecurityConfig`, `AuthService`, `RateLimiterService`, `LoginRateLimitSecurityTest`, `AdminUserManagementSecurityTest` | Implemented |
| V2 Authentication | Credential exposure prevention | Passwords are accepted through request DTOs and omitted from responses/log tests | `CreateUserRequest`, `UserResponse`, `UserService`, `RuntimeSecurityEventLoggingTest` | Implemented |
| V6 Authentication | Password policy and compromised password screening | Local denylist rejects known weak/compromised examples before hashing | `PasswordPolicyService`, `UserService`, `PasswordPolicyAndResetSecurityTest` | Implemented |
| V6 Authentication | Password history and reuse prevention | Current password and last stored password hashes are checked with `PasswordEncoder.matches` before accepting a new password | `PasswordHistory`, `PasswordHistoryRepository`, `PasswordPolicyService`, `PasswordPolicyAndResetSecurityTest` | Implemented |
| V6 Authentication | Authenticated password change | Authenticated endpoint requires the current password and stores only a new BCrypt hash | `AuthController`, `ChangePasswordRequest`, `UserService`, `PasswordPolicyAndResetSecurityTest` | Implemented |
| V6 Authentication | Password reset | Reset tokens are generated with `SecureRandom`, stored as SHA-256 hashes, expire, are one-time use and create audit events | `PasswordResetService`, `PasswordResetToken`, `PasswordResetTokenRepository`, `PasswordResetRequest`, `PasswordResetConfirmRequest`, `PasswordPolicyAndResetSecurityTest` | Implemented |
| V6 Authentication | MFA evaluation | MFA is not implemented because the coursework scope uses local username/password plus JWT and has no external authenticator, email/SMS or IdP integration | `docs/ASVS_EVIDENCE.md`, `docs/SECURITY_ASSESSMENT.md` | Out of scope for Sprint 2 |
| V3 Session Management | Stateless API session | JWT signed with HS256, expiry checked, role claim validated and logout revokes the current token | `JwtService`, `AuthController`, `JwtAuthenticationFilter`, `JwtServiceSecurityTest`, `RuntimeSecurityEventLoggingTest` | Implemented with residual risk |
| V4 Access Control | Role-based access control | Centralized ADMIN/ANALYST/AUDITOR route rules and authorization matrix tests | `SecurityConfig`, `RbacAuthorizationMatrixTest` | Implemented |
| V4 Access Control | Object-level authorization | Analysts can access only assigned/eligible cases | `ReportService`, `CaseReviewService`, `AnalystCaseOwnershipTest` | Implemented |
| V2 Business Logic | Explicit workflow transitions | Reports use a defined state transition policy: `SUBMITTED -> UNDER_REVIEW/REJECTED`, `UNDER_REVIEW -> MORE_INFO_REQUIRED/RESOLVED/REJECTED`, `MORE_INFO_REQUIRED -> UNDER_REVIEW/RESOLVED/REJECTED`; `RESOLVED` and `REJECTED` are terminal | `ReportWorkflowPolicy`, `ReportService`, `BusinessLogicWorkflowSecurityTest` | Implemented |
| V2 Business Logic | Workflow abuse prevention | Invalid jumps, unauthorised roles and non-owner analyst state changes are rejected and tested | `ReportService`, `CaseReviewService`, `BusinessLogicWorkflowSecurityTest`, `AnalystCaseOwnershipTest`, `RbacAuthorizationMatrixTest` | Implemented |
| V2 Business Logic | Transactions and concurrency | Critical report/case changes are transactional; `Report` and `CaseReview` use optimistic locking; stale writes map to `409 Conflict` | `Report`, `CaseReview`, `GlobalExceptionHandler`, `BusinessLogicWorkflowSecurityTest` | Implemented |
| V5 Validation | Request validation | Bean Validation on request DTOs | `CreateReportRequest`, `CreateUserRequest`, `Update*Request`, controllers | Implemented |
| V5 Validation | Domain invariants | Tracking code, safe filename and report description primitives | `TrackingCode`, `SafeFilename`, `ReportDescription`, domain tests | Implemented |
| V5 Validation | Upload validation | Size, extension, MIME, magic-byte and safe-path validation | `FileStorageService`, `ReportControllerAttachmentUploadTest`, `SafeFilenameSecurityTest` | Implemented |
| V7 Error Handling | Controlled errors | Generic errors, no stack traces and correlation IDs | `GlobalExceptionHandler`, `SecurityConfig`, `ErrorHandlingSecurityTest` | Implemented |
| V8 Data Protection | Secret management | Env-var configuration, fail-fast JWT secret validation and Gitleaks scan | `application.yaml`, `.env.example`, `.gitleaks.toml`, `SecurityConfigurationValidatorTest` | Implemented |
| V8 Data Protection | Evidence integrity | SHA-256 hashes for attachments, evidence packages and backup files plus HMAC-SHA256 signed backup manifests | `FileStorageService`, `CasePackageService`, `BackupService`, `BackupServiceIntegrationTest` | Implemented |
| V11 Cryptography | Key separation and validation | JWT and backup manifest HMAC use separate secrets; weak/missing/reused secrets fail startup validation | `SecurityConfigurationValidator`, `application.yaml`, `.env.example`, `SecurityConfigurationValidatorTest` | Implemented |
| V11 Cryptography | Backup authenticity and tamper detection | Backup ZIPs contain `manifest.json` plus `manifest.hmac.json`; verify/restore rejects modified manifests, changed file content and unsigned extra ZIP entries | `BackupService`, `BackupServiceIntegrationTest` | Implemented |
| V11 Cryptography | Key lifecycle | Secure installation guide documents key purpose, storage, manual rotation and `BACKUP_HMAC_KEY_ID`; automated rotation is out of current scope | `docs/SECURE_INSTALLATION.md` | Partially implemented |
| V9 Communication Security | Browser-facing secure headers | HSTS, CSP with `form-action 'self'`, frame protection, referrer policy and permissions policy | `SecurityConfig`, `SecurityHeadersTest` | Implemented baseline |
| V10 Malicious Code Prevention | Static/security analysis | SpotBugs, SonarCloud, CodeQL Code Scanning and archiveable SAST summary artifacts | `.github/workflows/dev.yml`, `sast / SonarCloud SAST Scan`, `sast-reports` | Evidence review |
| V10 Malicious Code Prevention | Dependency risk | Dependency-Check and CycloneDX SBOM evidence | `.github/workflows/dev.yml`, `dependency-scanning / Dependency Vulnerability Scanning`, `dependency-check-sca-reports`, `sbom-cyclonedx` | Evidence review |
| V10 Malicious Code Prevention | Runtime security evidence | Runtime security/IAST readiness evidence and OWASP ZAP baseline | `.github/workflows/dev.yml`, `dast-scan / dast-scan`, `iast-runtime-security-evidence`, `dast-zap-baseline-reports` | Evidence review |
| V11 Business Logic | Controlled case state changes | Closed-case modification protection, assignment conflict checks and explicit report workflow transitions | `CaseReviewService`, `ReportWorkflowPolicy`, `ClosedCaseSecurityTest`, `AnalystCaseOwnershipTest`, `BusinessLogicWorkflowSecurityTest` | Implemented |
| V13 API Security | DTO API contracts | Controllers return DTOs/records, including audit and security alert responses | `dto` package, controllers | Implemented |
| V13 API Security | Abuse controls | Rate limits for login, tracking, upload and download flows | `RateLimiterService`, security tests | Implemented |

## Scope Boundaries

The Level 2 evidence is scoped to implemented application features and automated
pipeline evidence. CodeQL findings are primarily stored in GitHub Code Scanning,
with an archiveable run summary artifact. Runtime security evidence is always
generated through tests; complete external IAST telemetry requires optional
Contrast agent configuration. Advanced production operations such as external
SIEM/WORM storage, distributed token revocation, privileged-user MFA and
authenticated DAST contexts are treated as operational hardening beyond the
current sprint scope.
