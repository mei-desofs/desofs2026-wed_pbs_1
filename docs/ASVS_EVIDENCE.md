# ASVS Evidence Mapping

This document supports the ASVS tracker for Phase 2 Sprint 2. It maps controls
to implemented code, tests and pipeline evidence. The spreadsheet remains the
formal tracker, but this file explains how each claim can be defended.

## Coverage Summary

| Area | Status | Evidence |
| --- | --- | --- |
| Authentication | Strong baseline | `AuthController`, `AuthService`, `JwtService`, `JwtAuthenticationFilter`, BCrypt, inactive-user checks, login rate limiting, runtime auth events and JWT security tests. |
| Authorization / RBAC | Strong | `SecurityConfig`, `AdminAuthorizationTest`, `AuditorAuthorizationTest`, `RbacAuthorizationMatrixTest`, `AnalystCaseOwnershipTest`. |
| Input validation | Strong | DTO validation, `ReportDescription`, `SafeFilename`, `TrackingCode`, controller/service tests. |
| File upload security | Strong baseline | `FileStorageService`, MIME/extension/magic byte checks, path normalization, upload tests. |
| Error handling | Strong baseline | Controlled JSON errors and tests for malformed/unauthorized responses without stack traces. |
| Logging and monitoring | Partial/strong | `AuditLogService`, `SecurityMonitoringService`, audit/security tests, login events, invalid JWT alerts and brute-force alerts. Logs are not tamper-proof. |
| Rate limiting | Strong baseline | `RateLimiterService`, public tracking/upload/download limits and login rate limiting. |
| Backup integrity | Strong baseline | `BackupService`, manifests, SHA-256, restore staging, integration tests. |
| DevSecOps | Strong evidence | CI, SpotBugs, Dependency-Check, Gitleaks, ZAP and JaCoCo artifacts mapped in `docs/PIPELINE_ARTIFACTS.md`. |

## Suggested ASVS Control Evidence

| ASVS topic | GhostReport evidence | Status |
| --- | --- | --- |
| Password storage | BCrypt `PasswordEncoder`; password complexity in `CreateUserRequest`. | Implemented |
| Session management | Stateless JWT; no server-side session state for API authentication; signature, expiry and role validation tested. | Implemented |
| Access control | Centralized RBAC in `SecurityConfig`; ownership checks in services; admin activate/deactivate tests. | Implemented |
| Generic error handling | Generic responses for unauthorized/forbidden/malformed flows; tests check absence of internals. | Implemented |
| Input validation | Bean Validation DTOs and domain primitives. | Implemented |
| File upload validation | Size, MIME, extension, magic bytes, safe names and controlled storage. | Implemented |
| Path traversal prevention | `SafeFilename`, normalized paths and storage boundary checks. | Implemented |
| Logging | Audit logs for critical operations; sanitization of log details. | Implemented |
| Monitoring | Security alerts for suspicious tracking, upload, path traversal, backup, ownership and brute-force login events. | Implemented baseline |
| Data integrity | SHA-256 hashes for evidence and backups. | Implemented |
| Secret management | GitHub Actions secrets and Gitleaks workflow. | Implemented |
| Secure configuration | `.env.example`, `docs/SECURE_INSTALLATION.md`, `SecurityConfigurationValidator`, fail-fast configuration tests. | Implemented |
| Dependency monitoring | OWASP Dependency-Check workflow and artifacts. | Implemented |
| Static analysis | SpotBugs workflow and XML artifacts. | Implemented |
| Dynamic analysis | OWASP ZAP baseline workflow and reports. | Implemented |

## Pipeline Evidence by ASVS Area

| ASVS evidence area | Artifact evidence |
| --- | --- |
| Secure verification | `ci-surefire-test-reports`, `ci-jacoco-coverage-report` |
| Static analysis | `sast-spotbugs-report` |
| Dependency management | `dependency-check-sca-html`, `dependency-check-sca-json`, `dependency-check-sca-xml`, `dependency-check-sca-sarif` |
| Secret management | `secret-scan-gitleaks-json` |
| Dynamic analysis | `dast-zap-baseline-html`, `dast-zap-baseline-json`, `dast-zap-baseline-xml` |
| Runtime evidence | `dast-ghostreport-app-log` |

The full artifact map is maintained in `docs/PIPELINE_ARTIFACTS.md`.

## Known Gaps to Track Honestly

| Gap | Recommended wording |
| --- | --- |
| Malware scanning | Planned future hardening; current upload security is validation-based, not antivirus scanning. |
| Storage quotas | Not implemented; future work for abuse and capacity control. |
| Tamper-proof audit logs | Audit logs exist but are not append-only/hash-chained. |
| Distributed rate limiting | Current implementation is in-memory and suitable for single-instance/dev use. |
| Full admin lifecycle | Admin currently creates/lists/activates/deactivates users; edit/delete/role changes/password resets are not implemented. |
| Authenticated DAST | ZAP baseline is unauthenticated and passive. |
| MFA | Not implemented. |
| Full IAST agent | Not implemented; runtime security instrumentation is documented separately. |

## Sprint 2 High-Value ASVS Improvements

| Improvement | Why it matters | Evidence to add |
| --- | --- | --- |
| Login rate limiting | Strengthens authentication abuse protection. | `LoginRateLimitSecurityTest` and `BRUTE_FORCE_LOGIN_ATTEMPT` alert. |
| Inactive users | Makes admin user management more credible. | `AdminUserManagementSecurityTest` verifies deactivate/reactivate and login rejection. |
| JWT tamper/expired tests | Proves token validation behavior. | `JwtServiceSecurityTest`. |
| Error response tests | Supports information disclosure controls. | `ErrorHandlingSecurityTest`. |
| Runtime security events | Supports monitoring and assessment evidence. | `RuntimeSecurityEventLoggingTest`, `LOGIN_SUCCESS`, `LOGIN_FAILED`, `INVALID_JWT_TOKEN`. |
| ASVS screenshots/artifacts | Makes tracker defensible. | Link workflow artifacts and test output. |

## Evidence Storage

Recommended evidence location:

```text
Deliverables/Phase 2/Evidence/
```

Store only curated evidence there: screenshots, downloaded workflow artifacts,
coverage summaries and ASVS notes. Do not store local build folders or runtime
backups.
