# Security Configuration Assessment

This assessment focuses on the current branch,
`feature/security-configuration-assessment`, and maps configuration hardening to
code, tests, workflows and residual risks.

## Current State

| Area | Evidence | Status |
| --- | --- | --- |
| Profiles | `application.yaml`, `application-dev.yaml`, `application-test.yaml` | Implemented |
| Production-like config | Default profile requires `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` | Implemented |
| JWT secret validation | `SecurityConfigurationValidator`, `JwtService`, validator tests | Implemented |
| Seed users | Disabled by default; restricted by profile and validator | Implemented |
| Database config | PostgreSQL default/prod with `ddl-auto=validate`; dev uses `update`; test uses H2 | Implemented |
| H2 console | Disabled in default/dev; enabled only for test profile | Acceptable for tests |
| Stack traces | `server.error.include-stacktrace=never` | Implemented |
| Upload limits | Multipart 10 MB and max files per request | Implemented |
| Backup/upload separation | Validator rejects same normalized directory | Implemented |
| Docker hardening | Non-root user, read-only container, named volumes, `no-new-privileges` | Implemented baseline |
| Secret scanning | `.gitleaks.toml`, Gitleaks workflow | Implemented |

## Configuration Risks and Mitigations

| Risk | Current mitigation | Residual risk |
| --- | --- | --- |
| Hardcoded secrets | Production-like profile has no secret defaults; Gitleaks scans repo. | Placeholder values must never be used in real environments. |
| Weak JWT secret | Minimum length checked at startup. | No automated rotation mechanism. |
| Dev settings in production | Validator rejects dev/test JWT secrets and seed users in production-like profiles. | Operators must set the right active profile. |
| Schema drift | Production-like `ddl-auto=validate`. | No Flyway/Liquibase migrations yet. |
| Sensitive error output | Stack traces disabled and exception handler returns generic errors. | Correlation IDs are not yet propagated into structured logs. |
| Unsafe filesystem paths | Upload/backup paths are normalized and separated. | OS-level permissions remain an operational requirement. |
| Multi-instance abuse | In-memory rate limits exist. | Distributed rate limiting is future work. |

## Fail-Fast Checks

The application fails startup when:

- `JWT_SECRET` is missing or shorter than 32 characters.
- `JWT_EXPIRATION_SECONDS` is not positive.
- `app.upload-dir` or `ghostreport.backup-dir` is blank.
- Upload and backup directories resolve to the same path.
- Production-like profiles use dev/test JWT placeholders.
- Production-like profiles enable seed users.

Evidence: `SecurityConfigurationValidatorTest`.

## Presentation Evidence

Show:

- `application.yaml` requiring environment variables.
- `SecurityConfigurationValidator`.
- `SecurityConfigurationValidatorTest`.
- `.env.example` with placeholders only.
- Gitleaks workflow artifact.
- Dockerfile/compose hardening settings.

## Recommended Future Work

- Add Flyway or Liquibase migrations.
- Add structured logging with correlation ID propagation.
- Add secret rotation runbook.
- Replace in-memory rate limiting with Redis for distributed deployment.
- Add production TLS/reverse-proxy deployment documentation.
