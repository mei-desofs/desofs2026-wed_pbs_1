# Authorization Matrix

This document is ASVS evidence for GhostReport authorization controls.

Covered ASVS IDs:

- V8.1.1: access control rules are enforced server-side.
- V8.1.2: authorization decisions are deny-by-default for protected resources.
- V8.2.3: object access is constrained by role and ownership.
- V8.3.2: direct object references are checked before resource access.
- V8.4.2: responses are filtered so roles only receive fields they need.

## Role And Object Matrix

| Endpoint | Method | Allowed role | Forbidden role | Own resource | Foreign resource | Fields visible by role |
| --- | --- | --- | --- | --- | --- | --- |
| `/reports` | `POST` | Public | None | Not applicable | Not applicable | Returns `id`, `status`, one-time `trackingCode`. Does not return `trackingCodeHash`. |
| `/reports/verify` | `POST` | Public with valid tracking code | Public with invalid tracking code | Tracking-code match required | Tracking-code mismatch denied | Returns report status/details for the matching tracking code only. |
| `/reports/{id}/attachments` | `POST` | Public with valid tracking code | Public with invalid tracking code | Report ID and tracking code must match | Mismatched ID/code denied | Returns attachment metadata only. |
| `/reports/{id}/attachments/list` | `POST` | Public with valid tracking code | Public with invalid tracking code | Report ID and tracking code must match | Mismatched ID/code denied | Returns attachment `id`, `originalName`, `mimeType`, `size`; no storage path/hash. |
| `/reports/download` | `POST` | Public with valid tracking code | Public with invalid tracking code | Attachment report and tracking code must match | Mismatched attachment/code denied | Streams file only after object check. |
| `/analyst/reports` | `GET` | `ANALYST`, `ADMIN` | Anonymous, `AUDITOR` | Analysts see own assigned reports | Analysts do not see reports assigned to another analyst | Analysts receive full description only for own assigned reports; unassigned reports have `description: null`. Admin receives full report DTOs. |
| `/analyst/reports/{id}/assign` | `POST` | `ANALYST`, `ADMIN` at route level | Anonymous, `AUDITOR` | Analyst can claim unassigned or own case | Case assigned to another analyst returns conflict | Returns case review assignment metadata. |
| `/analyst/reports/{id}/status` | `PATCH` | Assigned `ANALYST`, `ADMIN` | Anonymous, `AUDITOR` | Assigned analyst can update open case status | Other analysts denied | Returns report DTO for authorized caller only. |
| `/analyst/reports/{id}/priority` | `PATCH` | Assigned `ANALYST`, `ADMIN` | Anonymous, `AUDITOR` | Assigned analyst can update open case priority | Other analysts denied | Returns case review metadata for authorized caller only. |
| `/analyst/reports/{id}/notes` | `PATCH` | Assigned `ANALYST`, `ADMIN` | Anonymous, `AUDITOR` | Assigned analyst can update open case notes | Other analysts denied | Internal notes are returned only to owner/admin. |
| `/analyst/reports/{id}/case-review` | `GET` | Assigned `ANALYST`, `ADMIN` | Anonymous, `AUDITOR` | Assigned analyst can read case review | Other analysts denied | Case review fields only for owner/admin. |
| `/analyst/my-cases` | `GET` | `ANALYST`, `ADMIN` | Anonymous, `AUDITOR` | Analyst receives only cases assigned to current username | Foreign assigned cases excluded | Case review summary for current analyst only. |
| `/analyst/reports/{id}/attachments` | `GET` | Assigned `ANALYST`, `ADMIN` | Anonymous, `AUDITOR` | Assigned analyst can list attachment metadata | Other analysts and unassigned direct reads denied | Returns metadata only; no storage path/hash. |
| `/analyst/attachments/{attachmentId}/download` | `GET` | Assigned `ANALYST`, `ADMIN` | Anonymous, `AUDITOR` | Assigned analyst can download own case attachment | Other analysts denied | Streams file only after attachment-to-report ownership check. |
| `/analyst/reports/{id}/case-package` | `POST` | Assigned `ANALYST`, `ADMIN` | Anonymous, `AUDITOR` | Assigned analyst can generate package for closed own case | Other analysts denied | Returns generated package metadata to owner/admin. |
| `/audit/logs` | `GET` | `AUDITOR`, `ADMIN` | Anonymous, `ANALYST` | Not applicable | Not applicable | Audit log DTO only. |
| `/audit/security-alerts` | `GET` | `AUDITOR`, `ADMIN` | Anonymous, `ANALYST` | Not applicable | Not applicable | Security alert DTO only. |
| `/audit/cases/closed` | `GET` | `AUDITOR`, `ADMIN` | Anonymous, `ANALYST` | Not applicable | Not applicable | Closed-case metadata only; no report description, tracking hash or internal notes. |
| `/audit/cases/{reportId}/evidence-package/verify` | `GET` | `AUDITOR`, `ADMIN` | Anonymous, `ANALYST` | Closed case only | Non-closed/missing package denied | Integrity result omits package path, stored filenames and original filenames. |
| `/admin/users` | `GET`, `POST` | `ADMIN` | Anonymous, `ANALYST`, `AUDITOR` | Admin oversight | Admin oversight | User DTOs omit password hashes; create validates username, email, password and role. |
| `/admin/users/{id}` | `PUT`, `DELETE` | `ADMIN` | Anonymous, `ANALYST`, `AUDITOR` | Admin oversight | Admin oversight | Edit supports username, email, role and active status; delete is logical deactivation. Last active admin cannot be demoted or disabled. |
| `/admin/users/{id}/activate` | `PATCH` | `ADMIN` | Anonymous, `ANALYST`, `AUDITOR` | Admin oversight | Admin oversight | Activates the user and writes an audit log. |
| `/admin/users/{id}/deactivate` | `PATCH` | `ADMIN` | Anonymous, `ANALYST`, `AUDITOR` | Admin oversight | Admin oversight | Deactivates the user and writes an audit log. Last active admin cannot be disabled. |
| `/admin/backups/**` | Various | `ADMIN` | Anonymous, `ANALYST`, `AUDITOR` | Admin oversight | Admin oversight | Admin can create, list, verify, download and stage restores. |
| `/admin/audit-logs`, `/admin/security-alerts` | `GET` | `ADMIN` | Anonymous, `ANALYST`, `AUDITOR` | Admin oversight | Admin oversight | Audit/security DTOs only. |

