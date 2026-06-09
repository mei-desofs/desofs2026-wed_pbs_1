# Runtime Security Evidence and IAST Readiness

GhostReport produces runtime security evidence in the `dast-scan / dast-scan`
job of the main GitHub Actions workflow:

```text
.github/workflows/dev.yml
```

This is not presented as complete IAST by default. The project always produces
runtime security evidence through automated tests. Complete external IAST
telemetry exists only when the Contrast Java agent is configured, downloaded,
attached to the running Spring Boot application with `-javaagent`, and the
runtime tests/ZAP traffic exercise that instrumented application.

## Architecture

```text
GitHub Actions dev workflow
  -> dast-scan / dast-scan
  -> Maven security-focused tests
  -> Spring Boot application context / MockMvc runtime
  -> AuditLogService and SecurityMonitoringService
  -> Surefire reports and runtime security evidence artifact
  -> optional Contrast Java Agent download
  -> instrumented Spring Boot JAR with -javaagent
  -> ZAP traffic against the instrumented runtime
  -> Contrast platform telemetry
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
| Gate mode | Runtime security tests and app startup are blocking; external IAST telemetry is conditional on Contrast configuration |

The artifact contains:

- Surefire reports for the security-focused runtime tests;
- `target/iast-evidence/iast-runtime-evidence.md`;
- whether the Contrast Java agent was attached;
- ZAP/runtime traffic evidence for the instrumented application.

## Optional Contrast Java Agent Integration

To enable complete external IAST telemetry in GitHub Actions:

1. Create or select a Contrast application for GhostReport.
2. Add repository variables:
   - `CONTRAST__API__URL`
   - `CONTRAST__APPLICATION__NAME` (optional; defaults to `GhostReport-CI`)
   - `CONTRAST__SERVER__NAME` (optional; defaults to `github-actions`)
   - `CONTRAST__SERVER__ENVIRONMENT` (optional; defaults to `qa`)
   - `CONTRAST_AGENT_URL` (optional; defaults to `https://download.java.contrastsecurity.com/latest`)
3. Add repository secrets:
   - `CONTRAST__API__API_KEY`
   - `CONTRAST__API__SERVICE_KEY`
   - `CONTRAST__API__USER_NAME`
4. Re-run the `dev` workflow.
5. Open the `dast-scan / dast-scan` job and verify that
   `iast-runtime-security-evidence` says `Contrast Java agent: enabled and
   attached with -javaagent`.
6. Confirm the application appears in Contrast with findings/route telemetry
   from the runtime tests and ZAP baseline scan.

The current repository keeps these values outside source control and expects
them to be supplied as GitHub Actions variables/secrets.

## Scope Boundaries

The repository always produces local runtime security evidence through
automated tests. It does not claim complete IAST telemetry unless the optional
Contrast integration is configured with a tenant, valid credentials and an
instrumented runtime execution.
