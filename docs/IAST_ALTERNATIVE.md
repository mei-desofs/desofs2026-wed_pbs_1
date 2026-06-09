# IAST Alternative for DESOFS

## Decision

GhostReport does not use a commercial external IAST platform because the
project cannot rely on tenant access, paid features or CI credentials that are
not available in the academic environment.

Instead, the project adopts a defensible runtime security testing approach that
is IAST-like but not full IAST. The evidence is generated from a running Spring
Boot application, security-focused runtime tests, application security logs, and
OWASP ZAP baseline traffic.

## Alternatives Considered

| Option | Fit for GhostReport | Decision |
| --- | --- | --- |
| Commercial IAST agent | Strong runtime/source-to-sink evidence, but requires external platform access and secrets. | Not used. |
| DongTai IAST | Open-source Java IAST with agent-style instrumentation, but introduces a separate platform and operational setup beyond this repository. | Not adopted for the current academic pipeline. |
| OpenTelemetry-only tracing | Useful runtime observability, but not a vulnerability detection or taint-tracking IAST tool by itself. | Not used as IAST evidence. |
| Spring Boot runtime security tests plus OWASP ZAP | Fully local/CI reproducible, exercises real controls at runtime, and integrates with existing SAST/SCA/DAST evidence. | Adopted. |

## Adopted Evidence Model

The adopted model combines:

- Spring Boot integration/security tests;
- application audit and security monitoring assertions;
- a packaged application started in CI;
- endpoint smoke traffic against the live app;
- OWASP ZAP baseline against `http://localhost:8081`;
- uploaded Surefire, runtime evidence, ZAP reports and application logs.

This gives evidence that security controls execute correctly at runtime. It
does not claim in-process taint tracking, source-to-sink tracing or full IAST
coverage.

## Endpoints Exercised

| Endpoint | Runtime purpose |
| --- | --- |
| `GET /index.html` | Public frontend availability and browser security headers |
| `GET /login.html` | Login page availability and headers |
| `GET /api/reports/track/INVALID-CI-CODE` | Public report tracking negative path |
| `GET /api/admin/users` | Protected admin endpoint without authentication |

ZAP baseline also scans the live application surface from
`http://localhost:8081`.

## Controls Validated at Runtime

| Control | Evidence source |
| --- | --- |
| Authentication success/failure logging | `RuntimeSecurityEventLoggingTest` |
| Brute-force/rate-limit alerting | `LoginRateLimitSecurityTest` |
| JWT expiry, signature and claim validation | `JwtServiceSecurityTest` |
| Invalid JWT alerting | `RuntimeSecurityEventLoggingTest` |
| CSRF rejection and accepted protected requests | `CsrfSecurityTest` |
| Security headers and CSP | `SecurityHeadersTest`, ZAP baseline |
| Generic error handling without stack traces | `ErrorHandlingSecurityTest` |
| Audit/security log sanitization | `RuntimeSecurityEventLoggingTest` |

## Relationship with SAST and DAST

SAST and SCA identify source-code and dependency risks before runtime. ZAP DAST
checks the live HTTP surface. The runtime security evidence bridges these views
by exercising the implemented controls inside the running application and
recording application-level evidence such as audit logs, alerts, endpoint
responses and server logs.

## Claim Boundary

Recommended wording:

```text
GhostReport does not claim full agent-based IAST. For the DESOFS requirement,
the project provides an IAST-like runtime security testing layer composed of
Spring Boot security tests, live endpoint exercise, application audit/security
logs and OWASP ZAP baseline evidence. This demonstrates runtime validation of
key security controls while clearly documenting the absence of an IAST agent or
taint-tracking engine.
```
