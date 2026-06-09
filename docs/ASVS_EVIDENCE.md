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
| V2 Validation and Business Logic | Partially Compliant | DTO validation, service checks, domain invariants, explicit workflow transitions, transactional state changes, optimistic locking, rate limiting and ownership tests. | Business limits outside report/case workflows can still be expanded. |
| V3 Web Frontend Security | Partially Compliant | Security headers, CSP, externalized frontend scripts/styles, safe DOM rendering and `SecurityHeadersTest`. | Fresh ZAP evidence should confirm the latest CSP behaviour. |
| V4 API and Web Service | Partially Compliant | Controllers use DTOs, generic JSON errors and role-protected endpoints. | Cache behaviour, method restrictions and API abuse cases need more complete tests. |
| V5 File Handling | Partially Compliant | Upload validation, safe path handling, MIME/extension checks, magic-byte checks, mockable malware scanner, quarantine and secure download headers. | Local scanner is coursework evidence only; production should integrate a real AV service and define retention for quarantined files. |
| V6 Authentication | Partially Compliant | BCrypt, inactive-user checks, login rate limiting, compromised-password denylist, password history/reuse prevention, authenticated password change and one-time expiring password reset tokens. | MFA is not implemented; reset delivery is represented by a generated token because no email/SMS provider is in scope. |
| V7 Session Management | Partially Compliant | Stateless JWT expiry, validation and database-backed logout revocation evidence. | No refresh-token rotation or concurrent-session inventory controls. |
| V8 Authorization | Compliant for current scope | `ADMIN`, `ANALYST`, `AUDITOR` rules, object ownership checks, field-level filtering, authorization matrix and negative-path tests. | Future roles or workflow changes must update the matrix and tests before tracker changes. |
| V9 Self-contained Tokens | Partially Compliant | JWT signature, expiry, issuer/audience, `jti`, `kid`, key rotation and persistent revocation tests. | Rotation is configuration-driven; there is no JWKS endpoint or automated rollover scheduler. |
| V10 OAuth and OIDC | Not Applicable | GhostReport does not use OAuth/OIDC or an external IdP. | Becomes applicable if an IdP is added. |
| V11 Cryptography | Partially Compliant | BCrypt, JWT HMAC with key identifiers and SHA-256 integrity hashes. | JWT rotation is implemented, but broader key custody and backup key lifecycle remain operational controls. |
| V12 Secure Communication | Partially Compliant | Security headers and installation guidance for TLS deployment. | CI DAST runs on local HTTP and does not prove production TLS/cipher configuration. |
| V13 Configuration | Partially Compliant | Profiles, environment variables, fail-fast validation, Gitleaks and SCA evidence. | Residual dependency findings require documented triage. |
| V14 Data Protection | Partially Compliant | DTO responses avoid passwords/tokens, tracking codes are not placed in frontend URLs and file/package integrity checks exist. | Retention, deletion and encryption-at-rest policies are incomplete. |
| V15 Secure Coding and Architecture | Partially Compliant | Single `dev` pipeline, tests, JaCoCo, SpotBugs, SonarCloud, CodeQL, Dependency-Check, CycloneDX, Gitleaks, ZAP and PIT evidence review. | Security findings remain triage-driven; PIT is not a blocking quality gate. |
| V16 Security Logging and Error Handling | Partially Compliant | Audit/security event services, correlation IDs, UTC timestamps, redaction, integrity hashes, sanitized error handling and runtime security tests. | No external SIEM, WORM storage or automated retention policy. |
| V17 WebRTC | Not Applicable | No WebRTC, TURN/STUN, media capture or real-time browser communication exists. | None in current scope. |

## Evidence References

