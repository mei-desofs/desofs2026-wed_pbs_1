# DevSecOps Pipeline Evidence

GhostReport uses separate GitHub Actions workflows so that build/test, SAST,
SCA, secret scanning and DAST can run independently and produce clear evidence.

## Workflow Overview

| Workflow | Purpose | Main artifact evidence |
| --- | --- | --- |
| `ci-tests.yml` | Compile, run automated tests and generate JaCoCo coverage. | Surefire reports and JaCoCo HTML/exec report. |
| `sast-spotbugs.yml` | Run SpotBugs static analysis over Java code. | SpotBugs XML report. |
| `sca-dependency-check.yml` | Run OWASP Dependency-Check over Maven dependencies. | HTML, JSON, XML and SARIF reports. |
| `secret-scan-gitleaks.yml` | Detect hardcoded secrets in the repository. | Gitleaks JSON report. |
| `dast-zap.yml` | Start GhostReport and run OWASP ZAP baseline DAST. | ZAP HTML, JSON, XML and application log. |

All workflows run on `push` and `pull_request` for `main` and `develop`.

## CI Tests and Coverage

The CI workflow prepares Java 17, starts a PostgreSQL 16 service container and
runs:

```bash
./mvnw clean compile
./mvnw test jacoco:report
```

This verifies compilation, automated tests and coverage evidence in one
repeatable workflow.

## SAST - SpotBugs

SpotBugs is executed as SAST evidence:

```bash
./mvnw -DskipTests compile com.github.spotbugs:spotbugs-maven-plugin:4.8.6.6:spotbugs -Dspotbugs.xmlOutput=true
```

Current use is evidence/manual triage. Findings should be reviewed and either
fixed, documented as accepted risk, or justified as framework-managed behavior.

## SCA - OWASP Dependency-Check

Dependency-Check is executed in evidence mode:

```bash
./mvnw org.owasp:dependency-check-maven:12.1.0:check -Dformat=ALL -DossindexAnalyzerEnabled=false -DfailOnError=false -DfailBuildOnCVSS=11
```

Notes:

- `NVD_API_KEY` is read from GitHub Actions secrets.
- OSS Index is disabled to avoid unrelated 401 failures.
- The build is not blocked automatically while the team triages CVEs.
- Reports are generated in HTML, JSON, XML and SARIF.

## Secret Scanning - Gitleaks

Gitleaks scans the repository root and generates a JSON report. An empty JSON
array means no hardcoded secrets were found in the scanned content.

## DAST - OWASP ZAP Baseline

The application runs directly on the GitHub runner:

```text
http://localhost:8081
```

ZAP runs inside a Docker container with host networking and scans that local
application. This is not a Docker deployment of GhostReport; Docker is only
used to run the ZAP scanner.

The current DAST mode is:

- baseline/passive;
- unauthenticated;
- non-intrusive;
- evidence/manual triage.

Authenticated scans and active/full scans are future hardening work.

## Reporting Guidance

When describing the pipeline in the report, use precise wording:

> The project uses separate GitHub Actions workflows for build/test, SAST,
> SCA, secret scanning and baseline DAST. SAST and SCA currently operate as
> evidence-producing workflows with manual triage, while CI tests validate
> compilation, automated tests and JaCoCo coverage.

Avoid claiming:

- malware scanning;
- policy gates for all vulnerabilities;
- authenticated DAST;
- full active ZAP scans;
- automatic remediation of dependency CVEs.
