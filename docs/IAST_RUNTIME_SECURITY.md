# Runtime Security Evidence and IAST Readiness

GhostReport produces runtime security evidence in the `dast-scan / dast-scan`
job of the main GitHub Actions workflow:

```text
.github/workflows/dev.yml
```

This is not presented as complete IAST by default. The project always produces
runtime security evidence through automated tests. External IAST telemetry is
optional and depends on Contrast Java agent variables/secrets being configured.

## Architecture

```text
GitHub Actions dev workflow
  -> dast-scan / dast-scan
  -> Maven security-focused tests
  -> Spring Boot application context / MockMvc runtime
  -> AuditLogService and SecurityMonitoringService
  -> Surefire reports and runtime security evidence artifact
  -> optional Contrast Java Agent readiness notes
```

## Runtime Coverage

The runtime security evidence job executes tests for:

- successful and failed authentication events;
- invalid/expired JWT handling;
- login rate limiting and brute-force alert generation;
- CSRF security behaviour;
- generic error responses without stack traces;
- browser security headers;
- audit/security event sanitization.

## Monitored Events

| Event | Evidence |
| --- | --- |
| Successful login | `LOGIN_SUCCESS` audit log in runtime tests |
| Failed login | `LOGIN_FAILED` audit log in runtime tests |
| Repeated failed login | `BRUTE_FORCE_LOGIN_ATTEMPT` security alert |
| Invalid JWT | `INVALID_JWT_TOKEN` security alert |
| Generic error handling | Correlation ID and controlled error response |
| Security headers | CSP, frame protection, referrer policy and related headers |

## Pipeline Integration

| Item | Value |
| --- | --- |
| Workflow | `.github/workflows/dev.yml` |
| Job | `dast-scan / dast-scan` |
| Artifact | `iast-runtime-security-evidence` |
| Gate mode | Runtime security tests are blocking; external IAST telemetry is optional |

The artifact contains:

- Surefire reports for the security-focused runtime tests;
- `target/iast-evidence/iast-runtime-evidence.md`;
- readiness notes for optional external IAST agent telemetry.

## Optional Contrast Java Agent Integration

To enable external IAST telemetry in GitHub Actions, configure:

```text
CONTRAST_AGENT_VERSION
CONTRAST__API__URL
CONTRAST__API__API_KEY
CONTRAST__API__SERVICE_KEY
CONTRAST__API__USER_NAME
```

The current repository keeps these values outside source control and expects
them to be supplied as GitHub Actions variables/secrets.

## Scope Boundaries

The repository always produces local runtime security evidence through
automated tests. It does not claim complete IAST telemetry unless the optional
Contrast integration is configured with a tenant, valid credentials and an
instrumented runtime execution.
