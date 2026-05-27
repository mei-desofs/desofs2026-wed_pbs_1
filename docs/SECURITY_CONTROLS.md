# Security Controls Mapping

This document maps implemented GhostReport controls to code, tests, ASVS
evidence and known limitations. It is intended to support the final report and
avoid overclaiming.

## Control Matrix

| Control | Implementation | Tests / Evidence | Status | Limitation |
| --- | --- | --- | --- | --- |
| JWT authentication | `AuthController`, `AuthService`, `JwtService`, `JwtAuthenticationFilter` | `JwtServiceSecurityTest`, login tests, CI artifacts | Implemented | No refresh-token flow. |
| Password hashing | `BCryptPasswordEncoder` in `SecurityConfig` | User creation/login tests | Implemented | No password reset flow. |
| Inactive users | `User.active`, `CustomUserDetailsService`, `AuthService`, admin activate/deactivate endpoints | `AdminUserManagementSecurityTest` | Implemented | No scheduled account review workflow. |
| Login rate limiting | `RateLimiterService`, `AuthController`, `RateLimitProperties` | `LoginRateLimitSecurityTest` | Implemented baseline | In-memory, not distributed. |
| Brute-force alerting | `SecurityMonitoringService.recordBruteForceLoginAttempt` | `LoginRateLimitSecurityTest` | Implemented baseline | No SIEM integration. |
| RBAC | `SecurityConfig` route rules | `RbacAuthorizationMatrixTest`, `AdminAuthorizationTest`, `AuditorAuthorizationTest` | Implemented | No fine-grained permission model beyond roles/ownership. |
| Analyst ownership | `ReportService`, `CaseReviewService`, `CasePackageService` | `AnalystCaseOwnershipTest`, `CasePackageServiceIntegrationTest` | Implemented | Admin has oversight access. |
| Input validation | DTO Bean Validation and domain primitives | domain tests, controller tests | Implemented | Validation is application-level, not WAF-based. |
| Upload validation | `FileStorageService`, `SafeFilename` | `FileStorageServiceTest`, `ReportControllerAttachmentUploadTest` | Implemented baseline | No antivirus/malware engine. |
| Path traversal prevention | Normalized path checks and safe filename validation | upload, backup and filename tests | Implemented | Depends on controlled storage paths. |
| Error handling | `GlobalExceptionHandler`, Spring Security handlers | `ErrorHandlingSecurityTest`, public flow tests | Implemented baseline | No correlation ID yet. |
| Audit logs | `AuditLogService` | audit/security tests | Implemented baseline | Not append-only or hash-chained. |
| Security alerts | `SecurityMonitoringService` | tracking/upload/ownership/login tests | Implemented baseline | No external monitoring sink. |
| Backup integrity | `BackupService`, manifests, SHA-256 hashes | `BackupServiceIntegrationTest`, admin backup tests | Implemented | No encrypted/signed backup storage. |
| Evidence packages | `CasePackageService` | `CasePackageServiceIntegrationTest`, auditor tests | Implemented | No long-term chain-of-custody signing. |
| DevSecOps evidence | GitHub Actions workflows | CI, SAST, SCA, Gitleaks, ZAP artifacts | Implemented evidence | SAST/SCA/DAST are manual triage, not strict gates. |

## Recommended Report Wording

Use:

> GhostReport implements a baseline set of application security controls,
> including JWT authentication, BCrypt password hashing, RBAC, analyst
> ownership checks, upload validation, path traversal protection, audit logs,
> security alerts, backup integrity verification and DevSecOps evidence.

Avoid:

> GhostReport fully prevents all attacks against authentication, uploads,
> dependencies and audit logs.

## Future Work

The following controls should remain future work unless implemented and tested:

- MFA for privileged roles;
- malware scanning with a real scanning engine;
- distributed rate limiting with Redis or equivalent;
- tamper-proof audit logs with append-only storage or hash chaining;
- signed/encrypted backup storage;
- authenticated DAST;
- full user lifecycle management with role changes and password resets.
