# Phase 2 Sprint 2 Evidence

This folder is a curated local archive for presentation and assessment. GitHub
Actions does not write directly into this repository folder.

The primary evidence source is the GitHub Actions `dev` workflow defined in:

```text
.github/workflows/dev.yml
```

Use this local folder only after downloading artifacts from a real GitHub
Actions run.

## Current Workflow Artifacts

| GitHub Actions job | Artifact | Local evidence folder |
| --- | --- | --- |
| `build-test / build-and-test` | `ci-surefire-test-reports` | `testing/` |
| `build-test / build-and-test` | `ci-jacoco-coverage-report` | `testing/` |
| `build-test / build-and-test` | `pit-mutation-testing-report` | `testing/` |
| `security-secrets / secrets` | `secret-scan-gitleaks-json` | `secret-scanning/` |
| `sast / SonarCloud SAST Scan` | `sast-reports` | `sast/` |
| `dependency-scanning / Dependency Vulnerability Scanning` | `dependency-check-sca-reports` | `sca/` |
| `dependency-scanning / Dependency Vulnerability Scanning` | `sbom-cyclonedx` | `sca/` |
| `dast-scan / dast-scan` | `iast-runtime-security-evidence` | `testing/` |
| `dast-scan / dast-scan` | `dast-zap-baseline-reports` | `dast/` |

## Evidence Index

| Folder | Evidence type | DESOFS rubric | ASVS chapters |
| --- | --- | --- | --- |
| `testing/` | Surefire/JUnit, JaCoCo, runtime security evidence and PIT. | Build and Test, ASVS | V2, V5, V6, V7, V8, V9, V16 |
| `sast/` | SpotBugs, SonarCloud notes and CodeQL summaries/screenshots. | Development, Pipeline Automation | V15, V16 |
| `sca/` | OWASP Dependency-Check and CycloneDX SBOM. | Build and Test, Pipeline Automation | V13, V15 |
| `secret-scanning/` | Gitleaks JSON reports. | Pipeline Automation, Production | V13, V14, V15 |
| `dast/` | OWASP ZAP baseline reports and application logs. | Build and Test, ASVS | V3, V4, V12 |
| `pipelines/` | GitHub Actions screenshots, job summaries, run links and actionlint evidence. | Pipeline Automation | V15 |
| `asvs/` | Tracker exports and ASVS notes. | ASVS, Overall Project | V1-V17 |
| `code-review/` | Pull Request templates, review screenshots and governance evidence. | Development, Overall Project | V15, V16 |
| `assessment/` | Security assessment summaries and triage notes. | Production, Operate, Overall Project | V13, V15, V16 |

## Collecting Downloaded Artifacts

1. Run the `dev` workflow from a Pull Request or with `workflow_dispatch`.
2. Open the completed workflow run in GitHub Actions.
3. Download the artifacts.
4. Extract or place the downloaded artifact folders under:

```text
downloaded-artifacts/
```

5. Run from the repository root:

```powershell
.\scripts\collect-evidence.ps1
```

If the artifacts are in a different folder, pass it explicitly:

```powershell
.\scripts\collect-evidence.ps1 -ArtifactsDir "C:\Users\Barbara Silva\Downloads\ghostreport-artifacts"
```

The script copies recognized artifacts into the evidence categories above.
Unrecognized artifacts are copied to `pipelines/` so they are not lost.

## Evidence Rules

- Do not invent evidence or hide failed findings.
- Keep original tool reports whenever possible.
- Use `Partially Compliant`, `Residual Risk` or `Evidence Review` when a
  control is incomplete.
- Do not mark ASVS requirements as `Compliant` unless implementation plus test,
  pipeline or report evidence exists.
- Keep old evidence if it is referenced by reports; replace it only with newer
  equivalent evidence from a later run.
- Remember that GitHub Actions artifacts are the source of truth; this folder is
  only the local presentation/archive copy.
