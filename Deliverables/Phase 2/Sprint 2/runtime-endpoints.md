# Runtime endpoints exercised by CI

This file mirrors the purpose of the generated CI artifact
`target/iast-evidence/runtime-endpoints.md`. The exact HTTP status values are
produced during each `dast-scan` run by
`.github/scripts/runtime_security_probe.py`.

## Live probes

| Endpoint/probe | Runtime purpose | Expected control |
| --- | --- | --- |
| `GET /index.html` | Public frontend availability and security headers. | Public page works without authentication. |
| `GET /login.html` | Unauthenticated frontend request. | Public/static route handling. |
| `GET /admin/users` without token | Protected endpoint access. | Unauthorized response. |
| `GET /admin/users` with invalid JWT | Invalid token handling. | Controlled unauthorized response and security alert evidence. |
| `POST /auth/login` invalid credentials | Failed login. | Generic failure, audit log and rate-limit accounting. |
| repeated `POST /auth/login` invalid credentials | Brute-force evidence. | Rate limiting/security alert evidence where threshold is reached. |
| `POST /auth/password/change` without CSRF/auth | Protected state-changing request. | Rejection without sensitive details. |
| `POST /reports` valid payload | Public anonymous report flow. | Report and tracking code generated. |
| `POST /reports` invalid payload | Required fields and validation. | Controlled validation error. |
| `POST /reports` dangerous characters | XSS-style characters in data. | Data accepted/validated as text, not executed. |
| `POST /reports` mass-assignment attempt | Extra JSON fields such as `role`, `status`, `id`. | DTO binding ignores unauthorised fields. |
| `POST /reports/verify` valid tracking code | Tracking flow. | Report status returned without internal secrets. |
| `POST /reports/verify` invalid tracking code | Enumeration resistance. | Controlled error. |
| repeated invalid `POST /reports/verify` | Tracking abuse. | Rate-limit/enumeration evidence. |
| `POST /reports/{id}/attachments` allowed file | Upload happy path. | Allowed type stored under generated name. |
| `POST /reports/{id}/attachments` forbidden extension | Extension allowlist. | Rejection. |
| `POST /reports/{id}/attachments` suspicious content | MIME/signature validation. | Rejection. |
| `POST /reports/{id}/attachments` traversal filename | Filename/path validation. | Rejection or safe handling. |
| `POST /auth/login` admin/analyst/auditor | Real internal login starts MFA. | Password accepted but JWT not issued before MFA. |
| `POST /auth/mfa/verify` invalid code | MFA rejection. | Controlled rejection. |
| `POST /auth/mfa/verify` valid dev code | MFA completion. | JWT issued for role. |
| `GET /admin/users` with admin JWT | Admin authorization. | Access allowed. |
| `GET /admin/users` with analyst JWT | Analyst boundary. | Access denied. |
| `GET /admin/users` with auditor JWT | Auditor boundary. | Access denied. |
| `GET /analyst/panel` with analyst JWT | Analyst authorization. | Access allowed. |
| `GET /analyst/panel` with auditor JWT | Auditor boundary. | Access denied. |
| `GET /audit/logs` with auditor JWT | Auditor authorization. | Access allowed. |
| `GET /audit/logs` with analyst JWT | Analyst boundary. | Access denied. |
| `GET /definitely-not-a-real-endpoint` | 404/error handling. | No stack trace or framework details expected. |

## Covered by runtime-focused tests

Some flows remain better validated through MockMvc/JUnit than live probes:

- expired JWT;
- backup creation/verify/tampering/path rejection;
- ZIP Slip-style archive tampering;
- max upload size.

These are included in the `dast-scan` Maven test subset and therefore remain
part of the runtime security evidence artifact.
