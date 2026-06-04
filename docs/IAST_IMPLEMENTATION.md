# IAST Implementation

This document describes how GhostReport integrates IAST-oriented runtime
security evidence into the DevSecOps pipeline.

## Adopted Approach

GhostReport uses a practical two-part approach:

| Layer | Purpose | Evidence |
| --- | --- | --- |
| Runtime security tests | Exercise authentication, authorization, error handling and monitoring paths while the application context is running. | Surefire reports and runtime security events. |
| Optional Contrast Java Agent | Provide JVM agent-based IAST telemetry when project/team credentials are configured. | Contrast platform findings plus workflow readiness artifact. |

This avoids storing commercial IAST credentials in the repository and keeps the
pipeline executable in local and academic CI environments.

## GitHub Actions Workflow

The workflow `04 - IAST Runtime Security Evidence` runs on:

- `push` to `main` and `develop`;
- `pull_request` targeting `main` and `develop`;
- manual `workflow_dispatch`.

It runs:

```bash
./mvnw \
  -Dtest=RuntimeSecurityEventLoggingTest,ErrorHandlingSecurityTest,SecurityHeadersTest,JwtServiceSecurityTest,LoginRateLimitSecurityTest \
  test
```

## Evidence Generated

| Evidence | Location |
| --- | --- |
| Runtime security test results | `ghostreport/target/surefire-reports/**` |
| IAST/runtime evidence notes | `ghostreport/target/iast-evidence/iast-runtime-evidence.md` |
| GitHub artifact | `iast-runtime-security-evidence` |

## Vulnerability Categories Exercised

| Category | Runtime evidence |
| --- | --- |
| Authentication abuse | Login failure logging and brute-force alert tests |
| Token misuse | Invalid/expired JWT alert tests |
| Information disclosure | Generic error handling tests |
| Browser-facing hardening | Security header tests |
| Security monitoring | Audit and security alert persistence tests |

## Assumptions

- Local CI evidence is generated without external IAST credentials.
- External IAST telemetry is enabled only when Contrast variables/secrets are
  present in GitHub Actions.
- IAST findings are reviewed together with SAST, DAST and SCA findings because
  exploitability and application context matter.
