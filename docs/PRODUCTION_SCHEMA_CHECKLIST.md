# Production Schema Checklist

GhostReport's default production-like configuration uses PostgreSQL with
`spring.jpa.hibernate.ddl-auto=validate`. That means the application validates
an existing schema and does not create or migrate tables automatically.

This checklist documents the schema elements that must exist before a
production-like deployment. It is not an executable migration chain. Before
claiming production readiness, convert this into Flyway or Liquibase migrations
and test them against PostgreSQL.

## Current Gap

The repository has JPA entities and tests that generate schemas through H2 or
development `ddl-auto=update`, but it does not include formal versioned
migrations. A persistent PostgreSQL database may need explicit DDL for newer
fields such as optimistic-lock `version` columns and audit/security alert
tables.

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
| Users | Unique username/email, password hash, role check for `ANALYST`, `AUDITOR`, `ADMIN`, active flag and created timestamp. |

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

SELECT indexname
FROM pg_indexes
WHERE tablename = 'revoked_tokens'
  AND indexname IN ('idx_revoked_tokens_jti', 'idx_revoked_tokens_expires_at');
```

## Recommended Next Step

Add Flyway or Liquibase and create a baseline migration from the current JPA
model. After that, keep `ddl-auto=validate` in production-like profiles and let
CI run migration validation against PostgreSQL.

