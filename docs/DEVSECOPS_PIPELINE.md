# DevSecOps Pipeline

GhostReport uses GitHub Actions to automate build validation, automated tests,
coverage, security analysis and evidence collection. The workflows are kept
separate so each security activity can be rerun independently and presented with
clear artifacts.

## Pipeline Overview

```text
00 Secret Scanning
01 Build, Tests and Coverage
02A Static Analysis - SpotBugs
02B Dependency Scanning - OWASP Dependency-Check
02C Static Analysis - CodeQL
02D SBOM - CycloneDX
03 DAST - OWASP ZAP Baseline
04 IAST / Runtime Security Evidence
05 Mutation Testing - PIT
```

All workflows support manual execution with `workflow_dispatch`. The project
also runs them on `push` and `pull_request` for `main` and `develop`, giving
visible evidence during Pull Request review.

## Workflow Map

| Stage | Workflow file | GitHub Actions name | Trigger | Gate mode | Main evidence |
| --- | --- | --- | --- | --- | --- |
| 00 | `secret-scan-gitleaks.yml` | `00 - Secret Scanning Gitleaks` | push, pull_request, manual | Blocking for confirmed leaks | `secret-scan-gitleaks-json` |
| 01 | `ci-tests.yml` | `01 - CI Build, Tests and Coverage` | push, pull_request, manual | Blocking | `ci-surefire-test-reports`, `ci-jacoco-coverage-report` |
| 02A | `sast-spotbugs.yml` | `02A - SAST SpotBugs` | push, pull_request, manual | Evidence review | `sast-spotbugs-report` |
| 02B | `sca-dependency-check.yml` | `02B - SCA OWASP Dependency-Check` | push, pull_request, manual | Evidence review | Dependency-Check HTML, JSON, XML, SARIF |
| 02C | `sast-codeql.yml` | `02C - SAST CodeQL` | push, pull_request, manual | Evidence review | GitHub Code Scanning alerts |
| 02D | `sbom-cyclonedx.yml` | `02D - SBOM CycloneDX` | push, pull_request, manual | Evidence review | `sbom-cyclonedx` |
| 03 | `dast-zap.yml` | `03 - DAST OWASP ZAP Baseline` | push, pull_request, manual | Evidence review | ZAP HTML, JSON, XML and application log |
| 04 | `iast-runtime.yml` | `04 - IAST Runtime Security Evidence` | push, pull_request, manual | Evidence review | Runtime security test report and IAST integration notes |
| 05 | `mutation-pit.yml` | `05 - Mutation Testing PIT` | push, pull_request, manual | Evidence review | `pit-mutation-testing-report` |

## Job Responsibilities

| Stage | What it validates |
| --- | --- |
| 00 | Repository content is scanned for hardcoded secrets before deeper validation. |
| 01 | Java 17 build, Maven compilation, automated tests, JaCoCo report and coverage thresholds. |
| 02A | Java static analysis with SpotBugs. |
| 02B | Known vulnerable dependency analysis with OWASP Dependency-Check. |
| 02C | Semantic Java security analysis with CodeQL. |
| 02D | Dependency inventory with CycloneDX SBOM. |
| 03 | Runtime-facing HTTP baseline scan with OWASP ZAP against a live GhostReport instance. |
| 04 | Runtime security instrumentation evidence during automated security tests and optional Contrast Java agent readiness. |
| 05 | Test strength assessment with PIT mutation testing. |

## Blocking Policy

CI build/tests/coverage are the main merge gate because they validate whether
the application builds and whether security regression tests pass. Gitleaks is
blocking because confirmed secrets should not enter the repository.

SAST, SCA, SBOM, DAST, IAST evidence and mutation testing produce reviewable
artifacts. Their findings are assessed by the team because security tools often
require context to distinguish applicable findings from framework-managed
behavior or non-exploitable results.

## Artifact Evidence

| Evidence area | Artifact or location |
| --- | --- |
| Automated tests | `ci-surefire-test-reports` |
| Coverage | `ci-jacoco-coverage-report` |
| Static analysis | `sast-spotbugs-report`, CodeQL Code Scanning |
| Dependency scanning | `dependency-check-sca-html`, `dependency-check-sca-json`, `dependency-check-sca-xml`, `dependency-check-sca-sarif` |
| SBOM | `sbom-cyclonedx` |
| Secret scanning | `secret-scan-gitleaks-json` |
| DAST | `dast-zap-baseline-html`, `dast-zap-baseline-json`, `dast-zap-baseline-xml`, `dast-ghostreport-app-log` |
| IAST/runtime evidence | `iast-runtime-security-evidence` |
| Mutation testing | `pit-mutation-testing-report` |

## Why Workflows Remain Separate

Separate workflows keep the Actions page easy to explain while avoiding one
large job that hides evidence. Long-running scanners can be rerun individually,
and each workflow uploads focused artifacts with stable names.

## Presentation Path

For the final demonstration:

1. Open a Pull Request and show the checks list.
2. Show `00` and `01` as the primary gates.
3. Open the `02` security analysis workflows and their artifacts.
4. Show ZAP runtime evidence in `03`.
5. Show IAST/runtime security evidence in `04`.
6. Link the artifacts to `docs/ASVS_LEVEL2_EVIDENCE.md`.
