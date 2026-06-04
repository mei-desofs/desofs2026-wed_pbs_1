# Demo Script

Use this script for the final practical demonstration. Keep the demo short and
evidence-driven.

## 1. Create a Change

Make a small documentation or harmless code change on a feature branch.
For this local recovery branch, do not push unless the team is ready to open the
real GitHub PR.

Example:

```powershell
git status
git branch --show-current
```

## 2. Open a Pull Request

In GitHub:

- base branch: `develop` or `main`;
- compare branch: the team feature branch;
- use `.github/pull_request_template.md`.

Show that the PR template requires build, tests, DTOs, validation, errors, logs
and dependency/security checks.

## 3. Show Pipeline

Open the PR checks or the Actions tab.

Point out that workflows run on `pull_request`:

- `00 - Secret Scanning Gitleaks`
- `01 - CI Build, Tests and Coverage`
- `02A - SAST SpotBugs`
- `02B - SCA OWASP Dependency-Check`
- `02C - SAST CodeQL`
- `02D - SBOM CycloneDX`
- `03 - DAST OWASP ZAP Baseline`
- `04 - Mutation Testing PIT`

## 4. Show Build

Open `01 - CI Build, Tests and Coverage`.

Show:

- Java 17 setup.
- Maven compile step.
- PostgreSQL service container.

## 5. Show Tests

In the same workflow, show:

- Maven test execution.
- Surefire artifact: `ci-surefire-test-reports`.
- Security tests such as RBAC, JWT, uploads, error handling and configuration.

## 6. Show Coverage

Show:

- JaCoCo report generation.
- Artifact: `ci-jacoco-coverage-report`.
- Coverage threshold in `pom.xml`.

## 7. Show SAST

Open:

- `02A - SAST SpotBugs`
- `02C - SAST CodeQL`

Show:

- SpotBugs XML artifact.
- CodeQL Code Scanning result.

## 8. Show Dependency Check

Open `02B - SCA OWASP Dependency-Check`.

Show artifacts:

- `dependency-check-sca-html`
- `dependency-check-sca-json`
- `dependency-check-sca-xml`
- `dependency-check-sca-sarif`

Explain that findings are manually triaged because CVE scanners can produce
false positives.

## 9. Show Gitleaks

Open `00 - Secret Scanning Gitleaks`.

Show:

- repository-root scan;
- redacted JSON artifact;
- `.gitleaks.toml` placeholder allowlist.

## 10. Show DAST

Open `03 - DAST OWASP ZAP Baseline`.

Show:

- application starts at `http://localhost:8081`;
- ZAP runs in Docker;
- artifacts: HTML, JSON, XML and runtime log.

Be precise: this is passive unauthenticated baseline DAST.

## 11. Show Artifacts

Open workflow artifacts and map them to `docs/PIPELINE_ARTIFACTS.md`.

## 12. Show ASVS Documentation

Open:

- `docs/ASVS_LEVEL2_EVIDENCE.md`
- `docs/ASVS_EVIDENCE.md`

Show the mapping from ASVS requirement to code/test/pipeline evidence.

## 13. Show Branch Protection

Open GitHub repository settings and show the configured rules:

- pull request required;
- at least one approval;
- required status checks;
- stale approvals dismissed;
- force pushes disabled.

Use `docs/BRANCH_PROTECTION_RULES.md` as the expected configuration.

## 14. Conclude

Final message:

> Sprint 1 failed mainly in demonstrating automation. Sprint 2 connects secure
> coding, ASVS Level 2 evidence, PR governance and DevSecOps workflows that
> produce demonstrable artifacts.
