# DevSecOps Workflow Strategy

GhostReport uses a lightweight DevSecOps workflow designed for an academic
delivery while preserving realistic secure software development practices.

## Objectives

- provide repeatable validation on push and pull request;
- keep each security activity understandable and auditable;
- generate artifacts that support the ASVS tracker and final report;
- avoid overcomplicating the project before final delivery.

## Validation Layers

| Layer | Tooling | Purpose |
| --- | --- | --- |
| Repository hygiene | Gitleaks | Detect hardcoded secrets. |
| Build and tests | Maven, Surefire, JaCoCo | Validate functionality and coverage. |
| Static analysis | SpotBugs | Detect suspicious Java code patterns. |
| Dependency analysis | OWASP Dependency-Check | Identify vulnerable dependencies. |
| Dynamic analysis | OWASP ZAP Baseline | Inspect runtime behavior from the outside. |

## Pull Request Expectations

For Pull Requests, reviewers should check:

- CI build/tests completed successfully;
- security workflows produced artifacts;
- new security claims are supported by code, tests or evidence;
- known limitations are documented instead of overstated;
- report and ASVS updates match the implemented behavior.

## Sprint 2 Operating Mode

| Activity | Mode |
| --- | --- |
| Build and tests | Blocking |
| Secret scanning | Blocking for confirmed leaks |
| Coverage | Evidence |
| SpotBugs | Evidence/manual triage |
| Dependency-Check | Evidence/manual triage |
| ZAP baseline | Evidence/manual triage |

This mode is intentionally conservative. It creates strong evidence without
blocking the team on findings that require manual analysis, such as dependency
false positives or baseline DAST hardening warnings.

## Future Hardening

Future iterations can add:

- coverage thresholds;
- dependency severity gates after CVE triage rules are defined;
- authenticated DAST;
- scheduled nightly security workflows;
- CodeQL;
- signed release artifacts;
- stronger branch protection rules.
