# Pipeline Artifacts and Evidence Map

This document maps GitHub Actions artifacts to the final report, ASVS tracker
and technical evidence folders.

## Artifact Map

| Workflow | Artifact | Use in report / ASVS |
| --- | --- | --- |
| `00 - Secret Scanning Gitleaks` | `secret-scan-gitleaks-json` | Evidence for secret scanning and secure repository hygiene. |
| `01 - CI Build, Tests and Coverage` | `ci-surefire-test-reports` | Evidence that automated tests ran in CI. |
| `01 - CI Build, Tests and Coverage` | `ci-jacoco-coverage-report` | Evidence for coverage tracking and test maturity. |
| `02A - SAST SpotBugs` | `sast-spotbugs-report` | Static analysis evidence and hardening findings. |
| `02B - SCA OWASP Dependency-Check` | `dependency-check-sca-html` | Human-readable dependency vulnerability report. |
| `02B - SCA OWASP Dependency-Check` | `dependency-check-sca-json` | Machine-readable dependency vulnerability evidence. |
| `02B - SCA OWASP Dependency-Check` | `dependency-check-sca-xml` | Structured report for external tooling or archiving. |
| `02B - SCA OWASP Dependency-Check` | `dependency-check-sca-sarif` | Future code scanning integration evidence. |
| `02C - SAST CodeQL` | GitHub Code Scanning alerts | Semantic SAST evidence for Java security patterns. |
| `02D - SBOM CycloneDX` | `sbom-cyclonedx` | Software bill of materials for dependency inventory. |
| `03 - DAST OWASP ZAP Baseline` | `dast-zap-baseline-html` | Human-readable runtime security report. |
| `03 - DAST OWASP ZAP Baseline` | `dast-zap-baseline-json` | Machine-readable DAST evidence. |
| `03 - DAST OWASP ZAP Baseline` | `dast-zap-baseline-xml` | Structured DAST evidence. |
| `03 - DAST OWASP ZAP Baseline` | `dast-ghostreport-app-log` | Evidence that the application started during DAST. |
| `04 - Mutation Testing PIT` | `pit-mutation-testing-report` | Mutation testing evidence for test strength. |

## Recommended Evidence Folder Placement

| Artifact type | Suggested folder |
| --- | --- |
| GitHub Actions screenshots | `Deliverables/Phase 2/Evidence/pipelines/` |
| Surefire/JUnit summaries | `Deliverables/Phase 2/Evidence/testing/` |
| JaCoCo screenshots/export | `Deliverables/Phase 2/Evidence/testing/` |
| SpotBugs report | `Deliverables/Phase 2/Evidence/sast/` |
| CodeQL alerts/screenshots | `Deliverables/Phase 2/Evidence/sast/` |
| Dependency-Check report | `Deliverables/Phase 2/Evidence/sca/` |
| CycloneDX SBOM | `Deliverables/Phase 2/Evidence/sca/` |
| Gitleaks JSON | `Deliverables/Phase 2/Evidence/secret-scanning/` |
| ZAP reports/logs | `Deliverables/Phase 2/Evidence/dast/` |
| PIT report | `Deliverables/Phase 2/Evidence/testing/` |
| ASVS tracker exports | `Deliverables/Phase 2/Evidence/asvs/` |

## ASVS Evidence Mapping

| ASVS evidence area | Pipeline support |
| --- | --- |
| Secure build and verification | CI build/tests workflow. |
| Automated security testing | SpotBugs, CodeQL, Dependency-Check, Gitleaks and ZAP workflows. |
| Dependency risk management | Dependency-Check reports and CycloneDX SBOM. |
| Secret management | Gitleaks report and GitHub Actions secrets. |
| Dynamic runtime testing | ZAP baseline reports. |
| Test quality | JaCoCo and PIT mutation testing reports. |
| Security regression evidence | Repeated workflow runs on push and pull request. |

## Report Wording

Use precise wording:

> The pipeline produces separate artifacts for build/test results, coverage,
> static analysis, dependency analysis, SBOM, secret scanning, mutation testing
> and baseline DAST. These
> artifacts are used as technical evidence for the ASVS tracker and final
> report.

Avoid claiming:

- automatic remediation;
- complete vulnerability elimination;
- authenticated DAST;
- malware scanning;
- production-grade policy gates for all findings.
