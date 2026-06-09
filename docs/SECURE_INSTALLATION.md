# Secure Installation and Configuration

This guide documents how to run GhostReport with security-relevant configuration aligned with the current implementation. It intentionally avoids real secrets and distinguishes local development from production-like execution.

## Runtime Profiles

| Profile | Purpose | Security posture |
|---|---|---|
| `dev` | Local development with PostgreSQL defaults and optional seed users. | Allows local fallback values, including a development-only JWT secret. |
| `test` | Automated tests with H2, isolated upload/backup directories and test JWT secret. | Only for automated tests. |
| default / `prod` | Production-like execution. | Requires environment variables for DB, JWT secret and backup HMAC secret. Seed users are disabled. |

The default `application.yaml` is production-like. If no profile is active, the application requires `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` and `BACKUP_HMAC_SECRET`.

## Required Environment Variables

| Variable | Required in production-like mode | Description |
|---|---:|---|
| `DB_URL` | Yes | JDBC URL for PostgreSQL. |
| `DB_USERNAME` | Yes | Database user. |
| `DB_PASSWORD` | Yes | Database password. |
| `JWT_SECRET` | Yes | HMAC signing secret for JWT tokens. Must be at least 32 characters. |
| `BACKUP_HMAC_SECRET` | Yes | Separate HMAC signing secret for backup manifests. Must be at least 32 characters and must not equal `JWT_SECRET`. |
| `BACKUP_HMAC_KEY_ID` | No | Logical key identifier written into backup signature metadata. Defaults to `backup-hmac-v1`. |
| `JWT_ACTIVE_KEY_ID` | No | Identifier written to the JWT `kid` header for newly issued tokens. Defaults to `primary`. |
| `JWT_PREVIOUS_SECRETS` | No | Comma-separated validation-only rotation keys in `kid:secret` format. |
| `JWT_EXPIRATION_SECONDS` | No | Token lifetime. Defaults to `3600`. |
| `PASSWORD_RESET_TOKEN_TTL_MINUTES` | No | Password reset token lifetime. Defaults to `30`. |
| `PASSWORD_RESET_EXPOSE_TOKEN` | No | Development/test-only helper to expose reset tokens in API responses. Defaults to `false`; keep disabled outside controlled demos/tests. |
| `APP_UPLOAD_MAX_FILES_PER_REQUEST` | No | Maximum number of files accepted in a single public upload request. Defaults to `5`. |

Use `.env.example` as a template, but never commit real values.

## Local Development Startup

PowerShell example:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:DB_URL="jdbc:postgresql://localhost:5432/ghostreport"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="user"
$env:JWT_SECRET="dev-local-secret-with-at-least-32-chars"
$env:BACKUP_HMAC_SECRET="dev-local-backup-hmac-secret-32-chars"
$env:JWT_ACTIVE_KEY_ID="dev-key"

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
$env:BACKUP_HMAC_SECRET="<different-random-secret-at-least-32-characters>"
$env:BACKUP_HMAC_KEY_ID="prod-backup-hmac-2026-06"
$env:JWT_ACTIVE_KEY_ID="prod-2026-06"
$env:JWT_EXPIRATION_SECONDS="3600"

.\mvnw.cmd spring-boot:run
```

The application should fail during startup if critical variables are not provided or are unsafe.

## Docker Local Startup

The repository includes a local Docker setup:

```powershell
$env:DB_PASSWORD="<local-database-password>"
$env:JWT_SECRET="<random-secret-at-least-32-characters>"
$env:BACKUP_HMAC_SECRET="<different-random-secret-at-least-32-characters>"
$env:JWT_ACTIVE_KEY_ID="local-key"
docker compose up --build
```

The Docker image is built in two stages and runs the application as a non-root user. The compose file starts PostgreSQL 16, stores uploads/backups in named volumes and applies `no-new-privileges`.

For local containerized execution, the provided compose setup defaults to the `dev` profile with seed users disabled. A production deployment should use the default/`prod` profile after the database schema has been provisioned through the team's operational migration process.

## JWT Configuration

JWT tokens are signed using HMAC SHA-256. The active secret must be unique per environment and must not be reused between development, CI and production. The code validates minimum secret length, validates token `issuer`, `audience`, `expiry`, `jti`, signature and `kid`, and the production-like configuration requires the active secret to be provided externally.

New tokens include the active key identifier in the JWT header:

```text
JWT_ACTIVE_KEY_ID=prod-2026-06
JWT_SECRET=<new-random-secret-at-least-32-characters>
```

During key rotation, keep previous keys only for validation until all tokens signed with them have expired:

```text
JWT_ACTIVE_KEY_ID=prod-2026-07
JWT_SECRET=<new-july-secret-at-least-32-characters>
JWT_PREVIOUS_SECRETS=prod-2026-06:<previous-june-secret-at-least-32-characters>
```

Remove previous keys after the maximum JWT lifetime has elapsed. Previous keys are never used to issue new tokens.

## Cryptographic Key Lifecycle

GhostReport currently uses separate symmetric secrets for separate purposes:

| Key | Purpose | Configuration | Rotation guidance |
|---|---|---|---|
| JWT HMAC key | Signs stateless API tokens. | `JWT_SECRET` / `ghostreport.jwt.secret` | Rotate by deploying a new secret and allowing old short-lived tokens to expire. |
| Backup manifest HMAC key | Authenticates `manifest.json` inside backup ZIP files. | `BACKUP_HMAC_SECRET` / `ghostreport.backup.hmac-secret` | Rotate by changing the secret and increasing `BACKUP_HMAC_KEY_ID`; keep the previous key available only while old backups still need validation. |

The application validates that both secrets are at least 32 characters and that
the backup HMAC secret is not reused as the JWT secret. Production-like profiles
reject development/test placeholder values. Real secrets should be generated
with a cryptographically secure random generator and stored in an external
secret manager or deployment secret store, not in Git.

## Password Reset Configuration

Password reset tokens are generated with `SecureRandom`, stored only as SHA-256
hashes, expire after `PASSWORD_RESET_TOKEN_TTL_MINUTES` and are invalidated
after first use. The public reset request endpoint returns a generic response
to avoid account enumeration.

`PASSWORD_RESET_EXPOSE_TOKEN` exists only to support local academic testing or
controlled demonstrations where no email/SMS delivery provider is configured.
It must remain disabled in production-like environments.

## Database Configuration

The production-like profile uses PostgreSQL and `ddl-auto=validate`. Schema changes must therefore be handled deliberately rather than generated implicitly at runtime. The development profile uses `ddl-auto=update` for easier local iteration. The current repository does not include a formal Flyway/Liquibase migration chain; use `docs/PRODUCTION_SCHEMA_CHECKLIST.md` as the deployment checklist until versioned migrations are added.

JWT logout/replay protection requires a persistent revocation table. Provision the equivalent schema before running production-like profiles:

```sql
CREATE TABLE revoked_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_id VARCHAR(80) NOT NULL,
    subject VARCHAR(120) NOT NULL,
    key_id VARCHAR(80) NOT NULL,
    revoked_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX idx_revoked_tokens_jti ON revoked_tokens (token_id);