| Evidence type | Repository references |
| --- | --- |
| Authentication | `ghostreport/src/main/java/com/ghostreport/controller/AuthController.java`, `ghostreport/src/main/java/com/ghostreport/service/AuthService.java`, `ghostreport/src/main/java/com/ghostreport/service/JwtService.java` |
| Authorization | `docs/AUTHORIZATION_MATRIX.md`, `ghostreport/src/main/java/com/ghostreport/security/SecurityConfig.java`, `ghostreport/src/main/java/com/ghostreport/service/ReportService.java`, `ghostreport/src/main/java/com/ghostreport/service/CaseReviewService.java`, `ghostreport/src/test/java/com/ghostreport/security/RbacAuthorizationMatrixTest.java`, `ghostreport/src/test/java/com/ghostreport/security/AnalystCaseOwnershipTest.java` |
| Password policy and reset | `ghostreport/src/main/java/com/ghostreport/service/PasswordPolicyService.java`, `ghostreport/src/main/java/com/ghostreport/service/PasswordResetService.java`, `ghostreport/src/main/java/com/ghostreport/model/PasswordHistory.java`, `ghostreport/src/main/java/com/ghostreport/model/PasswordResetToken.java`, `ghostreport/src/test/java/com/ghostreport/security/PasswordPolicyAndResetSecurityTest.java` |
| Business workflow | `ghostreport/src/main/java/com/ghostreport/service/ReportWorkflowPolicy.java`, `ghostreport/src/main/java/com/ghostreport/service/ReportService.java`, `ghostreport/src/main/java/com/ghostreport/service/CaseReviewService.java`, `ghostreport/src/main/java/com/ghostreport/model/Report.java`, `ghostreport/src/main/java/com/ghostreport/model/CaseReview.java`, `ghostreport/src/test/java/com/ghostreport/security/BusinessLogicWorkflowSecurityTest.java` |
| Input validation | `ghostreport/src/main/java/com/ghostreport/dto`, `ghostreport/src/main/java/com/ghostreport/domain`, `ghostreport/src/test/java/com/ghostreport/domain` |
| File handling | `ghostreport/src/main/java/com/ghostreport/service/FileStorageService.java`, `ghostreport/src/main/java/com/ghostreport/service/MalwareScanner.java`, `ghostreport/src/main/java/com/ghostreport/service/LocalMalwareScanner.java`, `ghostreport/src/main/java/com/ghostreport/service/ReportService.java`, `ghostreport/src/test/java/com/ghostreport/service/FileStorageServiceTest.java`, `ghostreport/src/test/java/com/ghostreport/controller/ReportControllerAttachmentUploadTest.java` |
| Backup and integrity | `ghostreport/src/main/java/com/ghostreport/service/BackupService.java`, `ghostreport/src/test/java/com/ghostreport/service/BackupServiceIntegrationTest.java`, `docs/SECURE_INSTALLATION.md` |
| Error handling | `ghostreport/src/main/java/com/ghostreport/exception/GlobalExceptionHandler.java`, `ghostreport/src/test/java/com/ghostreport/security/ErrorHandlingSecurityTest.java` |
| Runtime security events | `ghostreport/src/main/java/com/ghostreport/service/SecurityMonitoringService.java`, `ghostreport/src/main/java/com/ghostreport/service/AuditLogService.java`, `ghostreport/src/main/java/com/ghostreport/security/CorrelationIdFilter.java`, `ghostreport/src/test/java/com/ghostreport/security/RuntimeSecurityEventLoggingTest.java` |
| Pipeline evidence | `.github/workflows/dev.yml`, GitHub Actions job summaries and downloaded artifacts |
| Local evidence archive | `Deliverables/Phase 2/Evidence`, populated manually from downloaded GitHub Actions artifacts |

## Requested Logging and Monitoring ASVS Items

| ASVS ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| V16.1.1 | Compliant | `AuditLogService`, `SecurityMonitoringService`, `RuntimeSecurityEventLoggingTest` | Security-relevant events are recorded as audit logs or security alerts. |
| V16.2.2 | Compliant | `AuthService`, `AuthController`, `RuntimeSecurityEventLoggingTest` | Login success, login failure and logout are logged without passwords/tokens. |
| V16.2.3 | Compliant | `JwtAuthenticationFilter`, `SecurityMonitoringService`, `RuntimeSecurityEventLoggingTest` | Invalid JWTs create security alerts without storing token values. |
| V16.2.4 | Compliant | `SecurityConfig`, `SecurityMonitoringService`, `RuntimeSecurityEventLoggingTest` | Forbidden access attempts create security alerts with endpoint context. |
| V16.2.5 | Compliant | `ReportService.recordUploadRejected`, upload security tests | Rejected uploads are logged and repeated rejection creates alerts. |
| V16.3.1 | Compliant | `CorrelationIdFilter`, `GlobalExceptionHandler`, DTO responses, runtime tests | `X-Correlation-ID` is accepted/created, propagated to responses and stored with audit/security events. |
| V16.3.2 | Compliant | `AuditLog`, `SecurityAlert`, `AuditLogService`, `SecurityMonitoringService` | Audit and alert timestamps are generated using UTC. |
| V16.3.4 | Partially Compliant | `AuditLog.integrityHash`, `SecurityAlert.integrityHash`, DTOs | Events include a SHA-256 integrity hash. External append-only/WORM storage is not implemented. |
| V16.4.1 | Compliant | `SecurityLogSanitizer`, runtime tests | Passwords, bearer tokens, reset tokens, authorization values and tracking codes are redacted from event details. |
| V16.4.2 | Compliant | `GlobalExceptionHandler`, `ErrorHandlingSecurityTest` | Error responses use generic messages and correlation IDs, avoiding stack traces. |
| V16.4.3 | Compliant | `AuditController`, `AdminController`, DTO responses | Audit/security event access is role-protected and returned through DTOs. |
| V16.5.2 | Partially Compliant | `SecurityMonitoringService`, `SecurityAlertRepository`, runtime tests | Security alerts are generated for relevant suspicious events; no external SIEM integration. |
| V16.5.3 | Partially Compliant | `docs/SECURITY_ASSESSMENT.md`, `docs/SECURE_INSTALLATION.md` | Retention/protection expectations are documented; automated retention and tamper-proof archive are future operational hardening. |

