# Security Configuration Assessment

This assessment documents the configuration controls used by GhostReport across
local development, automated tests and production-like execution.

## Configuration Evidence

| Area | Evidence | Status |
| --- | --- | --- |
| Runtime profiles | `application.yaml`, `application-dev.yaml`, `application-test.yaml` | Implemented |
| Production-like configuration | Default profile requires database and JWT environment variables | Implemented |
| JWT secret validation | `SecurityConfigurationValidator`, `JwtService`, validator tests | Implemented |
| Backup HMAC key validation | `SecurityConfigurationValidator`, `BackupService`, backup/config tests | Implemented |
| Seed users | Disabled by default and controlled by profile/validator | Implemented |
| Database configuration | PostgreSQL default/dev, H2 test profile, `ddl-auto=validate` in production-like config | Implemented |
| H2 console | Disabled outside the test profile | Implemented |
| Error output | `server.error.include-stacktrace=never` | Implemented |
| Upload limits | Multipart limit and max files per request | Implemented |
| Backup/upload separation | Validator rejects identical normalized directories | Implemented |
| Docker hardening | Non-root user, read-only app container, named volumes and `no-new-privileges` | Implemented |
| Secret scanning | `.gitleaks.toml` and Gitleaks workflow | Implemented |

## Fail-Fast Checks

The application validates configuration at startup:

- `JWT_SECRET` must be at least 32 characters.
- `BACKUP_HMAC_SECRET` must be at least 32 characters.
- `BACKUP_HMAC_SECRET` must be different from `JWT_SECRET`.
- `BACKUP_HMAC_KEY_ID` must not be blank.
- `JWT_EXPIRATION_SECONDS` must be positive.
- Upload and backup directories must be configured and distinct.
- Production-like profiles must not use dev/test JWT placeholders.
- Production-like profiles must not use dev/test backup HMAC placeholders.
- Production-like profiles must not enable seed users.

Evidence: `SecurityConfigurationValidatorTest`.

## Environment Variables

| Variable | Purpose |
| --- | --- |
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | JWT HMAC signing secret |
| `BACKUP_HMAC_SECRET` | Backup manifest HMAC signing secret |
| `BACKUP_HMAC_KEY_ID` | Backup manifest HMAC key identifier |
| `JWT_EXPIRATION_SECONDS` | Token lifetime |
| `APP_UPLOAD_MAX_FILES_PER_REQUEST` | Upload abuse-control limit |

Use `.env.example` as a local template. Real secrets are supplied through the
runtime environment or GitHub Actions secrets.

## Operational Notes

- The default profile is production-like and expects external configuration.
- The `dev` profile supports local iteration with PostgreSQL defaults.
- The `test` profile uses isolated H2 databases and test storage paths.
- Docker Compose is configured for local containerized execution.
