# Security Assessment

This assessment summarizes GhostReport's implemented security controls and the
evidence used to verify them. It should be read together with
`docs/ASVS_EVIDENCE.md`, `docs/DEVSECOPS_PIPELINE.md` and the ASVS tracker.

## Assessment Scope

| Area | Scope |
| --- | --- |
| Authentication | JWT login/logout flow, BCrypt password hashing, inactive-user checks, login rate limiting, password policy, password history, authenticated password change and password reset tokens. |
| Authorization | RBAC for `ADMIN`, `ANALYST` and `AUDITOR`, plus analyst ownership controls. |
| Input validation | DTO validation, domain primitives and upload validation. |
| File handling | Safe upload storage, attachment access, evidence packages and backup verification. |
| Audit and monitoring | Audit logs and security alerts for security-relevant events. |
| Configuration | Runtime profiles, environment variables, JWT secret validation and seed-user controls. |
| DevSecOps | The single `dev` GitHub Actions workflow: build, tests, coverage, secret scanning, SAST, SCA, SBOM, DAST, runtime security/IAST readiness and PIT evidence review. |

## Evidence Matrix

| Control | Evidence | Result | Status |
| --- | --- | --- | --- |
| Password hashing | `SecurityConfig.passwordEncoder()`, user creation tests | Passwords are stored with BCrypt. | Implemented |
| Password policy | `PasswordPolicyService`, `PasswordPolicyAndResetSecurityTest` | Compromised-password examples are rejected and password reuse is checked against current/history hashes. | Implemented |
| Authenticated password change | `AuthController`, `UserService`, `ChangePasswordRequest` | Current password is required and new password is stored only as a hash. | Implemented |
| Password reset | `PasswordResetService`, `PasswordResetToken`, reset tests | Reset tokens are random, single-use, expiring and stored only as SHA-256 hashes. | Implemented |
| JWT signing, validation and revocation | `JwtService`, `AuthController`, `JwtServiceSecurityTest`, `RuntimeSecurityEventLoggingTest` | Signature, expiry, issuer, audience, role validation and logout-driven revocation are tested. | Implemented with residual risk |
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
| DevSecOps pipeline | `.github/workflows/dev.yml` | One visible workflow timeline with dependent jobs and downloadable artifacts. | Implemented |
| SAST | SpotBugs, SonarCloud and CodeQL in the `sast` job | Static analysis evidence is generated; CodeQL primary evidence is GitHub Code Scanning. | Evidence review |
| SCA/SBOM | Dependency-Check and CycloneDX in the `dependency-scanning` job | Dependency risk and inventory evidence is generated. | Evidence review |
| Secret scanning | Gitleaks in `security-secrets` | Repository secret scan evidence is generated and confirmed leaks block the workflow. | Implemented |
| DAST | ZAP baseline in `dast-scan` | Runtime HTTP baseline evidence is generated against a live CI application instance. | Evidence review |
| Runtime security / IAST readiness | Runtime tests and optional Contrast readiness notes in `dast-scan` | Runtime security tests always run; external IAST telemetry is optional. | Evidence review |
| Coverage and mutation testing | JaCoCo and PIT in `build-test` | Coverage is blocking; PIT is evidence review with report/fallback artifact. | Evidence review |

## Gate Policy

| Check | Current behavior |
| --- | --- |
| Maven compile/test | Blocking |
| Security configuration validator tests | Blocking |
| JaCoCo report and coverage check | Blocking in `build-test` |
| Gitleaks | Blocking for confirmed leaks |
| Runtime security tests | Blocking inside `dast-scan` |
| Application startup for ZAP | Blocking inside `dast-scan` |
| PIT | Evidence review |
| SpotBugs, SonarCloud, CodeQL, Dependency-Check, CycloneDX and ZAP | Evidence review with manual triage |

## Tool Assessment Matrix

| Tool | Result | Evidence | Residual risk |
| --- | --- | --- | --- |
| JUnit/MockMvc | Latest local run passed with 123 tests. | `ci-surefire-test-reports`, `ghostreport/target/surefire-reports` | Add more negative-path tests for admin, report and backup workflows over time. |
| JaCoCo | Coverage gate passes locally and runs in CI. | `ci-jacoco-coverage-report`, `ghostreport/target/site/jacoco` | Some controllers/services can still be improved, but the current gate is passing. |
| Gitleaks | Generates JSON evidence and blocks confirmed leaks. | `secret-scan-gitleaks-json` | Workspace-wide local scans can include ignored diagnostic files; use repository-scope CI evidence for assessment. |
| SpotBugs | Runs in the SAST job and uploads XML evidence. | `sast-reports` | Findings require triage before suppression or acceptance. |
| SonarCloud | Runs when `SONAR_TOKEN` is configured. | `sast-reports`, SonarCloud UI | The job depends on repository secrets/variables being configured. |
| CodeQL | Publishes primary findings to GitHub Code Scanning. | GitHub Code Scanning, `sast-reports` summary | Local full SARIF archive is not claimed by this workflow. |
| Dependency-Check | Runs in evidence mode and uploads reports. | `dependency-check-sca-reports` | Findings require applicability and upgrade triage. |
| CycloneDX | Generates JSON/XML SBOM. | `sbom-cyclonedx` | SBOM is inventory evidence, not vulnerability triage by itself. |
| ZAP | Baseline DAST runs against a live app in CI. | `dast-zap-baseline-reports` | Baseline scan is unauthenticated and should be treated as first-line DAST evidence. |
| Runtime security / IAST readiness | Security-focused tests run with JaCoCo skipped and upload Surefire plus readiness notes. | `iast-runtime-security-evidence` | Contrast/IAST telemetry is optional and only exists when configured. |
| PIT | Runs in evidence review mode and uploads summary/exit code. | `pit-mutation-testing-report` | Mutation score is not a blocking Sprint 2 gate. |
| actionlint | Current `dev.yml` validated locally with actionlint 1.7.12. | Local command output or pipeline validation notes | Does not replace a real GitHub Actions run. |

## Scope Boundaries

The current assessment covers the implemented coursework application and its
automated security evidence. External SIEM, privileged-user MFA, distributed
token revocation, authenticated deep DAST, production TLS operations and
advanced monitoring are documented as future operational hardening. MFA remains
out of scope for Sprint 2 because the application does not integrate an
authenticator app, email/SMS provider or external identity provider.

The local folder `Deliverables/Phase 2/Evidence` is not automatically written by
GitHub Actions. It is a curated archive populated from downloaded workflow
artifacts using `scripts/collect-evidence.ps1`.
