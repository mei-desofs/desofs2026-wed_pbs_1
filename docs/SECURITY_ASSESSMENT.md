# Security Assessment

This assessment summarizes GhostReport's implemented security controls and the
evidence used to verify them.

## Assessment Scope

| Area | Scope |
| --- | --- |
| Authentication | JWT login flow, BCrypt password hashing, inactive-user checks and login rate limiting. |
| Authorization | RBAC for ADMIN, ANALYST and AUDITOR, plus analyst ownership controls. |
| Input validation | DTO validation, domain primitives and upload validation. |
| File handling | Safe upload storage, attachment access, evidence packages and backup verification. |
| Audit and monitoring | Audit logs and security alerts for security-relevant events. |
| Configuration | Runtime profiles, environment variables, JWT secret validation and seed-user controls. |
| DevSecOps | Build, tests, coverage, SAST, SCA, SBOM, secret scanning, DAST, IAST/runtime evidence and mutation testing. |

## Evidence Matrix

| Control | Evidence | Result | Status |
| --- | --- | --- | --- |
| Password hashing | `SecurityConfig.passwordEncoder()`, user creation tests | Passwords are stored with BCrypt. | Implemented |
| JWT signing and validation | `JwtService`, `JwtServiceSecurityTest` | Signature, expiry and role validation are tested. | Implemented |
| JWT secret validation | `SecurityConfigurationValidator`, `.env.example`, validator tests | Unsafe production-like JWT configuration fails fast. | Implemented |
| Login abuse protection | `RateLimiterService`, `LoginRateLimitSecurityTest` | Repeated failures trigger rate limiting and alerts. | Implemented |
| Runtime auth monitoring | `RuntimeSecurityEventLoggingTest`, `AuditLogService`, `SecurityMonitoringService` | Auth events are recorded without passwords or tokens. | Implemented |
| RBAC | `SecurityConfig`, `RbacAuthorizationMatrixTest` | Role-specific access is verified. | Implemented |
| Analyst ownership | `AnalystCaseOwnershipTest`, service ownership checks | Analysts are restricted to owned/eligible cases. | Implemented |
| Public report confidentiality | `TrackingCode`, tracking code tests | Public tracking and attachment listing require valid tracking codes. | Implemented |
| Upload validation | `FileStorageService`, upload tests | Size, MIME, extension, magic bytes and safe paths are verified. | Implemented |
| Path traversal protection | `SafeFilename`, storage boundary checks | Malicious names and paths are rejected. | Implemented |
| Error handling | `GlobalExceptionHandler`, `ErrorHandlingSecurityTest` | Responses avoid stack traces and include correlation IDs. | Implemented |
| Security headers | `SecurityConfig`, `SecurityHeadersTest` | Browser-facing headers are configured and tested. | Implemented |
| Audit logs | `AuditLogService`, audit tests | Critical state changes are logged with sanitized details. | Implemented |
| Security alerts | `SecurityMonitoringService`, alert tests | Suspicious activity creates security alerts. | Implemented |
| Backup integrity | `BackupService`, backup tests | Manifests and SHA-256 validation are implemented. | Implemented |
| Evidence packages | `CasePackageService`, package tests | Closed-case packages can be generated and verified. | Implemented |
| SAST | SpotBugs and CodeQL workflows | Static analysis evidence is generated. | Evidence review |
| SCA/SBOM | Dependency-Check and CycloneDX workflows | Dependency risk and inventory evidence is generated. | Evidence review |
| Secret scanning | Gitleaks workflow | Repository secret scan evidence is generated. | Implemented |
| DAST | ZAP baseline workflow | Runtime HTTP baseline evidence is generated. | Evidence review |
| IAST/runtime evidence | IAST runtime workflow | Runtime security tests and optional Java agent readiness are documented. | Evidence review |
| Coverage and mutation testing | JaCoCo and PIT workflows | Coverage and test strength evidence is generated. | Evidence review |

## Gate Policy

| Check | Current behavior |
| --- | --- |
| Maven compile/test | Blocking |
| Security configuration validator tests | Blocking |
| JaCoCo report and baseline thresholds | Blocking in CI |
| Gitleaks | Blocking for confirmed leaks |
| SpotBugs, CodeQL, Dependency-Check, SBOM, ZAP, IAST/runtime and PIT | Evidence review |

## Scope Boundaries

The assessment covers the implemented coursework application and its automated
security evidence. Additional production operations such as external SIEM,
privileged-user MFA, centralized rate limiting, token revocation and advanced
deployment TLS management are considered operational hardening.
