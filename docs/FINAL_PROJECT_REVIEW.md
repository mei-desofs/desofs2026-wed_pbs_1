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
- Added regression tests for admin user-management authorization, navbar visibility before login and admin MFA.
- Added mandatory code-based MFA for `ADMIN` users.

## Security And Authorization Findings

- Sensitive API routes are protected server-side in `SecurityConfig`:
  - `/admin/**` requires `ADMIN`;
  - `/analyst/**` requires `ANALYST` or `ADMIN`;
  - `/audit/**` requires `AUDITOR` or `ADMIN`.
- Static panel pages (`/admin.html`, `/analyst.html`, `/auditor.html`) are intentionally public because they host the login forms. Internal data and actions are loaded only through protected APIs after successful login.
- The observed pre-login navbar issue was a frontend CSS cascade bug, not a backend authorization bypass. The backend still returned `401` or `403` for direct API access without the required role.
- Analyst object-level access is enforced in services and covered by existing RBAC/ownership tests.
- Admin user DTOs do not expose password hashes.

## Admin MFA

MFA is implemented for `ADMIN` users when `ghostreport.mfa.enabled=true` and `ghostreport.mfa.admin-required=true`.

Flow:

- ADMIN submits username/password.
- After a correct password, the backend creates a short-lived MFA challenge and does not issue a JWT yet.
- The admin panel shows the MFA form and keeps internal navigation hidden while MFA is pending.
- `/auth/mfa/verify` validates the challenge code and only then returns the final JWT.
- Codes expire, are hashed in memory and are invalidated after successful use.
- Invalid, expired and successful MFA attempts are audit logged.

Configuration:

```yaml
ghostreport:
  mfa:
    enabled: true
    admin-required: true
    code-ttl-seconds: 300
    expose-code: false
```

In `dev`, `expose-code` defaults to `true` so the academic demo can display the code in the admin MFA form and log it. In production, `expose-code` remains `false`; delivery by email/SMS or an IdP integration is still a future operational enhancement.

## Remaining Gaps And Risks

- MFA is code-based and in-memory. A server restart invalidates pending MFA challenges, which is acceptable for this academic implementation.
- MFA delivery is not integrated with email/SMS; dev mode exposes the code for demonstration only.
- Static panel pages are visible to unauthenticated users by design; they must not contain sensitive data. This should remain true in future UI work.
- Admin user editing does not change passwords. Password change/reset is handled by the auth/password-reset flows.
- A `USER` role exists for authenticated basic users, but the reporter journey remains anonymous/tracking-code based.
- Audit/security endpoints currently return full lists without pagination; this may need pagination before production use.
- Some frontend auth state is in memory only. A refresh logs the operator out, which is safe but not a persistent-session UX.

## Test Commands

From `ghostreport/`:

```powershell
.\mvnw.cmd "-Dspring-boot.run.profiles=dev" spring-boot:run
.\mvnw.cmd "-Dtest=AdminUserManagementSecurityTest,FrontendNavbarVisibilityTest" test
.\mvnw.cmd test
```
