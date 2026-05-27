# ASVS Evidence Mapping

This document supports the ASVS tracker for Phase 2 Sprint 2. It maps controls
to implemented code, tests and pipeline evidence. The spreadsheet remains the
formal tracker, but this file explains how each claim can be defended.

## Coverage Summary

| Area | Status | Evidence |
| --- | --- | --- |
| Authentication | Partially strong | `AuthController`, `AuthService`, `JwtService`, `JwtAuthenticationFilter`, BCrypt, JWT tests to add. |
| Authorization / RBAC | Strong | `SecurityConfig`, `AdminAuthorizationTest`, `AuditorAuthorizationTest`, `RbacAuthorizationMatrixTest`, `AnalystCaseOwnershipTest`. |
| Input validation | Strong | DTO validation, `ReportDescription`, `SafeFilename`, `TrackingCode`, controller/service tests. |
| File upload security | Strong baseline | `FileStorageService`, MIME/extension/magic byte checks, path normalization, upload tests. |
| Error handling | Partial | Existing safe responses should be documented and tested more explicitly. |
| Logging and monitoring | Partial/strong | `AuditLogService`, `SecurityMonitoringService`, audit/security tests. Logs are not tamper-proof. |
| Rate limiting | Partial | `RateLimiterService`, public tracking/upload/download limits. Login rate limiting not implemented yet. |
| Backup integrity | Strong baseline | `BackupService`, manifests, SHA-256, restore staging, integration tests. |
| DevSecOps | Strong evidence | CI, SpotBugs, Dependency-Check, Gitleaks, ZAP and JaCoCo artifacts. |

## Suggested ASVS Control Evidence

| ASVS topic | GhostReport evidence | Status |
| --- | --- | --- |
| Password storage | BCrypt `PasswordEncoder`; password complexity in `CreateUserRequest`. | Implemented |
| Session management | Stateless JWT; no server-side session state for API authentication. | Implemented |
| Access control | Centralized RBAC in `SecurityConfig`; ownership checks in services. | Implemented |
| Generic error handling | Generic responses for unauthorized/forbidden flows; add explicit tests for stack trace absence. | Partial |
| Input validation | Bean Validation DTOs and domain primitives. | Implemented |
| File upload validation | Size, MIME, extension, magic bytes, safe names and controlled storage. | Implemented |
| Path traversal prevention | `SafeFilename`, normalized paths and storage boundary checks. | Implemented |
| Logging | Audit logs for critical operations; sanitization of log details. | Implemented |
| Monitoring | Security alerts for suspicious tracking, upload, path traversal, backup and ownership events. | Partial |
| Data integrity | SHA-256 hashes for evidence and backups. | Implemented |
| Secret management | GitHub Actions secrets and Gitleaks workflow. | Implemented |
| Dependency monitoring | OWASP Dependency-Check workflow and artifacts. | Implemented |
| Static analysis | SpotBugs workflow and XML artifacts. | Implemented |
| Dynamic analysis | OWASP ZAP baseline workflow and reports. | Implemented |

## Known Gaps to Track Honestly

| Gap | Recommended wording |
| --- | --- |
| Malware scanning | Planned future hardening; current upload security is validation-based, not antivirus scanning. |
| Storage quotas | Not implemented; future work for abuse and capacity control. |
| Tamper-proof audit logs | Audit logs exist but are not append-only/hash-chained. |
| Distributed rate limiting | Current implementation is in-memory and suitable for single-instance/dev use. |
| Full admin lifecycle | Admin currently creates/lists users; edit/deactivate/delete/role changes are not complete unless Sprint 2 implements them. |
| Authenticated DAST | ZAP baseline is unauthenticated and passive. |
| MFA | Not implemented. |

## Sprint 2 High-Value ASVS Improvements

| Improvement | Why it matters | Evidence to add |
| --- | --- | --- |
| Login rate limiting | Strengthens authentication abuse protection. | Unit/integration test and security alert. |
| Inactive users | Makes admin user management more credible. | Admin endpoint tests and login rejection test. |
| JWT tamper/expired tests | Proves token validation behavior. | Security integration tests. |
| Error response tests | Supports information disclosure controls. | Tests for no stack traces/internal messages. |
| ASVS screenshots/artifacts | Makes tracker defensible. | Link workflow artifacts and test output. |

## Evidence Storage

Recommended evidence location:

```text
Deliverables/Phase 2/Evidence/
```

Store only curated evidence there: screenshots, downloaded workflow artifacts,
coverage summaries and ASVS notes. Do not store local build folders or runtime
backups.
