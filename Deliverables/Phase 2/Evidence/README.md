# Phase 2 Sprint 2 Evidence

This folder is a curated local archive for presentation and assessment. GitHub
Actions does not write directly into this repository folder. The primary source
of pipeline evidence is still the GitHub Actions run, its job summaries and its
downloaded artifacts.

## Evidence Index

| Folder | Tool/evidence | DESOFS rubric | ASVS chapters | Current limitation / residual risk |
| --- | --- | --- | --- | --- |
| `testing/` | Surefire/JUnit, JaCoCo, runtime security evidence, PIT | Build and Test, ASVS | V2, V5, V6, V7, V8, V9, V16 | Runtime tests now include JWT logout/revocation locally; download a fresh CI artifact after pushing. PIT is evidence review; local JDK 17 with an ASCII Maven repository produced partial output but did not finish in the local time window. |
| `sast/` | SpotBugs and CodeQL summary | Development, Pipeline Automation | V15, V16 | CodeQL primary evidence is GitHub Code Scanning; SpotBugs remediation is documented in `docs/SPOTBUGS_TRIAGE.md` pending a fresh scan. |
| `sca/` | OWASP Dependency-Check and CycloneDX SBOM | Build and Test, Pipeline Automation | V13, V15 | Includes original and post-remediation Dependency-Check evidence; `docs/SCA_TRIAGE.md` records remaining residual-risk findings. |
| `secret-scanning/` | Gitleaks JSON report | Pipeline Automation, Production | V13, V14, V15 | Empty `[]` report means no leaked secrets were found in the scanned scope. A clean scan of tracked `HEAD` is archived separately from noisy local workspace diagnostics. |
| `dast/` | OWASP ZAP baseline reports | Build and Test, ASVS | V3, V4, V12 | Existing ZAP reports are pre-CSP-remediation evidence; rerun ZAP to archive the updated result. |
| `pipelines/` | GitHub Actions screenshots, job summaries, run links and workflow validation | Pipeline Automation | V15 | Screenshots/job summaries must be manually downloaded or captured. `actionlint-local-validation` records local workflow validation. |
| `asvs/` | Tracker exports and ASVS notes | ASVS, Overall Project | V1-V17 | Formal checklist lives in `Deliverables/Phase 2/ASVS_5.0_Tracker_Phase 2_Sprint 2.xlsx`. |
| `code-review/` | Pull request templates, review screenshots and governance evidence | Development, Overall Project | V15, V16 | GitHub branch protection must be shown in the repository UI. |
| `assessment/` | Security assessment summaries and triage notes | Production, Operate, Overall Project | V13, V15, V16 | Manual triage must remain traceable to actual reports. |

## Current Downloaded Artifacts

The local archive currently includes artifacts for:

- CI Surefire and JaCoCo evidence.
- Runtime security evidence and IAST readiness summary.
- PIT evidence summary and exit code.
- SpotBugs XML reports.
- CodeQL run-context summary.
- Dependency-Check HTML, JSON, XML and SARIF reports.
- CycloneDX SBOM in JSON and XML.
- Gitleaks JSON report.
- ZAP baseline HTML, JSON and XML reports.

## Collecting Downloaded Artifacts

1. Run the GitHub Actions workflows from the pull request or through
   `workflow_dispatch`.
2. Download the artifacts from the workflow run page.
3. Extract or place them under a clean local folder, for example:

```text
downloaded-artifacts/
```

4. Run from the repository root:

```powershell
.\scripts\collect-evidence.ps1
```

If the artifacts are in a different clean folder, pass it explicitly:

```powershell
.\scripts\collect-evidence.ps1 -ArtifactsDir "C:\Users\Bárbara Silva\Downloads\ghostreport-artifacts"
```

The collection script is intended to copy downloaded GitHub Actions artifacts
into this curated archive. It should not be run against the whole Windows Temp
folder because that can copy unrelated files.

## Evidence Rules

- Do not invent evidence or hide failed findings.
- Keep the original tool report whenever possible.
- Use `Partially Compliant`, `Residual Risk` or `Evidence Review` when a control
  is incomplete.
- Do not mark ASVS requirements as `Compliant` unless implementation plus
  tests or tool evidence exists.
- Keep old evidence if it is referenced by reports; replace it only with newer
  equivalent evidence from a later run.
