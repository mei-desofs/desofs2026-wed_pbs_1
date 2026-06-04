# Security Testing Evidence

GhostReport uses automated tests and pipeline tools as security evidence for
Phase 2 Sprint 2.

| Test/tool | Purpose | Evidence | Gate mode |
| --- | --- | --- | --- |
| JUnit/MockMvc | Authentication, authorization, validation, error handling and file/backup flows | Surefire reports and `ghostreport/src/test/java` | Blocking |
| JaCoCo | Coverage visibility and minimum coverage gate | `target/site/jacoco` and CI artifact | Blocking in Stage 01 |
| Runtime security tests | Focused tests for JWT, rate limiting, headers and security events | `iast-runtime-security-evidence` | Evidence review |
| OWASP ZAP | Baseline DAST against the running app | ZAP HTML/JSON/XML artifacts | Evidence review |
| PIT | Mutation testing for test-quality evidence | `target/pit-reports` and CI artifact | Evidence review |

## PIT Configuration

PIT is configured with a limited initial scope:

- `com.ghostreport.domain.*`
- `com.ghostreport.service.*`

The goal is to produce useful mutation evidence for domain and service logic
without making Sprint 2 depend on a broad mutation threshold across controllers,
Spring wiring and infrastructure code. The workflow should publish the PIT
artifact even when the mutation score requires manual review.

Mutation testing remains evidence review, not a blocker. A non-zero PIT exit
code must be visible and triaged, not hidden.

## Current PIT Status

The Maven PIT plugin was updated to `1.25.3`, HTML and XML output are enabled,
and reports are configured with `timestampedReports=false` for a predictable
`target/pit-reports` path.

Local execution on this workstation still fails before report generation
because the installed JVM is Java 23 and PIT exits with `MINION_DIED` /
`CoverageMinion` startup failure. The GitHub Actions workflow uses Java 17 and
is the expected environment for producing the real PIT artifact. If the CI run
still fails, keep the generated exit code and summary as evidence review and
triage the workflow logs.
