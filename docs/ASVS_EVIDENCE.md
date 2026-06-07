# ASVS 5.0 Evidence Mapping

This document supports the formal spreadsheet:

```text
Deliverables/Phase 2/ASVS_5.0_Tracker_Phase 2_Sprint 2.xlsx
```

The spreadsheet is the checklist of record. This file explains what evidence can
be used to justify the ASVS status values. Do not mark a requirement as
`Compliant` unless there is implementation plus test, pipeline or report
evidence.

## Assessment Rules

- `Compliant`: implemented and supported by automated test, pipeline evidence
  or a concrete artifact.
- `Partially Compliant`: implemented only partly, missing operational maturity,
  missing test depth or still under security triage.
- `Not Applicable`: outside GhostReport's architecture or coursework scope.
- `Not Started`: no defensible evidence found.

## Chapter Summary

| ASVS chapter | Current status | Evidence | Main gaps |
| --- | --- | --- | --- |
| V1 Encoding and Sanitization | Partially Compliant | DTO validation, domain value objects, safe filenames, safe frontend DOM rendering, CSP tests and ZAP baseline. | Continue expanding negative-path tests beyond the reviewed frontend XSS/data-exposure scope. |
| V2 Validation and Business Logic | Partially Compliant | DTO validation, service checks, domain invariants, rate limiting and ownership tests. | Business limits and workflow abuse cases need more explicit documentation/tests. |
| V3 Web Frontend Security | Partially Compliant | Security headers, CSP, externalized frontend scripts/styles, safe DOM rendering and `SecurityHeadersTest`. | Fresh ZAP evidence should confirm the latest CSP behaviour. |
| V4 API and Web Service | Partially Compliant | Controllers use DTOs, generic JSON errors and role-protected endpoints. | Cache behaviour, method restrictions and API abuse cases need more complete tests. |
| V5 File Handling | Partially Compliant | Upload validation, safe path handling, MIME/extension checks and file service tests. | No antivirus scanning, quarantine workflow or per-user storage quotas. |
| V6 Authentication | Partially Compliant | BCrypt, inactive-user checks, login rate limiting and login/logout audit events. | No MFA, secure password reset or password history/reuse policy. |
| V7 Session Management | Partially Compliant | Stateless JWT expiry, validation and logout-driven revocation evidence. | No refresh-token rotation, distributed revocation store or concurrent-session controls. |
| V8 Authorization | Partially Compliant | `ADMIN`, `ANALYST`, `AUDITOR` rules, RBAC tests and analyst ownership tests. | Field-level authorization and service-level negative paths can be expanded. |
| V9 Self-contained Tokens | Partially Compliant | JWT signature, expiry, issuer/audience and revocation tests. | No key rotation or distributed revocation strategy. |
| V10 OAuth and OIDC | Not Applicable | GhostReport does not use OAuth/OIDC or an external IdP. | Becomes applicable if an IdP is added. |
| V11 Cryptography | Partially Compliant | BCrypt, JWT HMAC and SHA-256 integrity hashes. | No formal key lifecycle/rotation plan. |
| V12 Secure Communication | Partially Compliant | Security headers and installation guidance for TLS deployment. | CI DAST runs on local HTTP and does not prove production TLS/cipher configuration. |
| V13 Configuration | Partially Compliant | Profiles, environment variables, fail-fast validation, Gitleaks and SCA evidence. | Residual dependency findings require documented triage. |
| V14 Data Protection | Partially Compliant | DTO responses avoid passwords/tokens, tracking codes are not placed in frontend URLs and file/package integrity checks exist. | Retention, deletion and encryption-at-rest policies are incomplete. |
| V15 Secure Coding and Architecture | Partially Compliant | Single `dev` pipeline, tests, JaCoCo, SpotBugs, SonarCloud, CodeQL, Dependency-Check, CycloneDX, Gitleaks, ZAP and PIT evidence review. | Security findings remain triage-driven; PIT is not a blocking quality gate. |
| V16 Security Logging and Error Handling | Partially Compliant | Audit/security event services, sanitized error handling and runtime security tests. | No tamper-resistant logs or external SIEM integration. |
| V17 WebRTC | Not Applicable | No WebRTC, TURN/STUN, media capture or real-time browser communication exists. | None in current scope. |

## Evidence References

| Evidence type | Repository references |
| --- | --- |
| Authentication | `ghostreport/src/main/java/com/ghostreport/controller/AuthController.java`, `ghostreport/src/main/java/com/ghostreport/service/AuthService.java`, `ghostreport/src/main/java/com/ghostreport/service/JwtService.java` |
| Authorization | `ghostreport/src/main/java/com/ghostreport/security/SecurityConfig.java`, `ghostreport/src/test/java/com/ghostreport/security/RbacAuthorizationMatrixTest.java` |
| Input validation | `ghostreport/src/main/java/com/ghostreport/dto`, `ghostreport/src/main/java/com/ghostreport/domain`, `ghostreport/src/test/java/com/ghostreport/domain` |
| File handling | `ghostreport/src/main/java/com/ghostreport/service/FileStorageService.java`, `ghostreport/src/test/java/com/ghostreport/service/FileStorageServiceTest.java` |
| Backup and integrity | `ghostreport/src/main/java/com/ghostreport/service/BackupService.java`, `ghostreport/src/test/java/com/ghostreport/service/BackupServiceIntegrationTest.java` |
| Error handling | `ghostreport/src/main/java/com/ghostreport/exception/GlobalExceptionHandler.java`, `ghostreport/src/test/java/com/ghostreport/security/ErrorHandlingSecurityTest.java` |
| Runtime security events | `ghostreport/src/main/java/com/ghostreport/service/SecurityMonitoringService.java`, `ghostreport/src/test/java/com/ghostreport/security/RuntimeSecurityEventLoggingTest.java` |
| Pipeline evidence | `.github/workflows/dev.yml`, GitHub Actions job summaries and downloaded artifacts |
| Local evidence archive | `Deliverables/Phase 2/Evidence`, populated manually from downloaded GitHub Actions artifacts |

