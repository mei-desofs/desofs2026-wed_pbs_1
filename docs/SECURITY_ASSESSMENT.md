# Security Assessment

This assessment summarizes the current security posture of GhostReport for Phase 2 Sprint 2. It maps implemented controls to evidence and identifies residual risks without claiming controls that are not implemented.

## Assessment Scope

| Area | Scope |
|---|---|
| Authentication | JWT login flow, password hashing, inactive users, login rate limiting. |
| Authorization | RBAC, analyst ownership, auditor/admin separation. |
| Input validation | DTO validation and domain primitives. |
| File handling | Upload validation, path traversal controls, attachment access, evidence packages. |
| Audit and monitoring | Audit logs and security alerts for relevant events. |
| Configuration | Profiles, environment variables, JWT secret validation, seed users, upload/backup paths. |
| DevSecOps | CI tests, JaCoCo, SpotBugs, Dependency-Check, Gitleaks and ZAP baseline. |

## Evidence Matrix

| Control | Evidence | Result | Residual risk | Status |
|---|---|---|---|---|
| Password hashing | `SecurityConfig.passwordEncoder()`, user creation tests. | Passwords are stored with BCrypt. | Existing manually inserted DB rows can still contain invalid hashes and should be recreated. | Implemented |
| JWT signing and validation | `JwtService`, `JwtServiceSecurityTest`. | Signature, expiry and role mismatch are tested. | No token revocation list. | Implemented |
| JWT secret configuration | `SecurityConfigurationValidator`, `SecurityConfigurationValidatorTest`, `.env.example`. | Weak/unsafe production-like secrets fail fast. | Secret rotation process is operational, not implemented in app. | Implemented |
| Login abuse protection | `RateLimiterService`, `LoginRateLimitSecurityTest`. | Repeated failures trigger 429 and alerts. | In-memory limiter is single-instance only. | Implemented baseline |
| Runtime auth monitoring | `RuntimeSecurityEventLoggingTest`, `AuditLogService`, `SecurityMonitoringService`. | Login success/failure and invalid JWT events are recorded without secrets. | Not a full SIEM integration. | Implemented baseline |
| RBAC | `SecurityConfig`, `RbacAuthorizationMatrixTest`. | ADMIN/ANALYST/AUDITOR access is verified. | Future endpoints require matrix updates. | Implemented |
| Analyst ownership | `AnalystCaseOwnershipTest`, service ownership checks. | Analysts cannot access cases owned by another analyst. | Admin oversight remains intentionally broad. | Implemented |
| Public report confidentiality | `TrackingCode`, `TrackingCodeEnumerationTest`, `ReportControllerAttachmentUploadTest`. | Tracking codes and public attachment listing require valid codes. | No CAPTCHA or distributed anti-automation. | Implemented baseline |
| Upload validation | `FileStorageService`, `ReportControllerAttachmentUploadTest`, `FileStorageServiceTest`. | Size, MIME, extension, magic bytes and safe paths are tested. | No real malware scanning. | Implemented baseline |
| Path traversal protection | `SafeFilename`, storage boundary checks, backup path checks. | Malicious names and paths are rejected. | Must remain covered for new file features. | Implemented |
| Error handling | `GlobalExceptionHandler`, `ErrorHandlingSecurityTest`. | Stack traces/internal details are not exposed in tested flows. | Error format could be expanded with correlation IDs in future. | Implemented baseline |
| Audit logs | `AuditLogService`, audit/security tests. | Critical operations are logged with sanitized details. | Logs are not append-only or hash-chained. | Implemented baseline |
| Security alerts | `SecurityMonitoringService`, alert repository tests. | Suspicious tracking/upload/backup/ownership/auth events create alerts. | No external alerting/SIEM. | Implemented baseline |
| Backup integrity | `BackupService`, `BackupServiceIntegrationTest`, admin/auditor backup endpoints. | Manifests and SHA-256 validation are implemented. | Restore is staged/validated; full operational DR runbook is future work. | Implemented baseline |
| Evidence packages | `CasePackageService`, `CasePackageServiceIntegrationTest`. | Closed-case package generation and verification are tested. | Chain-of-custody is basic, not legally certified. | Implemented baseline |
| SAST | `sast-spotbugs.yml`, SpotBugs artifacts. | Static analysis runs as evidence. | Findings require manual triage. | Evidence mode |
| SCA | `sca-dependency-check.yml`, Dependency-Check artifacts. | CVE reports are generated. | Dependency-Check false positives/negatives require triage. | Evidence mode |
| Secret scanning | `secret-scan-gitleaks.yml`, Gitleaks artifact. | Repository secret scan runs. | Does not replace key rotation or GitHub secret management. | Evidence mode |
| DAST | `dast-zap.yml`, ZAP baseline artifacts. | Passive baseline scan runs against a live app. | Scan is unauthenticated and not a full active attack scan. | Evidence mode |
| Test coverage | JaCoCo in Maven and CI artifacts. | Coverage reports are generated. | Coverage thresholds may be tuned over time. | Implemented baseline |

## Build Blocking vs Evidence Mode

| Check | Current behavior | Rationale |
|---|---|---|
| Maven compile/test | Build blocking | Functional and security regression tests must pass. |
| Security configuration validator tests | Build blocking | Unsafe config behavior is directly testable. |
| JaCoCo report generation | Build blocking as part of tests | Coverage report must be produced; strict thresholds can be increased gradually. |
| SpotBugs | Evidence mode | Findings require triage to avoid blocking on false positives during coursework hardening. |
| Dependency-Check | Evidence mode | CVE matching can include false positives and external feed instability. |
| Gitleaks | Build blocking if leaks are found | Hardcoded secrets should not enter the repository. |
| ZAP Baseline | Evidence mode | Passive unauthenticated findings need manual review before becoming gates. |

## Findings and Residual Risks

| Finding / limitation | Current mitigation | Future work |
|---|---|---|
| No malware scanning | File validation, MIME/magic byte checks and blocked executable extensions. | Integrate a real scanner such as ClamAV before claiming antivirus protection. |
| In-memory rate limiting | Limits exist for login, tracking, upload and download. | Move to Redis or another shared store for multi-instance deployment. |
| Audit logs not tamper-proof | Sanitized audit logs and security alerts are stored in DB. | Add append-only storage, hash chaining or external SIEM/WORM storage. |
| No MFA | Strong passwords, JWT and RBAC. | Add MFA for ADMIN/AUDITOR. |
| DAST unauthenticated | Public surface is scanned. | Add authenticated ZAP context for role-specific flows. |
| No full IAST agent | Runtime security events and DAST provide runtime evidence. | Integrate a dedicated IAST tool if license/setup constraints allow. |

## Assessment Conclusion

GhostReport has a strong coursework-level security baseline: authentication, RBAC, ownership, upload security, audit trails, security alerts, backup integrity and DevSecOps evidence are implemented and tested. The remaining risks are mainly production maturity items: distributed rate limiting, real malware scanning, tamper-proof logs, MFA, authenticated DAST and a dedicated IAST agent.