## Enforcement Points

| Control | Implementation | Test evidence |
| --- | --- | --- |
| Central role routing | `SecurityConfig` protects `/admin/**`, `/analyst/**` and `/audit/**`. | `RbacAuthorizationMatrixTest`, `AdminAuthorizationTest`, `AuditorAuthorizationTest`. |
| Analyst object ownership | `ReportService.checkInternalAccessToReport` and `CaseReviewService.getAccessibleCaseReview` require the authenticated analyst to be assigned to the case. | `AnalystCaseOwnershipTest`, `RbacAuthorizationMatrixTest`. |
| Direct object reference protection | Attachment downloads resolve the attachment, then authorize against the attachment's report before returning content. | `AnalystCaseOwnershipTest`. |
| Field-level filtering | `ReportService.getAllReports` redacts report descriptions from unassigned analyst queue entries; `AuditReadService` returns closed-case metadata only; `UserService` returns `UserResponse` without password hashes. | `AnalystCaseOwnershipTest`, `RbacAuthorizationMatrixTest`, `AuditorAuthorizationTest`, `AdminUserManagementSecurityTest`. |
| Admin lifecycle safety | `UserService.updateUser` and `UserService.setActive` prevent disabling or demoting the last active admin; logical delete maps to deactivation. | `AdminUserManagementSecurityTest`. |
| Security event evidence | Ownership violations create audit logs and security alerts. | `AnalystCaseOwnershipTest`, `RuntimeSecurityEventLoggingTest`. |

## Residual Boundaries

- Unassigned reports remain visible to analysts as queue entries so they can claim work, but sensitive description detail is redacted until assignment.
- Admin is an oversight role and can access analyst/audit views by design.
- Public report access is authorized with the report tracking code instead of a logged-in user identity.
