# IAST and Runtime Security Instrumentation

## Current Status

GhostReport does not currently integrate a dedicated commercial or open-source IAST agent. Therefore the project must not claim full IAST coverage.

The current implementation provides runtime security evidence through:

- Spring Boot integration/security tests that exercise the running application context.
- MockMvc tests for authentication, authorization, uploads, error handling and runtime events.
- JaCoCo runtime instrumentation during test execution.
- OWASP ZAP baseline DAST against a live application instance in CI.
- Application-level audit logs and security alerts for selected runtime events.

This is best described as **runtime security instrumentation and IAST-inspired validation**, not full IAST.

## Runtime Security Events Implemented

| Event | Evidence | Sensitive data handling |
|---|---|---|
| Successful login | `LOGIN_SUCCESS` audit log. | Does not store JWT or password. |
| Failed login | `LOGIN_FAILED` audit log. | Does not store submitted password. |
| Repeated failed login | `BRUTE_FORCE_LOGIN_ATTEMPT` security alert. | Does not store submitted password. |
| Inactive user login | `LOGIN_BLOCKED_INACTIVE_USER` audit log. | Does not store password. |
| Invalid/expired JWT | `INVALID_JWT_TOKEN` security alert. | Does not store raw JWT. |
| Tracking code abuse | `TRACKING_CODE_ENUMERATION` security alert. | Does not store valid tracking code. |
| Rejected upload | `UPLOAD_REJECTED` audit log and `SUSPICIOUS_UPLOAD_ACTIVITY` alert. | Sanitized details; raw malicious filename is not logged. |
| Ownership violation | `ANALYST_ACCESS_DENIED` audit log and `ANALYST_OWNERSHIP_VIOLATION` alert. | Uses report id only. |
| Backup path/integrity issues | Backup-related audit logs and alerts. | Filenames/paths are validated and sanitized. |

## Why a Full IAST Agent Was Not Added Yet

Full IAST requires an agent or instrumentation component running inside the application process and reporting vulnerabilities based on runtime data flows. This usually requires additional setup, licensing decisions, CI secrets, data retention decisions and tuning to avoid noise.

For this sprint, the safer approach is:

1. keep the application stable;
2. avoid overclaiming unsupported tooling;
3. implement runtime security events that are useful and testable;
4. document IAST as evaluated but not fully integrated.

## Future IAST Options

| Option | Notes |
|---|---|
| Contrast Community / Contrast Security agent | Requires account/tooling validation and CI/runtime setup. |
| OpenTelemetry-based security telemetry | Useful for runtime observability, but not a full IAST replacement by itself. |
| Custom security event logging | Already started; useful for assessment evidence, but does not perform taint/data-flow analysis. |

## Correct Report Wording

Use:

> GhostReport does not currently include a dedicated IAST agent. The project implements runtime security instrumentation and IAST-inspired validation through security integration tests, JaCoCo runtime instrumentation, OWASP ZAP DAST and application-level audit/security events.

Avoid:

> GhostReport has full IAST scanning.

or:

> Runtime logs are tamper-proof.
