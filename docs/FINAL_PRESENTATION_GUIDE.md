# Final Presentation Guide

Goal: recover Sprint 1 by showing that GhostReport now connects secure coding,
ASVS evidence, DevSecOps automation, pull request governance and practical
pipeline artifacts.

## Part 1 - Architecture

Show in 1 minute:

- Spring Boot backend with controllers, services, repositories, DTOs and domain
  primitives.
- Public reporter flow, analyst flow, auditor flow and admin flow.
- PostgreSQL for runtime, H2 for tests.
- Static frontend served by the same application.

Key message:

> The architecture separates HTTP/API contracts, business rules, persistence and
> security configuration.

## Part 2 - Security Implemented

Show in 2 minutes:

- `SecurityConfig` for RBAC and security headers.
- `JwtService` for signed/expiring tokens.
- `RateLimiterService` for abuse-sensitive endpoints.
- `TrackingCode`, `SafeFilename`, `ReportDescription` for invariants.
- DTO examples such as `ReportResponse`, `UserResponse`, `AuditLogResponse`.
- Upload validation and path traversal protections.

Key message:

> We do not expose domain entities through the API and we validate inputs at the
> DTO/domain/service boundaries.

## Part 3 - ASVS

Show in 1 minute:

- `docs/ASVS_LEVEL2_EVIDENCE.md`.
- Mapping for V2, V3, V4, V5, V7, V8, V9, V10, V11 and V13.
- Known gaps/future work.

Key message:

> We map each security claim to code, tests or pipeline artifacts and avoid
> claiming controls that are not implemented.

## Part 4 - Pipeline

Show in 2 minutes:

- GitHub Actions page.
- Workflows:
  - Gitleaks
  - CI build/tests/coverage
  - SpotBugs
  - Dependency-Check
  - CodeQL
  - CycloneDX
  - ZAP
  - PIT
- Artifacts tab for reports.

Key message:

> The Sprint 1 weakness was pipeline demonstration. In Sprint 2, every security
> activity produces visible evidence.

## Part 5 - Practical Demo

Use `docs/DEMO_SCRIPT.md`:

1. Make a harmless change.
2. Open a PR.
3. Show required review/checks.
4. Show workflows running or completed.
5. Open artifacts.
6. Link artifacts to ASVS evidence.

## Part 6 - Improvements Since Sprint 1

Emphasize:

- PR template and branch protection rules.
- Code review guidelines and coding standards.
- Stack security review.
- ASVS Level 2 mapping.
- Security configuration assessment.
- DTO correction for audit/security evidence endpoints.
- More explicit final demo evidence plan.

## Likely Questions and Answers

| Question | Answer |
| --- | --- |
| Does the pipeline really run on PRs? | Yes, workflows include `pull_request` triggers for `main` and `develop`; the demo PR shows the checks. |
| Are all security scanners blocking? | No. CI and confirmed secret leaks are blocking; SAST/SCA/DAST are evidence/manual triage during Sprint 2. |
| Do you expose entities? | Normal API responses use DTOs; audit/security evidence endpoints were corrected to response records. |
| Is this full ASVS Level 2? | It is a defensible coursework Level 2 baseline for implemented features, with documented gaps. |
| What is still missing? | MFA, authenticated DAST, malware scanning, distributed rate limiting, tamper-proof logs and migrations. |
