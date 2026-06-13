# GhostReport Final Project Review

Review date: 2026-06-13.

## Implemented In This Review

- Fixed authenticated navigation visibility before login. The role-specific navbars for admin, analyst and auditor now remain hidden even when later CSS rules define them as flex containers.
- Completed admin user-management lifecycle support:
  - list users;
  - create users;
  - edit username, email and role;
  - activate users;
  - deactivate users;
  - logical removal through deactivation;
  - protection against deactivating or demoting the last active admin;
  - audit logging for create, update, activate and deactivate actions.
- Added admin UI controls for editing users and changing activation status.
- Added regression tests for admin user-management authorization and navbar visibility before login.

## Security And Authorization Findings

- Sensitive API routes are protected server-side in `SecurityConfig`:
  - `/admin/**` requires `ADMIN`;
  - `/analyst/**` requires `ANALYST` or `ADMIN`;
  - `/audit/**` requires `AUDITOR` or `ADMIN`.
- Static panel pages (`/admin.html`, `/analyst.html`, `/auditor.html`) are intentionally public because they host the login forms. Internal data and actions are loaded only through protected APIs after successful login.
- The observed pre-login navbar issue was a frontend CSS cascade bug, not a backend authorization bypass. The backend still returned `401` or `403` for direct API access without the required role.
- Analyst object-level access is enforced in services and covered by existing RBAC/ownership tests.
- Admin user DTOs do not expose password hashes.

## MFA Feasibility

MFA is not implemented in this delivery.

Current authentication is local username/password with JWT. There is no existing MFA domain model, enrollment flow, recovery flow, authenticator-app/TOTP library integration, email/SMS provider, or external identity provider. Adding MFA safely would require more than a login-screen prompt: administrators need enrollment, backup/recovery handling, secret storage, reset procedures, audit events and tests for bypass/lockout cases.

Recommended future implementation:

- TOTP for `ADMIN` first, then optionally `AUDITOR`.
- Store MFA secrets encrypted at rest or delegate MFA to an IdP.
- Add setup, verify, disable and recovery flows.
- Require MFA completion before issuing a full-privilege JWT or encode an MFA claim and enforce it for privileged routes.
- Add tests for missing code, invalid code, replay/window behavior, disabled users, recovery and role changes.

Email codes are simpler for users, but only make sense if the project adds a reliable mail provider and delivery audit trail. For this codebase, TOTP or an external IdP is the cleaner security direction.

## Remaining Gaps And Risks

- MFA remains a documented future security improvement.
- Static panel pages are visible to unauthenticated users by design; they must not contain sensitive data. This should remain true in future UI work.
- Admin user editing does not change passwords. Password change/reset is handled by the auth/password-reset flows.
- There is no separate `USER` role for ordinary reporters; the public reporter journey is anonymous/tracking-code based.
- Audit/security endpoints currently return full lists without pagination; this may need pagination before production use.
- Some frontend auth state is in memory only. A refresh logs the operator out, which is safe but not a persistent-session UX.

## Test Commands

From `ghostreport/`:

```powershell
.\mvnw.cmd "-Dtest=AdminUserManagementSecurityTest,FrontendNavbarVisibilityTest" test
.\mvnw.cmd test
```
