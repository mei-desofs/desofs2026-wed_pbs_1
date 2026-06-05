# Branch Protection Rules

GitHub branch protection is configured in repository settings. These are the
recommended project rules for `main` and `develop`.

## Required Rules

| Setting | Required value | Rationale |
| --- | --- | --- |
| Require pull request before merging | Enabled | Prevents direct merges without review. |
| Required approvals | 1 minimum | Ensures peer review. |
| Dismiss stale approvals | Enabled | Requires re-review after meaningful changes. |
| Require status checks | Enabled | Ensures automated validation before merge. |
| Require branches to be up to date | Enabled | Reduces merge-time regressions. |
| Require conversation resolution | Enabled | Ensures review comments are handled. |
| Restrict who can push | Recommended for `main` | Protects the release branch. |
| Allow force pushes | Disabled | Protects history and evidence. |
| Allow deletions | Disabled | Protects protected branches. |

## Recommended Required Checks

The current GitHub Actions evidence workflow is `.github/workflows/dev.yml`.
These checks are the best candidates for required status checks:

| Check | Required? | Reason |
| --- | --- | --- |
| `build-test / build-and-test` | Yes | Blocks broken builds, failing tests and failing JaCoCo coverage. |
| `security-secrets / secrets` | Yes | Blocks confirmed repository secret leaks. |
| `sast / SonarCloud SAST Scan` | Conditional | Require it only when `SONAR_TOKEN` and SonarCloud project variables are configured for all protected branches. |
| `dependency-scanning / Dependency Vulnerability Scanning` | Optional/evidence review | Produces SCA and SBOM evidence; findings require manual triage. |
| `dast-scan / dast-scan` | Optional/evidence review | Produces runtime security and ZAP evidence; baseline DAST findings require manual triage. |

If the team wants strict Sprint 2 demonstration governance, it can require all
five checks. For day-to-day development, the minimum defensible gate is
`build-test / build-and-test` plus `security-secrets / secrets`, with the
remaining jobs reviewed as security evidence.

## Security Evidence Workflow

The `dev` workflow should run on Pull Requests and can also be triggered
manually with `workflow_dispatch`. It provides one timeline with:

- build, tests, JaCoCo and PIT evidence review;
- Gitleaks secret scanning;
- SpotBugs, SonarCloud and CodeQL SAST;
- OWASP Dependency-Check and CycloneDX SBOM;
- runtime security tests, optional IAST readiness notes and OWASP ZAP baseline.

## Demonstration Checklist

1. Open repository settings.
2. Show branch protection for `main` or `develop`.
3. Show required Pull Request approval.
4. Show required status checks with the current `dev` job names.
5. Open a Pull Request and show the `dev` workflow running or completed.
