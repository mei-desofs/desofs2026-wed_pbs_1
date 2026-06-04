# ASVS 5.0 Evidence Mapping

This document supports the formal spreadsheet
`Deliverables/Phase 2/ASVS_5.0_Tracker_Phase 2_Sprint 2.xlsx`.
The spreadsheet is the checklist of record; this file explains the evidence,
known gaps and residual risk used to fill it.

## Assessment Rules

- `Compliant` is used only where implementation and tests or pipeline evidence
  exist.
- `Partially Compliant` is used where a control exists but is incomplete,
  lacks full operational maturity, or has open findings.
- `Not Applicable` is used only where the feature is outside GhostReport scope.
- `Not Started` is used where no defensible implementation evidence exists.

## Chapter Summary

| ASVS chapter | Current status | Evidence | Tests/reports | Gaps and residual risk |
| --- | --- | --- | --- | --- |
| V1 Encoding and Sanitization | Partially Compliant | `SafeFilename`, `ReportDescription`, DTO validation, Spring Data JPA repositories | Domain tests, security tests, JaCoCo | Frontend still needs inline JavaScript/CSS removal and stronger output-encoding evidence. |
| V2 Validation and Business Logic | Partially Compliant | DTO validation, domain primitives, service-level state checks, rate limits | `TrackingCodeEnumerationTest`, `RateLimiterServiceTest`, RBAC tests | Business-limit documentation is incomplete. Some workflows need more negative-path tests. |
| V3 Web Frontend Security | Partially Compliant | Security headers, CSP without `unsafe-inline`, external JavaScript files and removed inline handlers/styles | `SecurityHeadersTest`, ZAP baseline | Existing ZAP evidence is pre-remediation; run ZAP again to prove the CSP finding is closed. |
| V4 API and Web Service | Partially Compliant | Controllers return DTOs, generic JSON errors and role-protected endpoints | MockMvc security tests, Surefire reports | HTTP method restrictions, proxy-boundary assumptions and cache behaviour need more explicit tests/docs. |
| V5 File Handling | Partially Compliant | File size, MIME, extension, magic-byte checks and normalized path boundaries | `FileStorageServiceTest`, upload/security tests | No antivirus scanning, quarantine, per-user storage quotas or archive unpacking policy. |
| V6 Authentication | Partially Compliant | BCrypt, inactive-user checks, login rate limiting and audit/security events | `LoginRateLimitSecurityTest`, `AdminUserManagementSecurityTest` | No MFA, password reset, password history/reuse checks or mature account recovery. |
| V7 Session Management | Partially Compliant | Stateless JWT validation, expiry and role checks | `JwtServiceSecurityTest`, runtime security evidence | No server-side token revocation/logout denylist, refresh-token rotation or concurrent-session policy. |
| V8 Authorization | Partially Compliant | `ADMIN`, `ANALYST`, `AUDITOR` route rules and ownership checks | `RbacAuthorizationMatrixTest`, `AnalystCaseOwnershipTest`, `AuditorAuthorizationTest` | Field-level authorization documentation and service-level coverage are not complete. |
| V9 Self-contained Tokens | Partially Compliant | JWT HS256 signature verification, algorithm allowlist and `exp` validation | `JwtServiceSecurityTest` | Missing `issuer`/`audience` claims, key rotation and token denylist. |
| V10 OAuth and OIDC | Not Applicable | No OAuth/OIDC, IdP, authorization-code flow, PKCE or token exchange exists | Architecture review | If an external IdP is added later, this chapter becomes applicable. |
| V11 Cryptography | Partially Compliant | BCrypt, HMAC-SHA256 JWTs and SHA-256 hashes for files/backups | JWT and backup tests | No formal cryptographic inventory, key lifecycle or rotation plan. |
| V12 Secure Communication | Partially Compliant | HSTS header and installation guidance for TLS deployment | `SecurityHeadersTest`, `docs/SECURE_INSTALLATION.md` | Local CI/DAST evidence does not prove production TLS, certificates or cipher configuration. |
| V13 Configuration | Partially Compliant | Environment-based secrets, prod-like fail-fast validation, disabled seed users, SCA triage | `SecurityConfigurationValidatorTest`, Gitleaks evidence, `docs/SCA_TRIAGE.md` | Dependency updates were applied, but a new Dependency-Check report is required before marking findings fixed. |
| V14 Data Protection | Partially Compliant | DTO responses avoid passwords, upload/package/backup hashes protect integrity | DTO tests, backup/package tests | Data classification, retention/deletion policy and encryption-at-rest are incomplete. |
| V15 Secure Coding and Architecture | Partially Compliant | CI, JaCoCo, SpotBugs, CodeQL, Dependency-Check, CycloneDX, Gitleaks, ZAP, PIT evidence | GitHub Actions artifacts under `Deliverables/Phase 2/Evidence`, `docs/SCA_TRIAGE.md`, `docs/SPOTBUGS_TRIAGE.md`, `docs/SECURITY_TESTING.md` | SpotBugs was reduced to 21 triaged findings; SCA needs a fresh post-upgrade scan; PIT is configured for real reports in CI Java 17 but local Java 23 still fails. |
| V16 Security Logging and Error Handling | Partially Compliant | `AuditLogService`, `SecurityMonitoringService`, generic error handlers and correlation IDs | `RuntimeSecurityEventLoggingTest`, `ErrorHandlingSecurityTest` | Logs are not tamper resistant and no SIEM/incident-response runbook is implemented. |
| V17 WebRTC | Not Applicable | GhostReport has no WebRTC, TURN/STUN, media server or browser media capture | Architecture review | None in current scope. |

