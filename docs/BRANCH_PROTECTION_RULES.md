# Branch Protection Rules

GitHub branch protection is configured in repository settings. These are the
project rules for `main` and `develop`.

## Required Rules

| Setting | Required value | Rationale |
| --- | --- | --- |
| Require pull request before merging | Enabled | Prevents direct merges without review. |
| Required approvals | 1 minimum | Ensures peer review. |
| Dismiss stale approvals | Enabled | Requires re-review after meaningful changes. |
| Require status checks | Enabled | Ensures automated validation before merge. |
| Required CI check | `01 - CI Build, Tests and Coverage` | Blocks broken build/tests/coverage. |
| Require branches to be up to date | Enabled | Reduces merge-time regressions. |
| Require conversation resolution | Enabled | Ensures review comments are handled. |
| Restrict who can push | Recommended for `main` | Protects the release branch. |
| Allow force pushes | Disabled | Protects history and evidence. |
| Allow deletions | Disabled | Protects protected branches. |

## Security Evidence Workflows

The following workflows should run on Pull Requests or be manually triggered for
assessment evidence:

- `00 - Secret Scanning Gitleaks`
- `01 - CI Build, Tests and Coverage`
- `02A - SAST SpotBugs`
- `02B - SCA OWASP Dependency-Check`
- `02C - SAST CodeQL`
- `02D - SBOM CycloneDX`
- `03 - DAST OWASP ZAP Baseline`
- `04 - Runtime Security Evidence and IAST Readiness`
- `05 - Mutation Testing PIT`

CI is the primary required status check. Security analysis workflows produce
reviewable evidence, and confirmed secret findings are treated as blocking.

## Demonstration Checklist

1. Open repository settings.
2. Show branch protection for `main` or `develop`.
3. Show required Pull Request approval.
4. Show required status checks.
5. Open a PR and show the workflows running or completed.