## Frontend XSS and Data Exposure Evidence

Scope covered in this sprint update: `V1.2.1`, `V1.2.2`, `V1.2.3`,
`V3.2.2`, `V14.2.1` and re-evaluation of `V14.3.1`.

| ASVS ID | Evidence | Status rationale |
| --- | --- | --- |
| V1.2.1 | `ghostreport/src/main/resources/static/js/dom.js`; `ghostreport/src/main/resources/static/js/*.js`; `ghostreport/src/test/java/com/ghostreport/security/FrontendXssDataExposureTest.java` | Frontend rendering now uses DOM APIs such as `createElement`, `textContent`, `createTextNode` and `replaceChildren`. Static tests fail if dangerous HTML parsing sinks are reintroduced. |
| V1.2.2 | `ghostreport/src/main/resources/static/js/submit.js`; `ghostreport/src/main/resources/static/js/track.js`; `FrontendXssDataExposureTest` | Generated URLs use fixed paths or `encodeURIComponent` for path variables. The public tracking flow no longer redirects with a tracking code in the query string. |
| V1.2.3 | `ghostreport/src/main/resources/static/js/dom.js`; `ghostreport/src/main/resources/static/js/admin.js`; `ghostreport/src/main/resources/static/js/analyst.js`; `ghostreport/src/main/resources/static/js/auditor.js` | API, URL and user-controlled values are inserted as text nodes or safe attributes through DOM APIs instead of string-concatenated HTML. |
| V3.2.2 | `ghostreport/src/test/java/com/ghostreport/security/FrontendXssDataExposureTest.java` | The regression test scans frontend JavaScript for dangerous DOM sinks and verifies that XSS payload characters are handled through text-node APIs rather than manual escaping plus HTML parsing. |
| V14.2.1 | `ghostreport/src/main/resources/static/js/submit.js`; `ghostreport/src/main/resources/static/js/track.js`; `FrontendXssDataExposureTest` | Tracking codes remain report-access secrets and are only submitted in request bodies where required. They are not placed in browser URLs, redirects or query-string parsers. |
| V14.3.1 | `ghostreport/src/main/resources/static/js/admin.js`; `ghostreport/src/main/resources/static/js/analyst.js`; `ghostreport/src/main/resources/static/js/auditor.js`; `FrontendXssDataExposureTest` | This control is applicable because GhostReport has browser-based authenticated panels. Bearer tokens are now kept only in JavaScript memory for the active page lifecycle and are not persisted in browser storage. Refreshing the page requires login again. |

## Tool Evidence Status

| Tool | Current evidence | Artifact/location | Status wording |
| --- | --- | --- | --- |
| JUnit/MockMvc | Local run passed with 121 tests and the workflow uploads Surefire reports. | `ci-surefire-test-reports`, `ghostreport/target/surefire-reports` | Blocking test evidence. |
| JaCoCo | Coverage report and coverage check run in `build-test`. | `ci-jacoco-coverage-report`, `ghostreport/target/site/jacoco` | Blocking coverage evidence. |
| PIT | PIT runs in evidence review mode and uploads fallback summary/exit code when needed. | `pit-mutation-testing-report` | Evidence review, not a blocking mutation score gate. |
| Gitleaks | Repository secret scan runs before dependent security jobs. Empty JSON means no leaks in scanned scope. | `secret-scan-gitleaks-json` | Blocking for confirmed leaks. |
| SpotBugs | XML/static analysis output is uploaded with SAST evidence. | `sast-reports` | Evidence review. |
| SonarCloud | Runs in SAST job when `SONAR_TOKEN` is configured. | `sast-reports`, SonarCloud UI | Evidence review; required secret must exist. |
| CodeQL | CodeQL runs and uploads primary findings to GitHub Code Scanning. | GitHub Code Scanning, `sast-reports` summary | Evidence review; local full SARIF is not claimed. |
| Dependency-Check | HTML/XML/JSON/SARIF reports are generated in evidence mode. | `dependency-check-sca-reports` | Evidence review with manual triage. |
| CycloneDX | SBOM is generated in JSON/XML. | `sbom-cyclonedx` | Dependency inventory evidence. |
| Runtime security / IAST readiness | Security-focused runtime tests run with JaCoCo skipped and upload Surefire plus readiness notes. | `iast-runtime-security-evidence` | Runtime evidence always; external IAST only if Contrast variables/secrets exist. |
| ZAP | Baseline DAST runs against a live CI application instance. | `dast-zap-baseline-reports` | Evidence review baseline DAST. |
| actionlint | Workflow syntax/semantics validated locally. | Local terminal output or pipeline notes | Supporting pipeline evidence. |

## Not Applicable Scope

`V10 OAuth and OIDC` is Not Applicable because GhostReport uses local
username/password authentication and self-contained JWTs, not OAuth/OIDC, PKCE,
authorization-code flow or token exchange with an external identity provider.

`V17 WebRTC` is Not Applicable because GhostReport has no browser media capture,
signaling, TURN/STUN, SRTP, DTLS media server or real-time communication
feature.

Mutual TLS/client-certificate requirements are Not Applicable where the current
architecture does not use client certificates for authentication or
authorization. Production TLS remains a deployment responsibility under V12.