## Evidence References

| Evidence type | Repository references |
| --- | --- |
| Authentication | `ghostreport/src/main/java/com/ghostreport/controller/AuthController.java`, `ghostreport/src/main/java/com/ghostreport/service/AuthService.java`, `ghostreport/src/main/java/com/ghostreport/service/JwtService.java` |
| Authorization | `ghostreport/src/main/java/com/ghostreport/security/SecurityConfig.java`, `ghostreport/src/test/java/com/ghostreport/security/RbacAuthorizationMatrixTest.java` |
| Input validation | `ghostreport/src/main/java/com/ghostreport/dto`, `ghostreport/src/main/java/com/ghostreport/domain`, `ghostreport/src/test/java/com/ghostreport/domain` |
| File handling | `ghostreport/src/main/java/com/ghostreport/service/FileStorageService.java`, `ghostreport/src/test/java/com/ghostreport/service/FileStorageServiceTest.java` |
| Backups and integrity | `ghostreport/src/main/java/com/ghostreport/service/BackupService.java`, `ghostreport/src/test/java/com/ghostreport/service/BackupServiceIntegrationTest.java` |
| Error handling | `ghostreport/src/main/java/com/ghostreport/exception/GlobalExceptionHandler.java`, `ghostreport/src/test/java/com/ghostreport/security/ErrorHandlingSecurityTest.java` |
| Runtime security events | `ghostreport/src/main/java/com/ghostreport/service/SecurityMonitoringService.java`, `ghostreport/src/test/java/com/ghostreport/security/RuntimeSecurityEventLoggingTest.java` |
| Pipeline evidence | `.github/workflows/*.yml`, `Deliverables/Phase 2/Evidence` |

## Tool Evidence Status

| Tool | Result | Evidence | Issues identified | Current status |
| --- | --- | --- | --- | --- |
| JUnit/MockMvc | 106 tests in current evidence, all passing | `Deliverables/Phase 2/Evidence/testing/ci-surefire-test-reports (1)` | Coverage gaps in admin/case/backup branches | Evidence accepted; add targeted tests. |
| JaCoCo | Coverage artifact exists | `Deliverables/Phase 2/Evidence/testing/ci-jacoco-coverage-report (1)` | Low coverage in some controllers/services | Evidence accepted; improve critical paths. |
| Dependency-Check | Report generated | `Deliverables/Phase 2/Evidence/sca/dependency-check-sca-json`, `docs/SCA_TRIAGE.md` | Old report found critical/high CVEs in Spring Boot/Tomcat/PostgreSQL and other dependencies | Dependency versions updated; fresh scan required before closing findings. |
| SpotBugs | Report generated | `Deliverables/Phase 2/Evidence/sast/sast-spotbugs-report (1)`, `Deliverables/Phase 2/Evidence/sast/spotbugs-post-remediation`, `docs/SPOTBUGS_TRIAGE.md` | Original report found 35 findings; post-remediation report has 21 findings | High-value findings remediated; remaining findings are triaged residual risk. |
| Gitleaks | Empty JSON report means no leaks found in scanned scope | `Deliverables/Phase 2/Evidence/secret-scanning` once organized | Artifact was previously categorized under SCA | Evidence valid after folder cleanup. |
| ZAP | Baseline report generated | `Deliverables/Phase 2/Evidence/dast` | Old report found CSP `unsafe-inline`, comments and cache informational alerts | CSP/frontend remediation applied; fresh ZAP run required as final evidence. |
| CodeQL | Code Scanning plus run summary | `Deliverables/Phase 2/Evidence/sast/sast-codeql-evidence-summary` | Local SARIF export is not promised by the current workflow | Evidence review. |
| PIT | Evidence review | `Deliverables/Phase 2/Evidence/testing/pit-mutation-testing-report`, `Deliverables/Phase 2/Evidence/testing/pit-local-java23-runtime-note.md`, `docs/SECURITY_TESTING.md` | Previous run had fallback only; local Java 23 still fails before report generation | Plugin/configuration updated for HTML/XML reports in CI Java 17. |

## Not Applicable Scope

`V10 OAuth and OIDC` is marked Not Applicable because GhostReport uses local
username/password authentication and self-contained JWTs, not OAuth/OIDC or an
external identity provider.

`V17 WebRTC` is marked Not Applicable because GhostReport has no media capture,
signaling, TURN/STUN, SRTP, DTLS media server or browser real-time communication
feature.

Mutual TLS/client-certificate requirements are Not Applicable where the current
architecture does not use client certificates for authentication or
authorization. Production TLS remains a deployment responsibility under V12.
