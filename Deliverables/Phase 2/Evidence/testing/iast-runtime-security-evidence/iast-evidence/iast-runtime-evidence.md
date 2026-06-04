# Runtime Security Evidence and IAST Readiness

Generated at: 2026-06-04T13:38:51Z

## Runtime security tests

Executed security-focused tests for authentication events, JWT validation, rate limiting, security headers and error handling.
The JaCoCo coverage gate is intentionally skipped in this workflow because Stage 01 is the blocking coverage workflow.

## IAST agent integration

Contrast Java agent configuration variables are not present in this run.
The workflow still produces local runtime security evidence from automated tests.
To enable external IAST telemetry, configure CONTRAST_AGENT_VERSION, CONTRAST__API__URL, CONTRAST__API__API_KEY, CONTRAST__API__SERVICE_KEY and CONTRAST__API__USER_NAME as GitHub Actions variables/secrets.
