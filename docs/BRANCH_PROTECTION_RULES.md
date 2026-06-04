# Branch Protection Rules

GitHub branch protection is configured in the repository settings, not in code.
This file defines the rules GhostReport should apply to `main` and `develop`
for Sprint 2 evidence.

## Required Rules

| Setting | Required value | Rationale |
| --- | --- | --- |
| Require pull request before merging | Enabled | Prevents direct merges without review. |
| Required approvals | 1 minimum | Satisfies mandatory code review governance. |
| Dismiss stale approvals | Enabled | Forces re-review after meaningful changes. |
| Require review from code owners | Optional unless code owners are added | Useful future hardening. |
| Require status checks | Enabled | Ensures pipeline evidence before merge. |
| Required CI check | `01 - CI Build, Tests and Coverage` | Blocks broken build/tests/coverage. |
| Require branches to be up to date | Enabled | Reduces merge-time regressions. |
| Require conversation resolution | Enabled | Ensures review comments are handled. |
| Restrict who can push | Recommended for `main` | Reduces accidental direct changes. |
| Allow force pushes | Disabled | Protects history and evidence. |
| Allow deletions | Disabled | Protects protected branches. |

## Security Workflows

The following workflows should run on pull requests and be used as evidence:

- `00 - Secret Scanning Gitleaks`
- `01 - CI Build, Tests and Coverage`
- `02A - SAST SpotBugs`
- `02B - SCA OWASP Dependency-Check`
- `02C - SAST CodeQL`
- `02D - SBOM CycloneDX`
- `03 - DAST OWASP ZAP Baseline`
- `04 - Mutation Testing PIT`

Only CI is recommended as a hard blocking gate in the current coursework phase.
Security scanners are evidence/manual triage gates unless the finding is a
confirmed secret leak.

## Demo Steps

During the final presentation:

1. Open repository settings.
2. Show branch protection for `main` or `develop`.
3. Show required pull request approval.
4. Show required status checks.
5. Open a PR and show the workflows running or completed.
