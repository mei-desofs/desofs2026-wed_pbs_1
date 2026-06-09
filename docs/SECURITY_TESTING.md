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

PIT is configured for the full GhostReport package:

- `com.ghostreport.*`

HTML and XML output are enabled, and `timestampedReports=false` keeps the final
report at a predictable path:

```text
ghostreport/target/pit-reports/index.html
```

Mutation testing remains evidence review, not a fast merge gate. It is separated
from the main `dev` workflow into the dedicated `pit-mutation-testing` workflow
so the main pipeline stays responsive while PIT can run long enough to generate
the complete final report.

## Current PIT Status

The dedicated workflow:

- runs `org.pitest:pitest-maven:mutationCoverage`;
- uses the full Maven PIT scope from `ghostreport/pom.xml`;
- validates that `target/pit-reports/index.html` exists;
- uploads the complete `target/pit-reports/**` artifact;
- writes `pit-evidence-summary.md` and `pit-mutation-summary.md` with the final
  mutation percentages when `mutations.xml` is generated.

Local execution on this workstation can still be unreliable with Java 23 because
PIT has previously failed with `MINION_DIED` / `CoverageMinion` startup failure.
The GitHub Actions workflow uses Java 17 and is the expected source of final PIT
evidence.
