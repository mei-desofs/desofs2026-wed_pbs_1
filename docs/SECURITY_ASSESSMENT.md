# Security Assessment

This assessment summarizes GhostReport's implemented security controls and the
evidence used to verify them.

## Assessment Scope

| Area | Scope |
| --- | --- |
| Authentication | JWT login/logout flow, BCrypt password hashing, inactive-user checks and login rate limiting. |
| Authorization | RBAC for ADMIN, ANALYST and AUDITOR, plus analyst ownership controls. |
| Input validation | DTO validation, domain primitives and upload validation. |
| File handling | Safe upload storage, attachment access, evidence packages and backup verification. |
| Audit and monitoring | Audit logs and security alerts for security-relevant events. |
| Configuration | Runtime profiles, environment variables, JWT secret validation and seed-user controls. |
| DevSecOps | Build, tests, coverage, SAST, SCA, SBOM, secret scanning, DAST, runtime security/IAST readiness evidence and mutation testing. |

## Evidence Matrix

| Control | Evidence | Result | Status |
| --- | --- | --- | --- |
| Password hashing | `SecurityConfig.passwordEncoder()`, user creation tests | Passwords are stored with BCrypt. | Implemented |
| JWT signing, validation and revocation | `JwtService`, `AuthController`, `JwtServiceSecurityTest`, `RuntimeSecurityEventLoggingTest` | Signature, expiry, issuer, audience, role validation and logout-driven token revocation are tested. | Implemented with residual risk |
| JWT secret validation | `SecurityConfigurationValidator`, `.env.example`, validator tests | Unsafe production-like JWT configuration fails fast. | Implemented |
| Login abuse protection | `RateLimiterService`, `LoginRateLimitSecurityTest` | Repeated failures trigger rate limiting and alerts. | Implemented |
| Runtime auth monitoring | `RuntimeSecurityEventLoggingTest`, `AuditLogService`, `SecurityMonitoringService` | Auth events are recorded without passwords or tokens. | Implemented |
| RBAC | `SecurityConfig`, `RbacAuthorizationMatrixTest` | Role-specific access is verified. | Implemented |
| Analyst ownership | `AnalystCaseOwnershipTest`, service ownership checks | Analysts are restricted to owned/eligible cases. | Implemented |
| Public report confidentiality | `TrackingCode`, tracking code tests | Public tracking and attachment listing require valid tracking codes. | Implemented |
| Upload validation | `FileStorageService`, upload tests | Size, MIME, extension, magic bytes and safe paths are verified. | Implemented |
| Path traversal protection | `SafeFilename`, storage boundary checks | Malicious names and paths are rejected. | Implemented |
| Error handling | `GlobalExceptionHandler`, `ErrorHandlingSecurityTest` | Responses avoid stack traces and include correlation IDs. | Implemented |
| Security headers | `SecurityConfig`, `SecurityHeadersTest` | Browser-facing headers are configured and tested, including CSP `form-action 'self'`. | Implemented baseline |
| Audit logs | `AuditLogService`, audit tests | Critical state changes are logged with sanitized details. | Implemented |
| Security alerts | `SecurityMonitoringService`, alert tests | Suspicious activity creates security alerts. | Implemented |
| Backup integrity | `BackupService`, backup tests | Manifests and SHA-256 validation are implemented. | Implemented |
| Evidence packages | `CasePackageService`, package tests | Closed-case packages can be generated and verified. | Implemented |
| SAST | SpotBugs and CodeQL workflows | Static analysis evidence is generated; CodeQL primary evidence is GitHub Code Scanning plus a run summary artifact. | Evidence review |
| SCA/SBOM | Dependency-Check and CycloneDX workflows | Dependency risk and inventory evidence is generated. | Evidence review |
| Secret scanning | Gitleaks workflow | Repository secret scan evidence is generated. | Implemented |
| DAST | ZAP baseline workflow | Runtime HTTP baseline evidence is generated. | Evidence review |
| Runtime security / IAST readiness evidence | Runtime security workflow | Runtime security tests and optional Java agent readiness are documented. | Evidence review |
| Coverage and mutation testing | JaCoCo and PIT workflows | Coverage evidence is blocking in CI; PIT evidence captures report output or a fallback triage summary. | Evidence review |

## Gate Policy

| Check | Current behavior |
| --- | --- |
| Maven compile/test | Blocking |
| Security configuration validator tests | Blocking |
| JaCoCo report and baseline thresholds | Blocking in CI |
| Gitleaks | Blocking for confirmed leaks |
| SpotBugs, CodeQL, Dependency-Check, SBOM, ZAP, runtime security/IAST readiness and PIT | Evidence review |

## Tool Assessment Matrix

| Tool | Result | Evidence | Issues Identified | Issues Mitigated | Residual Risk |
| --- | --- | --- | --- | --- | --- |
| SpotBugs | Evidence generated and post-remediation report available | `Deliverables/Phase 2/Evidence/sast/spotbugs-post-remediation`, `docs/SPOTBUGS_TRIAGE.md` | Original report had 35 findings; post-remediation report has 21 findings | High-value mutable exposure, broad exception and newline findings reduced | Remaining framework/model findings require triage before suppression |
| Dependency-Check | Evidence generated from downloaded artifact; dependencies updated locally | `Deliverables/Phase 2/Evidence/sca`, `docs/SCA_TRIAGE.md` | Old report included critical/high dependency findings | Spring Boot, Tomcat, PostgreSQL JDBC and Log4j versions updated | Fresh Dependency-Check run required before closing CVEs |
| Gitleaks | Empty JSON report in downloaded evidence | `Deliverables/Phase 2/Evidence/secret-scanning` | No leaked secrets in scanned scope | Artifact reorganized into secret-scanning evidence | Scope depends on repository contents scanned by the workflow |
| ZAP | Baseline evidence generated before CSP remediation | `Deliverables/Phase 2/Evidence/dast` | CSP `unsafe-inline`, comments and cache informational alerts | Inline frontend code removed and CSP updated in code | Fresh ZAP run required to prove closure |
| CodeQL | Code Scanning plus archiveable summary | `Deliverables/Phase 2/Evidence/sast/sast-codeql-evidence-summary` | Findings are reviewed in GitHub Code Scanning | Summary artifact documents the run for local archive | Full local SARIF export is not claimed |
| PIT | Evidence-review workflow configured for HTML/XML output | `Deliverables/Phase 2/Evidence/testing`, `docs/SECURITY_TESTING.md` | Local Java 23 run fails before report generation | PIT plugin/configuration updated for CI Java 17 | Confirm real PIT report after CI run |
| JaCoCo | Blocking coverage gate passes locally | `ghostreport/target/site/jacoco`, CI JaCoCo artifact | Critical controllers/services still have uneven coverage | Added tests for session/security and admin evidence flows | Add more service/controller branch tests over time |
| JUnit/MockMvc | 110 tests pass locally | Surefire output, `ghostreport/src/test/java` | Security regression gaps remain in some negative paths | JWT revocation, login rate limit and admin evidence tests added | Upload fresh CI artifacts after push |

## Scope Boundaries

The assessment covers the implemented coursework application and its automated
security evidence. Additional production operations such as external SIEM,
privileged-user MFA, centralized rate limiting, distributed token revocation and advanced
deployment TLS management are considered operational hardening.

The current code removes `unsafe-inline` from the CSP and externalizes the
frontend scripts/styles that previously required it. The downloaded ZAP
artifact is pre-remediation, so the DAST evidence should be regenerated before
closing that finding in the final assessment.
