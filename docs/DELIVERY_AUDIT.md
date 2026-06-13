# GhostReport Delivery Audit

Date: 2026-06-09

This audit records the current state of GhostReport against implementation,
tests, workflows and evidence. It intentionally does not treat documentation as
proof unless it is supported by code, tests or generated artifacts.

## Executive Summary

GhostReport is in a defensible academic state for DESOFS: the backend has
meaningful controls for JWT authentication, RBAC, ownership, CSRF, CSP/security
headers, upload validation, backup integrity, audit logs, security alerts and
generic error handling. The `dev` workflow now covers build/test/JaCoCo,
Gitleaks, CodeQL/SpotBugs/SonarCloud, Dependency-Check/SBOM and ZAP/runtime
security evidence. PIT is correctly separated into a dedicated workflow because
it is slower and evidence-review oriented. A local Dependency-Check run on
2026-06-09 identified newly published Spring Framework findings against
`spring-core`/`spring-web` 6.2.18; the project now applies the compatible
6.2.19 patch override while keeping Spring Boot 3.5.14.

The main remaining risks are operational rather than small code fixes:
production schema management, external immutable log storage, retention,
application-level backup encryption, real reset-token delivery, production MFA
delivery/enrollment, authenticated/deep DAST and full agent-based IAST. These must stay
documented as limitations instead of being presented as completed controls.

## Priority Findings

| Priority | Problem | Evidence | Impact | Viable now | Proposed correction |
| --- | --- | --- | --- | --- | --- |
| Critical | Production-like mode uses `ddl-auto=validate`, but there is no formal migration chain in the application. | `ghostreport/src/main/resources/application.yaml`; entities such as `Report`, `CaseReview`, `AuditLog`, `SecurityAlert`, `PasswordResetToken`, `RevokedToken`. | A real PostgreSQL deployment can fail startup or drift from tested H2/dev schemas. | Partially | Added `docs/PRODUCTION_SCHEMA_CHECKLIST.md`; future work should add Flyway/Liquibase before production claims. |
| High | Audit/security events are tamper-evident per row but not externally immutable. | `AuditLog.integrityHash`, `SecurityAlert.integrityHash`, `AuditLogService`, `SecurityMonitoringService`. | Good academic evidence, but not WORM/SIEM-grade non-repudiation. | Documentation only | Keep ASVS status partial; require external SIEM/WORM and retention as operational hardening. |
| High | Password reset has secure token generation/storage, but no email/SMS delivery. | `PasswordResetService`, `PASSWORD_RESET_EXPOSE_TOKEN`, tests. | Cannot claim real end-user reset delivery; exposing tokens is demo/test only. | No, requires infrastructure | Keep production config with `expose-token=false`; document delivery as out of scope. |
| High | Admin MFA is implemented with an academic one-time code flow, but has no production delivery channel. | `AuthService`, `MfaChallengeService`, `AdminMfaAuthenticationTest`, `ghostreport.mfa.*` config. | Suitable for coursework/demo; production needs email/SMS, TOTP or IdP enrollment and recovery. | Partially | Keep production delivery/enrollment as explicit operational limitation. |
| High | IAST evidence is runtime/IAST-like, not agent-based IAST. | `docs/IAST_*`, `dast-scan` runtime tests and ZAP baseline. | False claims could hurt assessment. | Yes | Keep wording as runtime security / IAST-like evidence only. |
| High | Dependency-Check reported Spring Framework CVEs against 6.2.18. | Local `dependency-check-report`, `pom.xml`; Maven metadata showed Boot 3.5.14 latest 3.5.x and Framework 6.2.19 available. | Open SCA findings can hurt the security/quality assessment. | Yes | Added `spring-framework.version=6.2.19`; rerun SCA in CI to confirm closure. |
| Medium | PIT was still referenced as a build-test artifact in some docs. | `SECURITY_TESTING.md`, `docs/README.md`, `docs/SECURITY_ASSESSMENT.md`. | Confuses evidence collection and grading walkthrough. | Yes | Updated docs to point to `pit-mutation-testing`. |
| Medium | `actionlint` was described too strongly in ASVS evidence. | `docs/ASVS_EVIDENCE.md`. | Claim is fragile if no local actionlint output is archived. | Yes | Reworded as optional supporting validation when available. |
| Medium | CodeQL/ZAP notes referenced stale artifact names or pre-remediation ZAP evidence. | `docs/TECH_STACK_SECURITY_REVIEW.md`. | Reviewers may look for artifacts that are not produced by the current workflow. | Yes | Updated wording to current `sast-reports` and latest-run evidence. |
| Medium | Rate limiting is in-memory only. | `RateLimiterService` uses a local `ConcurrentHashMap`. | Works in CI/single instance; not distributed across nodes. | Documentation only | Keep as academic/single-node control; use Redis or gateway limits in production. |
| Medium | Backup ZIPs are integrity/authenticity protected but not encrypted by the application. | `BackupService`, `BackupServiceIntegrationTest`, `SECURE_INSTALLATION.md`. | Confidentiality depends on storage/volume controls. | No, larger feature | Keep as partial data-protection control; add encrypted storage or ZIP encryption later. |
| Low | Repository ignored `.DS_Store/` as a directory, not `.DS_Store` files. | `.gitignore`. | Cosmetic but can pollute evidence/package diffs. | Yes | Fixed `.gitignore`. |

## Confirmed Strengths

- JWT validation checks signature, `kid`, issuer, audience, expiry, `jti`, role
  and persistent revocation.
- RBAC and object ownership are covered by negative-path tests.
- CSP is strict and does not include `unsafe-inline` or `unsafe-eval`.
- CSRF is enabled for state-changing authenticated endpoints.
- Error handling returns generic responses with correlation IDs.
- Audit/security logs sanitize passwords, tokens, authorization values and
  tracking codes.
- Uploads validate size, MIME type, extension, magic bytes, path traversal and
  malware-scanner control flow.
- Backup manifests include hashes and HMAC validation; tampering and unsigned
  ZIP entries are rejected in tests.
- Code scanning/SCA findings for Hibernate Validator and Angus Activation are
  triaged with time-bounded Dependency-Check suppressions rather than hidden
  silently.

## Evidence Boundaries

- CodeQL primary evidence lives in GitHub Code Scanning; the workflow does not
  promise a local full CodeQL SARIF artifact.
- Dependency-Check and ZAP run in evidence-review mode and require triage.
- PIT is a separate evidence-review workflow with a final HTML index; it is not
  a fast merge gate.
- Runtime security evidence is IAST-like academic evidence, not commercial IAST.
- Production TLS, SIEM/WORM, log retention, production MFA delivery/enrollment and reset-token delivery are
  deployment or future hardening items.
