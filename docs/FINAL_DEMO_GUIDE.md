# Final Demo Guide

This guide is the recommended Sprint 2 presentation path for GhostReport. It is
focused on the professor's feedback: show the pipeline running, show concrete
security evidence, explain Pull Request governance and connect the evidence to
ASVS.

## 1. Pull Request and Governance

Start in the Pull Request view and show:

- the Pull Request template checklist;
- at least one reviewer requirement;
- branch protection rules;
- required status checks;
- conversation resolution before merge.

Use `docs/BRANCH_PROTECTION_RULES.md`, `docs/CODE_REVIEW_GUIDELINES.md` and
`docs/CODING_STANDARDS.md` as the governance evidence.

## 2. Pipeline Timeline

Open GitHub Actions and select the `dev` workflow. This workflow is the current
security evidence pipeline and is defined in `.github/workflows/dev.yml`.

Show the jobs in this order:

1. `build-test / build-and-test`
2. `security-secrets / secrets`
3. `sast / SonarCloud SAST Scan`
4. `dependency-scanning / Dependency Vulnerability Scanning`
5. `dast-scan / dast-scan`

Explain that `sast`, `dependency-scanning` and `dast-scan` depend on
`build-test` and `security-secrets`, so the GitHub Actions graph gives a single
timeline for the presentation.

## 3. What Each Job Proves

| Job | What to show | Artifact/evidence |
| --- | --- | --- |
| `build-test / build-and-test` | Compile, automated tests, JaCoCo and PIT evidence review. | `ci-surefire-test-reports`, `ci-jacoco-coverage-report`, `pit-mutation-testing-report` |
| `security-secrets / secrets` | Gitleaks scan and no confirmed repository secrets. | `secret-scan-gitleaks-json` |
| `sast / SonarCloud SAST Scan` | SpotBugs, SonarCloud and CodeQL. | `sast-reports`, GitHub Code Scanning |
| `dependency-scanning / Dependency Vulnerability Scanning` | Dependency-Check and CycloneDX SBOM. | `dependency-check-sca-reports`, `sbom-cyclonedx` |
| `dast-scan / dast-scan` | Runtime security tests, IAST-like evidence and ZAP baseline. | `iast-runtime-security-evidence`, `dast-zap-baseline-reports` |

## 4. Gate Policy To Explain

- Blocking: Maven compile, tests, JaCoCo coverage, runtime security tests,
  application startup for DAST and confirmed Gitleaks findings.
- Evidence review: PIT mutation score/output, SpotBugs findings, CodeQL alerts,
  SonarCloud quality findings, Dependency-Check findings, SBOM and ZAP baseline
  alerts.

This wording is important. It shows that the pipeline is not "green by hiding
security issues"; it separates hard build gates from findings that require
manual security triage.

## 5. ASVS Evidence

Open the ASVS tracker and `docs/ASVS_EVIDENCE.md`. For each major area, connect
one implementation file, one test or pipeline artifact and one residual risk:

- Authentication and JWT: `JwtService`, `AuthService`, JWT tests and runtime
  security evidence.
- Authorization: `SecurityConfig`, RBAC/ownership tests and controller/service
  rules.
- Validation and sanitization: DTO validation, domain value objects and upload
  tests.
- File handling: `FileStorageService`, file validation tests and ZAP/runtime
  evidence.
- Configuration: profiles, environment variables, fail-fast validation and
  Gitleaks/SCA evidence.
- Logging and error handling: audit/security event services, error handler tests
  and sanitized runtime evidence.

## 6. Evidence Folder

Explain that `Deliverables/Phase 2/Evidence` is a local archive, not a folder
written by GitHub Actions. The correct process is:

1. Run the `dev` workflow in GitHub Actions.
2. Download the workflow artifacts.
3. Extract them under `downloaded-artifacts/`.
4. Run:

```powershell
.\scripts\collect-evidence.ps1
```

Then show the organized folders under `Deliverables/Phase 2/Evidence`.

## 7. Scope Boundaries

Use precise wording in the demo:

- Runtime security and IAST-like evidence exists in every `dast-scan` run.
- Full agent-based IAST is not claimed; the academic evidence is Spring Boot
  runtime tests, endpoint checks, logs and ZAP baseline.
- PIT is evidence review, not a blocking mutation threshold gate.
- CodeQL's primary evidence is GitHub Code Scanning.
- ZAP is baseline DAST against a live application, not authenticated deep DAST.
- Production TLS, SIEM integration and advanced operational monitoring are
  documented as future operational hardening.

## 8. Closing Message

Close by showing the artifacts page and saying:

> Sprint 1 failed mainly because the pipeline evidence was not demonstrated.
> Sprint 2 now has a single visible workflow timeline, blocking quality gates,
> security evidence artifacts and ASVS mapping that links implementation,
> tests, pipeline output and residual risks.
