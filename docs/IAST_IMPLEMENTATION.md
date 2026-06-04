# Runtime Security Evidence and IAST Readiness

This document describes how GhostReport produces runtime security evidence and
keeps the project ready for an optional external IAST agent integration.

## Adopted Approach

GhostReport uses a practical two-part approach:

| Layer | Purpose | Evidence |
| --- | --- | --- |
| Runtime security tests | Exercise authentication, authorization, error handling and monitoring paths while the application context is running. | Surefire reports and runtime security events. |
| Optional Contrast Java Agent | Provide JVM agent-based IAST telemetry when project/team credentials are configured. | Contrast platform findings plus workflow readiness artifact. |

This avoids storing commercial IAST credentials in the repository and keeps the
pipeline executable in local and academic CI environments.

## GitHub Actions Workflow

The workflow `04 - Runtime Security Evidence and IAST Readiness` runs on:

- `push` to `main` and `develop`;
- `pull_request` targeting `main` and `develop`;
- manual `workflow_dispatch`.

It runs:

```bash
./mvnw \
  -Djacoco.skip=true \
  -Dtest=RuntimeSecurityEventLoggingTest,ErrorHandlingSecurityTest,SecurityHeadersTest,JwtServiceSecurityTest,LoginRateLimitSecurityTest \
  test
```

The workflow intentionally skips the global JaCoCo coverage check. Stage 01 is
the blocking build/tests/coverage gate; Stage 04 fails only when the selected
runtime security tests fail or expected Surefire/evidence files are missing.

## Evidence Generated

| Evidence | Location |
| --- | --- |
| Runtime security test results | `ghostreport/target/surefire-reports/**` |
| Runtime/IAST readiness evidence notes | `ghostreport/target/iast-evidence/iast-runtime-evidence.md` |
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
- Without Contrast credentials, the project does not claim complete IAST
  telemetry. It claims runtime security evidence plus readiness for optional
  agent-based IAST.
- IAST findings, when available, are reviewed together with SAST, DAST and SCA
  findings because exploitability and application context matter.