## Requested Cryptography and Backup Protection ASVS Items

| ASVS ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| V11.1.1 | Compliant | `JwtService`, `BackupService`, `PasswordResetService`, `SecurityConfigurationValidatorTest` | Cryptographic controls use platform cryptography APIs: HMAC-SHA256, SHA-256, BCrypt and `SecureRandom`; no custom algorithms are implemented. |
| V11.1.2 | Compliant | `SecurityConfigurationValidator`, `application.yaml`, `.env.example`, `docs/SECURE_INSTALLATION.md` | Secrets are externalized and production-like startup fails for missing/weak values. |
| V11.1.3 | Compliant | `SecurityConfigurationValidatorTest.rejectsBackupHmacSecretReusedFromJwtSecret` | JWT and backup HMAC keys are logically separated and cannot reuse the same configured value. |
| V11.1.4 | Partially Compliant | `docs/SECURE_INSTALLATION.md` | Key lifecycle and rotation guidance is documented; automated rotation and secret-manager integration are future operational work. |
| V11.2.1 | Compliant | `JwtService`, `BackupService`, `PasswordResetService` | Approved primitives are used for implemented needs: HMAC-SHA256 for tokens/manifests, SHA-256 for integrity hashes, BCrypt for passwords and random reset tokens. |
| V11.2.2 | Compliant | `SecurityConfigurationValidator`, `JwtService`, `BackupService` | Weak symmetric secrets under 32 bytes are rejected. |
| V11.2.3 | Partially Compliant | `docs/SECURE_INSTALLATION.md`, `.env.example` | Secrets are configured through environment/deployment secrets; external KMS/HSM storage is documented as an operational improvement. |
| V11.2.4 | Compliant | `BackupServiceIntegrationTest.rejectsManifestTamperingEvenWhenZipSidecarHashIsUpdated` | Authenticated HMAC protects backup manifest integrity beyond unauthenticated ZIP hash sidecars. |
| V11.2.5 | Partially Compliant | `docs/SECURE_INSTALLATION.md` | Rotation is supported manually with `BACKUP_HMAC_KEY_ID`; automatic multi-key validation is not implemented. |
| V11.3.1 | Compliant | `BackupService`, `BackupServiceIntegrationTest.createsBackupWithManifestDatabaseExportsFileHashesAndFinalZipHash` | Backup manifests include SHA-256 hashes for exported files. |
| V11.3.2 | Compliant | `BackupService.verifyBackupFile`, `BackupServiceIntegrationTest.rejectsTamperedBackup` | Modified backup ZIP/content is rejected during validation. |
| V11.3.3 | Compliant | `BackupService.restoreBackup`, `BackupServiceIntegrationTest.restoreRejectsUnsignedBackupEntryEvenWhenZipSidecarHashIsUpdated` | Restore validates backup integrity and rejects unsigned ZIP entries before staging. |
| V11.3.4 | Partially Compliant | `BackupService`, `docs/SECURE_INSTALLATION.md` | Integrity and authenticity are implemented; confidentiality of backup ZIP contents depends on storage controls because application-level backup encryption is not implemented. |
| V11.3.5 | Compliant | `SecurityMonitoringService.recordBackupIntegrityFailure`, `BackupServiceIntegrationTest.rejectsTamperedBackup` | Backup integrity failures create security alerts. |
| V11.7.1 | Partially Compliant | `docs/SECURE_INSTALLATION.md`, `.env.example`, `.gitleaks.toml` | Secrets must not be committed and are supplied by environment/secrets; no external secrets manager is wired in this academic scope. |
| V11.7.2 | Partially Compliant | `BACKUP_HMAC_KEY_ID`, `docs/SECURE_INSTALLATION.md` | Key identifiers and manual rotation guidance exist; automated rotation and audit of key custody are future production hardening. |

