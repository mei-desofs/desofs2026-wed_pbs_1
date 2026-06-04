# ASVS Evidence Mapping

This document maps GhostReport security controls to implementation evidence.
The ASVS spreadsheet remains the formal checklist; this file provides repository
references that can be inspected during assessment.

## Evidence Summary

| Area | Evidence |
| --- | --- |
| Authentication | `AuthController`, `AuthService`, `JwtService`, `JwtAuthenticationFilter`, BCrypt password encoding, inactive-user checks and login rate limiting tests. |
| Authorization | Centralized role rules in `SecurityConfig`, RBAC tests and analyst ownership tests. |
| Input validation | Request DTO validation, `TrackingCode`, `SafeFilename`, `ReportDescription` and upload validation tests. |
| API contracts | DTO/record responses in the `dto` package, including audit/security evidence responses. |
| File handling | MIME, extension, magic-byte, size and normalized-path checks in `FileStorageService`. |
| Error handling | `GlobalExceptionHandler`, Spring Security error responses and tests for controlled responses. |
| Data protection | Environment-based secrets, JWT secret validation, attachment/package/backup hashes and Gitleaks scanning. |
| Logging and monitoring | `AuditLogService`, `SecurityMonitoringService` and runtime security event tests. |
| DevSecOps evidence | CI, JaCoCo, SpotBugs, CodeQL, Dependency-Check, CycloneDX, Gitleaks, ZAP, IAST/runtime evidence and PIT workflows. |

## Control Evidence

| ASVS topic | Project evidence | Status |
| --- | --- | --- |
| Password storage | BCrypt `PasswordEncoder` and user creation tests. | Implemented |
| Credential handling | Passwords accepted only in request DTOs and never returned in user responses. | Implemented |
| Session management | Stateless JWT, expiry validation, signature validation and role validation tests. | Implemented |
| Access control | `ADMIN`, `ANALYST` and `AUDITOR` rules in `SecurityConfig`. | Implemented |
| Object-level authorization | Analyst ownership controls in services and tests. | Implemented |
| Input validation | Bean Validation annotations and domain primitives. | Implemented |
| Upload validation | File type, size, magic-byte and safe-path checks. | Implemented |
| Error handling | Generic API errors, no stack traces and correlation IDs. | Implemented |
| Security logging | Sanitized audit logs and security alerts. | Implemented |
| Backup integrity | Backup manifests and SHA-256 verification. | Implemented |
| Secret scanning | Gitleaks workflow and `.gitleaks.toml` placeholder allowlist. | Implemented |
| Static analysis | SpotBugs and CodeQL workflows. | Evidence review |
| Dependency analysis | OWASP Dependency-Check and CycloneDX SBOM workflows. | Evidence review |
| Dynamic analysis | OWASP ZAP baseline workflow. | Evidence review |
| IAST/runtime evidence | Runtime security test workflow and optional Contrast Java Agent readiness. | Evidence review |

## Pipeline Artifact Mapping

| Evidence area | Artifact |
| --- | --- |
| Tests | `ci-surefire-test-reports` |
| Coverage | `ci-jacoco-coverage-report` |
| SAST | `sast-spotbugs-report`, CodeQL Code Scanning |
| SCA | `dependency-check-sca-*`, `sbom-cyclonedx` |
| Secret scanning | `secret-scan-gitleaks-json` |
| DAST | `dast-zap-baseline-*`, `dast-ghostreport-app-log` |
| IAST/runtime | `iast-runtime-security-evidence` |
| Mutation testing | `pit-mutation-testing-report` |

## Scope Boundaries

The current scope focuses on coursework-grade secure development evidence for
the implemented GhostReport features. Additional production hardening, such as
external SIEM integration, token revocation, advanced deployment TLS management
and authenticated DAST contexts, is documented as next-step operational work.
