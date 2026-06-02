# DevSecOps Pipeline Evidence

GhostReport uses separate GitHub Actions workflows so that build/test, SAST,
SCA, SBOM, mutation testing, secret scanning and DAST can run independently and produce clear evidence.
The intended orchestration is documented in [PIPELINE_FLOW.md](PIPELINE_FLOW.md),
and the artifact-to-evidence mapping is documented in
[PIPELINE_ARTIFACTS.md](PIPELINE_ARTIFACTS.md).

## Workflow Overview

| Workflow | Purpose | Main artifact evidence |
| --- | --- | --- |
| `secret-scan-gitleaks.yml` | Stage 00: detect hardcoded secrets in the repository. | Gitleaks JSON report. |
| `ci-tests.yml` | Stage 01: compile, run automated tests and generate JaCoCo coverage. | Surefire reports and JaCoCo HTML/exec report. |
| `sast-spotbugs.yml` | Stage 02A: run SpotBugs static analysis over Java code. | SpotBugs XML report. |
| `sca-dependency-check.yml` | Stage 02B: run OWASP Dependency-Check over Maven dependencies. | HTML, JSON, XML and SARIF reports. |
| `sast-codeql.yml` | Stage 02C: run CodeQL semantic static analysis. | GitHub Code Scanning alerts. |
| `sbom-cyclonedx.yml` | Stage 02D: generate CycloneDX software bill of materials. | CycloneDX JSON/XML SBOM. |
| `dast-zap.yml` | Stage 03: start GhostReport and run OWASP ZAP baseline DAST. | ZAP HTML, JSON, XML and application log. |
| `mutation-pit.yml` | Stage 04: run PIT mutation testing. | PIT mutation report. |

All workflows run on `push` and `pull_request` for `main` and `develop`, and
also support `workflow_dispatch` for manual evidence regeneration.

## Orchestration Summary

The validation flow is:

```text
Secret scanning -> CI build/tests/coverage -> SAST/SCA/SBOM -> DAST -> PIT -> evidence collection
```

The workflows are separate because this makes each validation activity easier
to rerun and explain. CI is the main blocking baseline; SAST, SCA, SBOM, DAST
and PIT are currently evidence/manual triage workflows.

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

## SAST - CodeQL

CodeQL complements SpotBugs with semantic analysis and publishes results through
GitHub Code Scanning. It is used as additional SAST evidence and findings should
be triaged manually.

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

## SBOM - CycloneDX

CycloneDX generates a software bill of materials for the Maven project. The SBOM
supports dependency inventory, SCA review and ASVS evidence, but it does not
replace vulnerability triage.

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

## Mutation Testing - PIT

PIT mutation testing is executed as test quality evidence. It helps identify
tests that execute code without detecting behavioral changes. Mutation score is
not currently a blocking threshold.

## Reporting Guidance

When describing the pipeline in the report, use precise wording:

> The project uses separate GitHub Actions workflows for build/test, SAST,
> SCA, SBOM, secret scanning, mutation testing and baseline DAST. The intended
> flow is secret scanning, CI build/tests/coverage, SAST/SCA/SBOM, DAST, PIT and
> evidence collection. Security analysis workflows currently operate as
> evidence-producing workflows with manual triage, while CI tests validate
> compilation, automated tests and JaCoCo coverage thresholds.

Avoid claiming:

- malware scanning;
- policy gates for all vulnerabilities;
- authenticated DAST;
- full active ZAP scans;
- automatic remediation of dependency CVEs.
