# Sprint 2 GAP Analysis

This analysis was prepared after reviewing the repository, documentation,
GitHub Actions workflows, tests, configuration, DTOs/entities, authentication,
authorization and ASVS evidence on branch
`feature/security-configuration-assessment`.

## Estado Atual

| Item | Estado | Prioridade | Impacto na avaliação |
| --- | --- | --- | --- |
| Pipeline DevSecOps demonstrável | Concluído/forte. Workflows cover build, tests, JaCoCo, SpotBugs, CodeQL, Dependency-Check, CycloneDX, Gitleaks, ZAP and PIT. | Crítico | Directly addresses the professor's main Sprint 1 criticism. High positive impact if demonstrated live through a PR. |
| PR governance | Parcial before this sprint; now documented with PR template, review guidelines and branch protection rules. | Crítico | Addresses missing PR rules and code review process. High impact because it is easy to demonstrate. |
| ASVS Level 2 evidence | Parcial/strong. Existing `ASVS_EVIDENCE.md` was broad; `ASVS_LEVEL2_EVIDENCE.md` now maps Level 2 topics more explicitly. | Alto | Shows requirements-to-implementation traceability requested by the professor. |
| Security configuration | Strong baseline. Profiles, env vars, fail-fast JWT validation, seed-user controls, Docker hardening and secret scanning exist. | Alto | Supports the current branch objective and Sprint 2 recovery. |
| DTO usage | Mostly implemented. Reports, users, backups, case packages and attachments use DTOs. Audit/admin security evidence endpoints previously returned entities and were corrected. | Alto | Directly maps to feedback about not exposing domain objects through the API. |
| Domain invariants | Partial/strong. `TrackingCode`, `SafeFilename`, `ReportDescription`, JPA constraints and service rules enforce key invariants. | Alto | Good evidence for secure coding discussion; remaining entities are mutable JPA models. |
| Validation and sanitization | Strong baseline. Bean Validation, domain primitives, file validation, safe path checks and sanitized logs exist. | Alto | Strong relation to ASVS V5 and professor feedback. |
| Authentication | Strong baseline. BCrypt, inactive-user checks, JWT signing/expiry/role validation and login rate limiting exist. | Alto | Supports ASVS V2/V3 and practical demo. |
| Authorization | Strong baseline. Centralized RBAC and analyst ownership tests exist. | Alto | Supports ASVS V4 and secure API claims. |
| Evidence of stack vulnerabilities | Previously weak; now covered by `TECH_STACK_SECURITY_REVIEW.md` and SCA/SBOM workflows. | Alto | Directly addresses the professor's comment about technology stack vulnerabilities. |
| Error handling | Strong baseline. Generic errors and no stack traces; tests exist. | Médio | Useful ASVS evidence; moderate impact unless demonstrated with tests. |
| Logging and monitoring | Implemented baseline. Audit logs and security alerts exist, but are not tamper-proof. | Médio | Positive evidence, with honest residual risk. |
| DAST depth | Partial. ZAP baseline exists but is passive and unauthenticated. | Médio | Demonstrable pipeline evidence; lower than authenticated DAST but acceptable if described honestly. |
| Dependency vulnerability gates | Partial. Dependency-Check is evidence/manual triage, not a blocking vulnerability policy. | Médio | Good evidence for Sprint 2; future hardening remains. |
| MFA | Ausente. Not implemented. | Baixo | ASVS/auth future work; do not claim Level 2 MFA coverage. |
| Malware scanning | Ausente. Upload validation exists, but no antivirus scanner. | Baixo | Mention as future work to avoid overclaiming. |
| Distributed rate limiting | Ausente. Current limiter is in-memory. | Baixo | Relevant for production maturity, not central to academic demo. |
| Tamper-proof audit logs | Ausente. Logs are stored in DB without hash chaining/WORM storage. | Baixo | Future work; mention honestly. |

## What Is Concluded

- Spring Boot application builds as a Maven module.
- Automated tests cover authentication, authorization, RBAC, uploads, backups,
  configuration, error handling, JWT, rate limiting and audit/security events.
- JaCoCo is configured with baseline thresholds.
- GitHub Actions workflows produce demonstrable security artifacts.
- Configuration uses environment variables and fail-fast validation.
- Secret scanning exists through Gitleaks.
- DTOs are used for normal API responses, and audit/security endpoints were
  aligned to DTO responses in this branch.
- Documentation now covers PR rules, code review, ASVS Level 2, stack risks,
  final presentation and demo script.

## Partially Implemented

- ASVS Level 2: many controls have evidence, but MFA, tamper-proof logs,
  distributed rate limiting, malware scanning and authenticated DAST remain
  future work.
- DAST: baseline/passive unauthenticated ZAP is implemented, but authenticated
  role-specific scanning is not.
- SCA: Dependency-Check and SBOM exist, but CVE findings require manual triage.
- Runtime monitoring: audit logs and security alerts exist, but no SIEM/WORM
  integration.

## Absent

- MFA.
- Authenticated ZAP context.
- Real malware/antivirus scanning.
- Flyway/Liquibase migrations.
- Distributed rate limiting.
- Tamper-proof audit log storage.
- Formal GitHub branch protection cannot be committed as code; it must be set
  in repository settings and shown during the demo.

## Incorrect or Misaligned

- Before this recovery work, audit/admin evidence endpoints returned JPA
  entities directly. This was misaligned with the professor's DTO guidance and
  has been corrected with immutable response records.
- The project had pipeline documentation, but lacked a single GAP analysis and
  final presentation/demo narrative tied exactly to the professor's feedback.
- Existing ASVS evidence was useful but not framed explicitly as Level 2
  presentation evidence.

## Sprint 2 Priority

The highest-scoring presentation path is:

1. Show a PR and branch protection/review rules.
2. Show workflows running and artifacts generated.
3. Show ASVS Level 2 evidence mapped to code/tests/pipeline.
4. Show secure configuration fail-fast tests.
5. Show one code example for DTOs/invariants/authorization.
