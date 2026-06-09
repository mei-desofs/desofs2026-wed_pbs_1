# Runtime Security Evidence and IAST-like Testing

GhostReport produces runtime security evidence in the `dast-scan / dast-scan`
job of the main GitHub Actions workflow:

```text
.github/workflows/dev.yml
```

The project does not claim full agent-based IAST. For the DESOFS academic
requirement, GhostReport uses an IAST-like runtime security testing approach:
security-focused Spring Boot tests run against the application runtime, the
application is started as a packaged JAR, selected endpoints are exercised, and
OWASP ZAP baseline scans the live HTTP surface.

## Architecture

```text
GitHub Actions dev workflow
  -> dast-scan / dast-scan
  -> Maven security-focused tests
  -> Spring Boot application context / MockMvc runtime
  -> AuditLogService and SecurityMonitoringService
  -> packaged Spring Boot JAR on localhost:8081
  -> endpoint smoke traffic
  -> OWASP ZAP baseline traffic
  -> runtime security evidence artifacts
```

## Runtime Coverage

The runtime security evidence job executes tests for:

- successful and failed authentication events;
- invalid/expired JWT handling;
- login rate limiting and brute-force alert generation;
- CSRF rejection and accepted CSRF-protected requests;
- generic error responses without stack traces;
- browser security headers;
- audit/security event sanitization.

## Endpoints Exercised in CI

The workflow records HTTP status evidence for:

| Endpoint | Purpose |
| --- | --- |
| `GET /index.html` | Public frontend availability and security headers |
| `GET /login.html` | Login page exposure and headers |
| `GET /api/reports/track/INVALID-CI-CODE` | Public tracking negative path |
| `GET /api/admin/users` | Protected admin endpoint without token |

ZAP baseline also crawls and passively scans `http://localhost:8081`.

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
| Gate mode | Runtime security tests and app startup are blocking; ZAP is evidence review |

The artifact contains:

- Surefire reports for the security-focused runtime tests;
- `target/iast-evidence/iast-runtime-evidence.md`;
- `target/iast-evidence/runtime-endpoints.md`;
- application startup/runtime log;
- ZAP baseline evidence in the separate `dast-zap-baseline-reports` artifact.

## Scope Boundaries

This approach is intentionally described as runtime security testing or
IAST-like evidence. It does not attach a JVM taint-tracking sensor, does not
provide data-flow tracing from source to sink, and does not replace a commercial
or dedicated open-source IAST platform. Findings are interpreted together with
SAST, SCA, SBOM and DAST evidence.
