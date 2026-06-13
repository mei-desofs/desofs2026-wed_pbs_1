# Production Schema Checklist

GhostReport's default production-like configuration uses PostgreSQL with
`spring.jpa.hibernate.ddl-auto=validate`. That means the application validates
an existing schema and does not create or migrate tables automatically.

This checklist documents the schema elements that must exist before a
production-like deployment. The repository includes one idempotent PostgreSQL
schema repair script for legacy audit/security metadata:
`ghostreport/src/main/resources/db/schema/postgresql/001_audit_alert_metadata.sql`.
Before claiming broader production readiness, convert the complete schema into
Flyway or Liquibase migrations and test them against PostgreSQL.

## Current Gap

The repository has JPA entities and tests that generate schemas through H2 or
development `ddl-auto=update`, but it does not include a full formal versioned
migration chain. A persistent PostgreSQL database may still need explicit DDL
for newer fields such as optimistic-lock `version` columns. Audit/security alert
metadata columns are handled by the included PostgreSQL repair script.

## Legacy Audit/Security Metadata Repair

The script `001_audit_alert_metadata.sql` is run through Spring SQL init in
`dev` and production-like PostgreSQL profiles before Hibernate schema
update/validation.

It preserves existing data and applies this order:

1. Add `correlation_id` and `integrity_hash` as nullable columns when missing.
2. Backfill `correlation_id` as `legacy-<id>` for existing rows with no value.
3. Backfill `integrity_hash` with a deterministic 64-character legacy hash
   derived from existing row fields.
4. Apply `NOT NULL` after the backfill.

For a clean database, Hibernate can create the tables normally. For an existing
database with old `audit_logs` or `security_alerts` rows, this script prevents
Hibernate from attempting unsafe `NOT NULL` column additions over null data.

## Tables and Columns to Verify

| Area | Required schema evidence |
| --- | --- |
| Reports | `reports.version BIGINT`, title/description/category/status/tracking hash, created timestamp. |
| Case reviews | `case_reviews.version BIGINT`, unique `report_id`, optional `assigned_analyst_id`, notes, priority, updated timestamp. |
| Audit logs | `audit_logs` with UTC timestamp, correlation ID, actor, action, target fields, details and `integrity_hash VARCHAR(64) NOT NULL`. |
| Security alerts | `security_alerts` with UTC timestamp, correlation ID, alert type, severity, actor, target fields, description and `integrity_hash VARCHAR(64) NOT NULL`. |
| JWT revocation | `revoked_tokens.token_id`, `subject`, `key_id`, `revoked_at`, `expires_at`, unique index on `token_id`, index on `expires_at`. |
| Password reset | `password_reset_tokens.token_hash VARCHAR(64) UNIQUE NOT NULL`, user FK, created/expires/used timestamps. |
| Password history | `password_history` user FK, password hash and created timestamp. |
| Attachments | Stored filename/reference/path, MIME type, size, hash and report FK. |
| Users | Unique username/email, password hash, role check for `ANALYST`, `AUDITOR`, `ADMIN`, active flag and created timestamp. Legacy `USER` rows must be remediated before enforcing the check; the application startup repair converts them to inactive `ANALYST` accounts. |

## Minimum PostgreSQL Checks

Use these checks against a deployed database before starting the app with the
default/production-like profile:

```sql
SELECT column_name
FROM information_schema.columns
WHERE table_name = 'reports' AND column_name = 'version';

SELECT column_name
FROM information_schema.columns
WHERE table_name = 'case_reviews' AND column_name = 'version';

SELECT column_name
FROM information_schema.columns
WHERE table_name = 'audit_logs' AND column_name = 'integrity_hash';

SELECT column_name
FROM information_schema.columns
WHERE table_name = 'security_alerts' AND column_name = 'integrity_hash';

SELECT count(*)
FROM audit_logs
WHERE correlation_id IS NULL OR integrity_hash IS NULL;

SELECT count(*)
FROM security_alerts
WHERE correlation_id IS NULL OR integrity_hash IS NULL;

SELECT indexname
FROM pg_indexes
WHERE tablename = 'revoked_tokens'
  AND indexname IN ('idx_revoked_tokens_jti', 'idx_revoked_tokens_expires_at');
```

## Recommended Next Step

Add Flyway or Liquibase and create a baseline migration from the current JPA
model. After that, keep `ddl-auto=validate` in production-like profiles and let
CI run migration validation against PostgreSQL.
