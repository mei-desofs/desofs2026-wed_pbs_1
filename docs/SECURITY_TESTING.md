# Security Testing Evidence

GhostReport uses automated tests and pipeline tools as security evidence for
Phase 2 Sprint 2.

| Test/tool | Purpose | Evidence | Gate mode |
| --- | --- | --- | --- |
| JUnit/MockMvc | Authentication, authorization, validation, error handling and file/backup flows | Surefire reports and `ghostreport/src/test/java` | Blocking |
| JaCoCo | Coverage visibility and minimum coverage gate | `target/site/jacoco` and `ci-jacoco-coverage-report` | Blocking in `build-test / build-and-test` |
| Runtime security tests | Focused tests for JWT, rate limiting, CSRF, headers and security events | `iast-runtime-security-evidence` | Blocking inside `dast-scan / dast-scan` |
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

Local execution on this workstation was tested in two ways:

- With the default local Maven repository under the Windows user profile, PIT
  fails with `MINION_DIED` / `CoverageMinion` startup failure.
- With a temporary Temurin JDK 17 and an ASCII-only Maven repository
  (`C:\Projetos\m2-pit`), PIT starts and generates partial report output, but
  did not finish within the local execution window.

The generated partial evidence is archived under
`Deliverables/Phase 2/Evidence/testing/pit-local-jdk17-ascii-m2-partial`.
The `dev` GitHub Actions workflow remains the expected place to produce the final
PIT artifact. If the CI run is still too slow, reduce the PIT scope further to
a smaller service/domain package and keep the exit code plus summary as
evidence review.
