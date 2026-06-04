# Phase 2 Sprint 2 Evidence

This folder is reserved for curated evidence used in the final delivery report
and ASVS tracker.

GitHub Actions does not write files into this repository folder. The primary
evidence is generated as GitHub Actions artifacts and job summaries. For the
final presentation or local archive, download those artifacts and organize them
here.

## Recommended Structure

| Folder | Evidence |
| --- | --- |
| `pipelines/` | GitHub Actions screenshots, job summaries and run links. |
| `testing/` | Surefire/JUnit reports, JaCoCo reports, runtime security reports and PIT evidence. |
| `sast/` | SpotBugs reports, CodeQL screenshots/notes and CodeQL run summary artifact. |
| `sca/` | OWASP Dependency-Check HTML/JSON/XML/SARIF artifacts and CycloneDX SBOM. |
| `secret-scanning/` | Gitleaks JSON evidence. |
| `dast/` | OWASP ZAP HTML/JSON/XML reports and DAST app log. |
| `asvs/` | ASVS tracker exports, notes and mapping evidence. |

## Collecting Downloaded Artifacts

1. Run the GitHub Actions workflows from the PR or from `workflow_dispatch`.
2. Download the artifacts from the workflow run page.
3. Place the downloaded `.zip` files, extracted artifact folders or individual
   report files under:

```text
downloaded-artifacts/
```

4. Run from the repository root:

```powershell
.\scripts\collect-evidence.ps1
```

The script expands `.zip` artifacts when needed and copies matching artifacts
into the evidence areas above. It does not delete existing files. Unrecognized
artifacts are copied to `pipelines/` for manual review.

Use a dedicated folder containing only GhostReport evidence artifacts. Avoid
passing the whole Windows `Temp` folder because it contains unrelated system and
application files.

If the files are still in another clean folder, pass that folder explicitly:

```powershell
.\scripts\collect-evidence.ps1 -ArtifactsDir "C:\Users\Bárbara Silva\Downloads\ghostreport-artifacts"
```

Unmatched files are skipped by default. To also copy unmatched files into
`pipelines/` for manual review, add `-IncludeUnmatched`.

## Evidence Rules

- Keep evidence small and relevant.
- Prefer downloaded GitHub Actions artifacts, job summaries and screenshots
  from the same run.
- Do not add local `target/`, `uploads/`, runtime backups or temporary extracted
  report text files.
- Every report claim should be traceable to code, tests, a workflow artifact or
  a documented limitation.