## Requested Business Logic ASVS Items

| ASVS ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| V2.3.1 | Compliant | `ReportWorkflowPolicy`, `ReportService.updateReportStatus`, `BusinessLogicWorkflowSecurityTest.permittedStatusTransitionSucceedsForOwningAnalyst` | Report states are changed through an explicit transition matrix instead of free-form `setStatus` from API input. |
| V2.3.2 | Compliant | `ReportWorkflowPolicy`, `BusinessLogicWorkflowSecurityTest.forbiddenStatusTransitionFailsAndKeepsPreviousState` | Invalid workflow jumps, such as `SUBMITTED` directly to `RESOLVED`, are rejected and the previous state is preserved. |
| V2.3.3 | Compliant | `ReportService.validateWorkflowActorRole`, `CaseReviewService.validateCaseEditorRole`, `BusinessLogicWorkflowSecurityTest.userWithoutWorkflowRoleCannotChangeReportStatus` | Workflow state changes require `ANALYST` or `ADMIN`; read-only auditor access cannot mutate report state. |
| V2.3.4 | Compliant | `ReportService.checkInternalAccessToReport`, `CaseReviewService.getAccessibleCaseReview`, `BusinessLogicWorkflowSecurityTest.analystWhoDoesNotOwnCaseCannotChangeReportStatus` | Analyst actions are constrained by case ownership; analysts cannot mutate another analyst's assigned case. |
| V2.4.1 | Compliant | `@Transactional` on report/case workflow operations, `BusinessLogicWorkflowSecurityTest.closedCaseWorkflowDataCannotBePartiallyModified` | Critical state mutations run in transactions and invalid operations leave no partial update. |
| V2.4.2 | Compliant | `@Version` on `Report` and `CaseReview`, `GlobalExceptionHandler.handleOptimisticLockingFailure`, `BusinessLogicWorkflowSecurityTest.concurrentReportUpdatesAreRejectedByOptimisticLocking` | Concurrent stale writes are rejected using optimistic locking and translated to `409 Conflict` at the API boundary. |

### Report Workflow Matrix

| Current status | Allowed next statuses |
| --- | --- |
| `SUBMITTED` | `UNDER_REVIEW`, `REJECTED` |
| `UNDER_REVIEW` | `MORE_INFO_REQUIRED`, `RESOLVED`, `REJECTED` |
| `MORE_INFO_REQUIRED` | `UNDER_REVIEW`, `RESOLVED`, `REJECTED` |
| `RESOLVED` | Terminal state, no further transitions |
| `REJECTED` | Terminal state, no further transitions |

## Stateless Session and JWT Evidence

Scope covered in this sprint update: `V7.2.1`, `V7.2.4`, `V7.4.2`,
`V7.5.3`, `V9.2.1`, `V9.2.2`, `V9.2.3`, `V9.2.4`, `V11.3.1` and
`V11.3.2`.

| ASVS ID | Evidence | Status rationale |
| --- | --- | --- |
| V7.2.1 | `ghostreport/src/main/java/com/ghostreport/service/JwtService.java`; `ghostreport/src/main/java/com/ghostreport/security/JwtAuthenticationFilter.java`; `ghostreport/src/test/java/com/ghostreport/security/JwtServiceSecurityTest.java` | JWTs remain stateless, include `exp`, `iat`, `jti`, `iss`, `aud`, `role` and `kid`, and are rejected when expired, malformed, signed with an unknown key or issued for the wrong user/role. |
| V7.2.4 | `ghostreport/src/main/java/com/ghostreport/controller/AuthController.java`; `ghostreport/src/main/java/com/ghostreport/model/RevokedToken.java`; `ghostreport/src/main/java/com/ghostreport/service/PersistentRevokedTokenStore.java`; `RuntimeSecurityEventLoggingTest` | Logout extracts the Bearer token, persists its `jti` until token expiry and subsequent use of the same token is rejected. |
| V7.4.2 | `ghostreport/src/main/java/com/ghostreport/security/SecurityConfig.java`; `JwtService` | GhostReport uses `SessionCreationPolicy.STATELESS` and Bearer JWTs rather than server HTTP sessions or authentication cookies. CSRF cookie is not an auth secret. |
| V7.5.3 | `JwtRevocationPersistenceIntegrationTest` | Replay of a logged-out token is blocked by the persisted `jti` revocation record, including after replacing the `JwtService` instance. Concurrent-session inventory is not implemented. |
| V9.2.1 | `JwtService`; `JwtRevocationPersistenceIntegrationTest` | Revocation is keyed by `jti`, stored in the database and considered valid until `expires_at`. Expired revocation rows are purged opportunistically during revoke operations. |
| V9.2.2 | `JwtService`; `application.yaml`; `SECURE_INSTALLATION.md` | Issued JWT headers include `kid`; validation requires a known `kid` and rejects missing/unknown key identifiers. |
| V9.2.3 | `JwtServiceSecurityTest` | Tokens signed by configured previous keys are accepted during rotation, while newly issued tokens use the active key identifier. |
| V9.2.4 | `JwtServiceSecurityTest` | Tokens with invalid `issuer`, invalid `audience`, invalid signature, expiry or unknown key id are rejected even when structurally valid. |
| V11.3.1 | `application.yaml`; `JwtService`; `SECURE_INSTALLATION.md` | JWT signing secrets are externalized, length-validated and identified by `JWT_ACTIVE_KEY_ID`; previous keys are configured separately for validation-only rotation windows. |
| V11.3.2 | `JwtService`; `JwtServiceSecurityTest` | Key rotation is supported through active and previous key sets. Old keys validate existing tokens but are not used to issue new tokens. |
## Requested Authentication ASVS Items

