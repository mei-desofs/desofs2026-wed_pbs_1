# Runtime log sanitization evidence

The `dast-scan` workflow writes application logs to
`target/ghostreport-dast-app.log` and generates
`target/iast-evidence/runtime-log-sanitization.md`.

This is runtime log pattern checking for IAST-like academic evidence. It is not
full IAST, not agent-based IAST, not JVM-agent telemetry, not taint tracking and
not source-to-sink telemetry.

The runtime probe also reads dev MFA codes from this log to complete
admin/analyst/auditor MFA in CI. That read is intentionally limited to the dev
profile and is used only to prove the end-to-end MFA flow in an academic
pipeline. The generated evidence redacts tokens, tracking codes and MFA
challenge identifiers.

## Patterns checked

The workflow scans runtime logs for obvious sensitive or technical leakage:

- `password=`
- `password:`
- `Authorization:`
- bearer tokens
- `JWT_SECRET`
- `BACKUP_HMAC_SECRET`
- `stacktrace`
- Java exception stack-trace patterns

If a pattern is found, the workflow writes a redacted sample to
`target/iast-evidence/runtime-log-sensitive-findings.txt` for manual review.

## What this validates

| Risk | Runtime evidence |
| --- | --- |
| Password leakage | Failed login and runtime logs are scanned for password-like patterns. |
| JWT leakage | Bearer-token patterns are detected and redacted in findings. |
| Secret leakage | Secret variable names are scanned. |
| Stack traces | Java exception stack-trace patterns are flagged. |
| Audit quality | Runtime tests also validate that audit logs redact passwords, tokens, authorization headers and tracking codes. |
| MFA evidence | Dev MFA codes can be consumed by the CI probe, but should not be used as production delivery. |

## Limitations

- Pattern scanning is not a formal DLP engine.
- A clean scan does not prove absence of every possible sensitive value.
- Results must be interpreted together with `RuntimeSecurityEventLoggingTest`,
  `AnonymousDataLoggingTest` and manual log review.
- In production, MFA code exposure in logs must remain disabled.
