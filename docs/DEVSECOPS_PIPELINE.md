# DevSecOps Pipeline

GhostReport uses one GitHub Actions workflow as the Sprint 2 evidence pipeline:

```text
.github/workflows/dev.yml
```

The workflow name is `dev`. It runs on `workflow_dispatch`, `push` and
`pull_request` for `main` and `develop`. This single workflow is the preferred
presentation view because GitHub Actions shows the jobs and their dependencies
in one timeline.

## Pipeline Timeline

```text
build-test / build-and-test
security-secrets / secrets
        |
        +--> sast / SonarCloud SAST Scan
        +--> dependency-scanning / Dependency Vulnerability Scanning
        +--> dast-scan / dast-scan
```

`build-test` and `security-secrets` run first. The SAST, SCA/SBOM and
DAST/runtime evidence jobs only start after those two jobs complete.

## Job Map

| Job | Purpose | Gate mode | Main artifacts/evidence |
| --- | --- | --- | --- |
| `build-test / build-and-test` | Maven automated tests, JaCoCo coverage report and scoped PIT mutation evidence review. | Blocking for tests and coverage. PIT is evidence review. | `ci-surefire-test-reports`, `ci-jacoco-coverage-report`, `pit-mutation-testing-report` |
| `security-secrets / secrets` | Gitleaks scan of repository content using `.gitleaks.toml`. | Blocking for confirmed secret leaks. | `secret-scan-gitleaks-json` |
| `sast / SonarCloud SAST Scan` | CodeQL, SpotBugs and SonarCloud SAST. | Evidence review. SonarCloud fails if `SONAR_TOKEN` is missing or the analysis fails. | `sast-reports`, GitHub Code Scanning alerts |
| `dependency-scanning / Dependency Vulnerability Scanning` | OWASP Dependency-Check and CycloneDX SBOM. | Evidence review. Dependency-Check does not block the pipeline by CVSS threshold in Sprint 2 evidence mode. | `dependency-check-sca-reports`, `sbom-cyclonedx`, Code Scanning SARIF when generated |
| `dast-scan / dast-scan` | Runtime security tests, IAST-like evidence, live application startup and OWASP ZAP baseline. | Runtime tests and application startup are blocking. ZAP is evidence review. | `iast-runtime-security-evidence`, `dast-zap-baseline-reports` |

## Blocking Policy

The project treats build, tests, coverage and confirmed secret leaks as the main
quality gates. These checks answer the basic question: can the application be
built, tested and reviewed without introducing obvious repository secrets?

SAST, SCA, SBOM, DAST, runtime security evidence and PIT produce evidence for
manual review. This is intentional: security tools often need triage to decide
whether a finding is exploitable, framework-managed, out of scope or already
mitigated elsewhere.

## Evidence Boundaries

| Area | What exists | What is not claimed |
| --- | --- | --- |
| CodeQL | CodeQL runs in GitHub Actions and publishes findings to GitHub Code Scanning. The workflow also uploads SAST summary files through `sast-reports`. | A local full CodeQL SARIF archive is not promised unless exported separately from GitHub. |
| Runtime security / IAST-like | Runtime security-focused tests always run and produce Surefire plus `iast-runtime-evidence.md`. The workflow starts the packaged app, records selected endpoint status checks and runs ZAP baseline against the live app. | Full agent-based IAST, taint tracking and source-to-sink telemetry are not claimed. |
| PIT | CI PIT runs in evidence review mode against `com.ghostreport.domain.*`, writes `pit-evidence-summary.md`, `pit-mutation-summary.md` and `pit-exit-code.txt`, and includes mutation percentages when `mutations.xml` is generated. | PIT is not a blocking mutation score gate in Sprint 2. Full-scope PIT can be run manually when deeper mutation evidence is needed. |
| ZAP | ZAP baseline runs against a live local GhostReport instance in the CI runner and uploads HTML/XML/JSON reports plus the application log. | ZAP baseline is not authenticated deep DAST and is not equivalent to a full penetration test. |
| Local evidence folder | `Deliverables/Phase 2/Evidence` is a curated local archive for downloaded artifacts. | GitHub Actions does not write directly into this repository folder. |

## Artifact Collection

The primary evidence source is the GitHub Actions run page: job logs, job
summaries and artifacts. For presentation and local archive, download the run
artifacts and organize them with:

```powershell
.\scripts\collect-evidence.ps1
```

The script expects artifacts under `downloaded-artifacts/` by default and copies
them into:

- `Deliverables/Phase 2/Evidence/testing`
- `Deliverables/Phase 2/Evidence/sast`
- `Deliverables/Phase 2/Evidence/sca`
- `Deliverables/Phase 2/Evidence/secret-scanning`
- `Deliverables/Phase 2/Evidence/dast`
- `Deliverables/Phase 2/Evidence/pipelines`
- `Deliverables/Phase 2/Evidence/asvs`

## Current Local Validation

Validate the workflow locally with `actionlint` when it is available. Validate
the Spring Boot module with:

```powershell
cd ghostreport
.\mvnw test
.\mvnw verify
```

`verify` is the local command that generates the JaCoCo report and applies the
coverage check. Final grading evidence should still come from a fresh GitHub
Actions run after the branch is pushed.

## Demo Path

For the final presentation:

1. Open the Pull Request checks page.
2. Open the `dev` workflow run.
3. Show `build-test` and `security-secrets` as the first gates.
4. Show the dependent SAST, SCA/SBOM and DAST/runtime jobs.
5. Open the artifacts list and connect each artifact to the ASVS evidence.
6. Explain which checks are blocking and which are evidence review.