| ASVS ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| V6.1.1 | Partially Compliant | `CreateUserRequest`, `ChangePasswordRequest`, `PasswordResetConfirmRequest`, `PasswordPolicyService` | Minimum length and complexity are enforced; local denylist adds compromised-password screening. |
| V6.2.2 | Compliant | `PasswordPolicyService`, `PasswordHistory`, `PasswordPolicyAndResetSecurityTest` | New passwords are checked against current and recent password hashes with `PasswordEncoder.matches`. |
| V6.2.3 | Compliant | `PasswordPolicyService`, `UserService`, `PasswordPolicyAndResetSecurityTest` | Compromised examples from the local denylist are rejected before hashing/storage. |
| V6.2.4 | Compliant | `PasswordResetService`, `PasswordResetToken`, `PasswordPolicyAndResetSecurityTest` | Reset tokens are random, stored as SHA-256 hashes, single-use and expiring. |
| V6.2.5 | Compliant | `UserService.changePassword`, `AuthController`, `PasswordPolicyAndResetSecurityTest` | Authenticated password change requires the current password. |
| V6.3.1 | Not Applicable / Out of Scope | Documentation in this file and `docs/SECURITY_ASSESSMENT.md` | MFA is not implemented because the coursework app has no authenticator app, email/SMS provider or external IdP integration. |

## File Upload, Malware Scanning and Download Evidence

Scope covered in this sprint update: `V5.1.1`, `V5.2.4`, `V5.4.3` and
`V3.2.1` for download response headers.

| ASVS ID | Evidence | Status rationale |
| --- | --- | --- |
| V5.1.1 | `FileStorageService.validateFile`, `FileStorageServiceTest`, `ReportControllerAttachmentUploadTest` | Uploads are constrained by size, safe filename validation, allowlisted MIME types, extension-to-MIME mapping and magic-byte checks before storage. |
| V5.2.4 | `MalwareScanner`, `LocalMalwareScanner`, `FileStorageService.scanFileOrReject`, `ReportControllerAttachmentUploadTest.eicarUploadIsRejectedQuarantinedAndAudited` | Uploads pass through a mockable scanner. The local implementation detects EICAR for deterministic test evidence. Production deployments should replace it with a real AV adapter. |
| V5.4.3 | `FileStorageService.quarantineRejectedFile`, `SecurityMonitoringService.recordMalwareUploadRejected`, `ReportService.recordUploadRejected` | Scanner-rejected files are not persisted as attachments, are copied into `quarantine/reports/{id}`, and generate security/audit evidence without exposing raw filenames or paths. |
| V3.2.1 | `ReportService.secureDownloadResponse`, `ReportControllerAttachmentUploadTest.publicDownloadReturnsSecureHeaders` | File downloads include `Content-Disposition: attachment`, `X-Content-Type-Options: nosniff` and no-store/no-cache headers. |

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
| JUnit/MockMvc | Maven `verify` runs the automated tests and the workflow uploads Surefire reports. | `ci-surefire-test-reports`, `ghostreport/target/surefire-reports` | Blocking test evidence. |
| JaCoCo | Coverage report and coverage check run during Maven `verify` in `build-test`. | `ci-jacoco-coverage-report`, `ghostreport/target/site/jacoco`, `ghostreport/target/jacoco.exec` | Blocking coverage evidence. |
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
