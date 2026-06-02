# Secure Installation and Configuration

This guide documents how to run GhostReport with security-relevant configuration aligned with the current implementation. It intentionally avoids real secrets and distinguishes local development from production-like execution.

## Runtime Profiles

| Profile | Purpose | Security posture |
|---|---|---|
| `dev` | Local development with PostgreSQL defaults and optional seed users. | Allows local fallback values, including a development-only JWT secret. |
| `test` | Automated tests with H2, isolated upload/backup directories and test JWT secret. | Only for automated tests. |
| default / `prod` | Production-like execution. | Requires environment variables for DB and JWT secret. Seed users are disabled. |

The default `application.yaml` is production-like. If no profile is active, the application requires `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and `JWT_SECRET`.

## Required Environment Variables

| Variable | Required in production-like mode | Description |
|---|---:|---|
| `DB_URL` | Yes | JDBC URL for PostgreSQL. |
| `DB_USERNAME` | Yes | Database user. |
| `DB_PASSWORD` | Yes | Database password. |
| `JWT_SECRET` | Yes | HMAC signing secret for JWT tokens. Must be at least 32 characters. |
| `JWT_EXPIRATION_SECONDS` | No | Token lifetime. Defaults to `3600`. |

Use `.env.example` as a template, but never commit real values.

## Local Development Startup

PowerShell example:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:DB_URL="jdbc:postgresql://localhost:5432/ghostreport"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="user"
$env:JWT_SECRET="dev-local-secret-with-at-least-32-chars"

.\mvnw.cmd spring-boot:run
```

Expected startup line:

```text
The following 1 profile is active: "dev"
```

## Production-Like Startup

In production-like mode, do not rely on development fallbacks:

```powershell
$env:DB_URL="jdbc:postgresql://prod-db:5432/ghostreport"
$env:DB_USERNAME="ghostreport_app"
$env:DB_PASSWORD="<from-secret-manager>"
$env:JWT_SECRET="<random-secret-at-least-32-characters>"
$env:JWT_EXPIRATION_SECONDS="3600"

.\mvnw.cmd spring-boot:run
```

The application should fail during startup if critical variables are missing or unsafe.

## JWT Configuration

JWT tokens are signed using HMAC SHA-256. The secret must be unique per environment and must not be reused between development, CI and production. The code validates minimum secret length, and the production-like configuration requires the value to be provided externally.

## Database Configuration

The production-like profile uses PostgreSQL and `ddl-auto=validate`. Schema changes must therefore be handled deliberately rather than generated implicitly at runtime. The development profile uses `ddl-auto=update` for easier local iteration.

## Seed Users

Seed users are restricted to `dev` and `test` profiles through `DataInitializer`. In production-like execution, seed users are disabled by default and must not be used for operational accounts.

Development seed accounts are only for local testing:

| Username | Role |
|---|---|
| `admin` | `ADMIN` |
| `analyst` | `ANALYST` |
| `auditor` | `AUDITOR` |

## Upload Configuration

Uploads are stored under `app.upload-dir`. The application validates file size, extension, MIME type, magic bytes and normalized paths. The current multipart limit is 10 MB per file/request.

Operational guidance:

- Keep upload storage outside source-controlled directories.
- Ensure the application user has only the filesystem permissions required for upload storage.
- Do not claim antivirus or malware scanning unless a real scanner is integrated.

## Backup Configuration

Backups are stored under `ghostreport.backup-dir`. The backup service rejects identical upload and backup directories and validates backup filenames/paths before reading or restoring metadata.

Operational guidance:

- Keep backups outside the upload directory.
- Protect backup storage with OS-level access controls.
- Treat backup ZIP files as sensitive evidence.
- Validate backup manifests before using them as evidence.

## Error Handling and Logs

Stack traces are disabled in application configuration. Runtime audit and security events must not include passwords, JWTs, raw secrets or full sensitive payloads. Security alerts are operational evidence, not tamper-proof logs.

## Security Configuration Checklist

| Check | Expected production-like value |
|---|---|
| Active profile | default/prod with external env vars |
| `JWT_SECRET` | External, unique, at least 32 chars |
| Seed users | Disabled |
| `ddl-auto` | `validate` |
| Stack traces | Disabled |
| Upload dir | Controlled, not source-controlled |
| Backup dir | Separate from upload dir |
| Rate limits | Positive values |