CREATE INDEX idx_revoked_tokens_expires_at ON revoked_tokens (expires_at);
```

## Seed Users

Seed users are restricted to `dev` and `test` profiles through `DataInitializer`. In production-like execution, seed users are disabled by default and must not be used for operational accounts.

Development seed accounts are only for local testing:

| Username | Role |
|---|---|
| `admin` | `ADMIN` |
| `analyst` | `ANALYST` |
| `auditor` | `AUDITOR` |

## Upload Configuration

Uploads are stored under `app.upload-dir`. The application validates file size, extension, MIME type, magic bytes, normalized paths and the maximum number of files per request. The current multipart limit is 10 MB per file/request and the default application-level upload count limit is 5 files per request.

Uploaded attachments are scanned through the `MalwareScanner` interface before they are persisted as report attachments. The default local implementation detects the EICAR test signature so automated tests can prove the scanner control path. Files rejected by the scanner are copied to `app.upload-dir/quarantine/reports/{reportId}` and are not stored in the attachment repository.

Operational guidance:

- Keep upload storage outside source-controlled directories.
- Ensure the application user has only the filesystem permissions required for upload storage.
- Replace the local scanner with a real antivirus adapter before claiming production malware detection coverage.
- Define retention, review and deletion procedures for quarantined files.

## Backup Configuration

Backups are stored under `ghostreport.backup-dir`. The backup service rejects identical upload and backup directories and validates backup filenames/paths before reading or restoring metadata.

Each backup ZIP contains:

- data files and uploaded evidence files;
- `manifest.json` with file paths, sizes and SHA-256 hashes;
- `manifest.hmac.json` with an HMAC-SHA256 signature of `manifest.json`, the algorithm and the configured key id;
- a sidecar `.sha256` file for the full ZIP hash.

Before verification or restore, GhostReport validates the manifest HMAC, checks
each file hash and rejects unsigned extra ZIP entries. A backup with a modified
manifest, modified content or added unsigned file is rejected before restore.

Operational guidance:

- Keep backups outside the upload directory.
- Protect backup storage with OS-level access controls.
- Treat backup ZIP files as sensitive evidence.
- Validate backup manifests before using them as evidence.
- Keep `BACKUP_HMAC_SECRET` separate from `JWT_SECRET`.
- Rotate backup HMAC keys deliberately and record the `BACKUP_HMAC_KEY_ID` used for each backup generation period.
- Backup ZIP encryption is not implemented in the current coursework scope; if confidentiality of stored backups is required, use encrypted storage/volumes or add application-level backup encryption as future hardening.

## Error Handling and Logs

Stack traces are disabled in application configuration. Runtime audit and
security events include a correlation ID, UTC timestamp and SHA-256 integrity
hash. Event details are sanitized to redact passwords, bearer tokens, reset
tokens, authorization values and tracking codes.

Security alerts are operational evidence. The application provides
tamper-evident fields inside the database record, but it does not provide
external WORM storage, SIEM forwarding or automated retention. Production
deployments should export logs/events to protected centralized storage and
define retention, access review and incident response procedures.

## Security Configuration Checklist

| Check | Expected production-like value |
|---|---|
| Active profile | default/prod with external env vars |
| `JWT_SECRET` | External, unique, at least 32 chars |
| `BACKUP_HMAC_SECRET` | External, unique, at least 32 chars, different from `JWT_SECRET` |
| `BACKUP_HMAC_KEY_ID` | Non-blank logical key id |
| `JWT_ACTIVE_KEY_ID` | Stable key identifier for the active secret |
| `JWT_PREVIOUS_SECRETS` | Only during rotation, removed after max token lifetime |
| `PASSWORD_RESET_EXPOSE_TOKEN` | `false` |
| Seed users | Disabled |
| `ddl-auto` | `validate` |
| Stack traces | Disabled |
| Upload dir | Controlled, not source-controlled |
| Backup dir | Separate from upload dir |
| Rate limits | Positive values |
| Log storage | Protected database plus external SIEM/WORM storage for production |
