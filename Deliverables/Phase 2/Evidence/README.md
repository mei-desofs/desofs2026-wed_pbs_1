# Phase 2 Sprint 2 Evidence

This folder is reserved for curated evidence used in the final delivery report
and ASVS tracker.

## Recommended Structure

| Folder | Evidence |
| --- | --- |
| `pipelines/` | GitHub Actions screenshots and run links. |
| `testing/` | Maven test output, Surefire summaries and JaCoCo screenshots. |
| `sast/` | SpotBugs XML/report summaries. |
| `sca/` | OWASP Dependency-Check HTML/JSON/XML/SARIF artifacts or screenshots. |
| `secret-scanning/` | Gitleaks JSON evidence. |
| `dast/` | OWASP ZAP HTML/JSON/XML reports and DAST app log. |
| `asvs/` | ASVS tracker exports, notes and mapping evidence. |

## Evidence Rules

- Keep evidence small and relevant.
- Prefer screenshots or downloaded GitHub Actions artifacts.
- Do not add local `target/`, `uploads/`, runtime backups or temporary extracted
  report text files.
- Every report claim should be traceable to code, tests, a workflow artifact or
  a documented limitation.
