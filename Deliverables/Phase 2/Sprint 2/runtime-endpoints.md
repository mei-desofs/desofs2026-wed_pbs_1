# Runtime endpoints exercised by CI

This file documents the intended shape of the generated CI artifact
`target/iast-evidence/runtime-endpoints.md`. Exact IDs, backup filenames and
HTTP statuses can vary per run, so the CI artifact remains the run-specific
source.

The expanded local validation on 2026-06-15 produced:

| Metric | Value |
| --- | --- |
| Total probes | 101 |
| Passed | 99 |
| Failed | 0 |
| Skipped | 2 |
| Public endpoint probes | 22 |
| Admin endpoint probes | 21 |
| Analyst endpoint probes | 17 |
| Auditor endpoint probes | 13 |
| Negative-case probes | 6 |

Skipped probes:

- `GET /login.html`: no separate public `login.html` exists; role pages contain
  their own login forms.
- `POST /admin/backups/{filename}/restore`: not exercised destructively by the
  runtime probe. Access and validation are covered; restore staging is covered
  by automated tests.

## Generated artifact format

The generated table uses this schema:

| Area | Endpoint/Probe | Method | Role used | Expected result | Obtained result | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |

## Public pages

The probe checks:

- `GET /`
- `GET /index.html`
- `GET /submit.html`
- `GET /track.html`
- `GET /login.html` when present
- `GET /admin.html`
- `GET /analyst.html`
- `GET /auditor.html`

Expected evidence:

- public pages respond without authentication when they exist;
- browser security headers are present;
- no obvious bearer token, JWT secret, backup secret or real tracking code is
  exposed in public HTML.

## Public report and file flow

The probe exercises:

- `POST /reports` valid payload;
- `POST /reports` invalid payload;
- `POST /reports` with script-like content;
- `POST /reports` mass-assignment attempt with `role`, `status`, `id`,
  `assignedAnalyst` and forced tracking fields;
- `POST /reports/verify` valid tracking code;
- `POST /reports/verify` invalid and repeated invalid tracking code;
- `POST /reports/{id}/attachments` allowed PDF;
- upload with forbidden extension;
- upload with traversal filename;
- upload with MIME/signature mismatch;
- `POST /reports/{id}/attachments/list`;
- `POST /reports/download` valid tracking code;
- download with invalid tracking code;
- download with invalid attachment id.

## Authentication and MFA

The probe exercises:

- invalid login;
- repeated invalid login for rate-limit/brute-force evidence;
- valid password login for `ADMIN`, `ANALYST` and `AUDITOR`;
- MFA invalid code rejection;
- MFA valid dev/test code completion;
- MFA challenge reuse rejection;
- password change rejection with wrong current password;
- logout without token;
- logout with a valid token after role-specific probes;
- password reset request generic response;
- password reset confirm with invalid token.

MFA codes are read only from dev/test logs. Tokens, tracking codes and MFA
challenge IDs are redacted from JSON artifacts.

## Protected role coverage

Admin probes include:

- unauthenticated and invalid-JWT access to admin endpoints;
- `ANALYST` and `AUDITOR` denied from `/admin/users`;
- `ADMIN` access to panel, users, audit logs, security alerts and backups;
- create/update/activate/deactivate/delete internal user;
- invalid role validation;
- create/list/download/verify backup;
- invalid backup filename validation;
- last-admin deactivation protection when applicable.

Analyst probes include:

- unauthenticated access denied;
- `AUDITOR` denied;
- `ANALYST` access to panel, reports and my-cases;
- assign report;
- invalid status/priority/notes;
- valid priority and notes when workflow permits;
- case-review, internal attachment list/download and case-package request;
- nonexistent case-review controlled error.

Auditor probes include:

- unauthenticated access denied;
- `ANALYST` denied;
- `AUDITOR` access to logs, security alerts, closed cases and backups;
- evidence-package verification for runtime/nonexistent reports;
- invalid backup filename validation;
- backup verify/manifest when a backup was created by the admin probe.

## Cross-cutting negative probes

The probe also checks:

- unknown endpoint;
- wrong HTTP method;
- malformed JSON;
- wrong `Content-Type`;
- malformed `Authorization` header;
- invalid JWT.

These checks verify controlled 4xx responses without stack traces or obvious
internal framework details.
