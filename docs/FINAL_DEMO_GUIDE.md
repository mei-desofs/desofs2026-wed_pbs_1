# Final Demo Guide

This guide explains how to demonstrate GhostReport's secure development
evidence during the final assessment.

## 1. Pull Request and Governance

Open a Pull Request and show:

- the PR template checklist;
- required review approval;
- branch protection settings;
- required CI status checks.

Use `docs/BRANCH_PROTECTION_RULES.md` and
`docs/CODE_REVIEW_GUIDELINES.md` as the project rules.

## 2. Pipeline Execution

Open GitHub Actions and show the numbered workflow sequence:

- `00 - Secret Scanning Gitleaks`
- `01 - CI Build, Tests and Coverage`
- `02A - SAST SpotBugs`
- `02B - SCA OWASP Dependency-Check`
- `02C - SAST CodeQL`
- `02D - SBOM CycloneDX`
- `03 - DAST OWASP ZAP Baseline`
- `04 - Runtime Security Evidence and IAST Readiness`
- `05 - Mutation Testing PIT`

Explain that build/tests/coverage and secret scanning are gates, while the
security analysis workflows produce artifacts for review. The numbered
workflows are the presentation timeline; they remain separate so each scanner
can be rerun and archived independently.

## 3. Artifacts

Show the artifacts created by each workflow:

- Surefire test reports.
- JaCoCo coverage report.
- SpotBugs XML report.
- CodeQL Code Scanning alerts and `sast-codeql-evidence-summary`.
- Dependency-Check reports.
- CycloneDX SBOM.
- Gitleaks JSON report.
- ZAP HTML/JSON/XML reports.
- Runtime security evidence and optional IAST readiness notes.
- PIT mutation testing report or fallback PIT triage summary.

Downloaded GitHub Actions artifacts can be copied into
`Deliverables/Phase 2/Evidence` with:

```powershell
.\scripts\collect-evidence.ps1
```

## 4. ASVS Evidence

Open `docs/ASVS_LEVEL2_EVIDENCE.md` and show how controls map to:

- source code;
- automated tests;
- GitHub Actions workflows;
- generated artifacts;
- configuration files.

## 5. Secure Coding Evidence

Use one or two concrete examples:

- `SecurityConfig` for RBAC and security headers.
- `JwtService` for signed and expiring tokens.
- `TrackingCode`, `SafeFilename` and `ReportDescription` for domain invariants.
- `AuditLogResponse` and `SecurityAlertResponse` as DTO response records.
- `SecurityConfigurationValidator` for fail-fast configuration validation.

## 6. Scope Boundaries

When discussing boundaries, keep the wording technical:

> The project implements the required secure development evidence for the
> current coursework scope. Production hardening items such as external SIEM,
> distributed token revocation, advanced deployment TLS management and authenticated DAST
> are documented as next-step operational hardening.

Also state the exact evidence boundaries:

- runtime security tests exist in every Stage 04 run;
- external IAST telemetry exists only if the optional Contrast agent is
  configured;
- PIT is evidence review, not a blocking quality gate;
- CodeQL evidence is primarily GitHub Code Scanning, with an archiveable summary
  artifact;
- CSP no longer allows `unsafe-inline` in code; the existing ZAP artifact is
  pre-remediation and should be regenerated before the final presentation.
