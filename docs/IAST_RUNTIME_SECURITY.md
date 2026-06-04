# Runtime Security Evidence and IAST Readiness

GhostReport includes a runtime security evidence stage that exercises the
application while security-sensitive code paths are active. The workflow is
implemented in `.github/workflows/iast-runtime.yml`.

## Architecture

```text
GitHub Actions
  -> Maven security-focused tests
  -> Spring Boot application context / MockMvc runtime
  -> AuditLogService and SecurityMonitoringService
  -> Surefire reports and runtime security evidence artifact
  -> optional Contrast Java Agent telemetry when configured
```

The default workflow executes runtime security tests and publishes evidence.
For environments with an IAST tenant, the workflow documents the required
Contrast Java agent configuration variables so the application can be run with a
JVM `-javaagent` and external IAST telemetry.

## Runtime Coverage

The runtime security evidence workflow executes tests for:

- successful and failed authentication events;
- invalid/expired JWT handling;
- login rate limiting and brute-force alert generation;
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

Workflow:

```text
.github/workflows/iast-runtime.yml
```

Artifact:

```text
iast-runtime-security-evidence
```

The artifact contains:

- Surefire reports for the security-focused runtime tests;
- `target/iast-evidence/iast-runtime-evidence.md`;
- readiness notes for external IAST agent telemetry.

## Optional Contrast Java Agent Integration

The selected compatible IAST agent for a Java/Spring Boot runtime is Contrast
Java Agent because it supports JVM instrumentation through `-javaagent` and can
observe application behavior while tests or runtime traffic exercise the app.

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
