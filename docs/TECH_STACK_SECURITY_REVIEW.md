# Technology Stack Security Review

This review addresses the Sprint 1 feedback that the stack was presented
without discussing associated vulnerabilities and mitigations.

| Technology | Description | Typical risks | Existing mitigations | Recommended improvements |
| --- | --- | --- | --- | --- |
| Spring Boot 3.5.x | Main backend framework. | Misconfiguration, outdated transitive dependencies, actuator exposure if added later. | Maven-managed dependencies, SCA workflow, no actuator endpoints in current scope. | Keep patch versions current and review Dependency-Check results. |
| Spring Security | Authentication, authorization and headers. | Broken access control, disabled CSRF misuse, weak headers. | Stateless API, centralized RBAC, JWT filter, security headers, RBAC tests. | Reassess CSRF if browser cookie sessions are introduced. |
| JWT / HS256 | Stateless API token. | Weak secrets, long-lived tokens, no revocation, token leakage. | Secret length validation, env vars, expiry, invalid JWT alerts, tests. | Add revocation/rotation and MFA for privileged users. |
| PostgreSQL 16 | Main database in dev/CI/Docker. | Weak credentials, SQL injection, schema drift, excessive privileges. | JPA parameterization, env credentials, production-like `ddl-auto=validate`, Docker healthcheck. | Add migrations and least-privilege production DB user documentation. |
| H2 | Test database. | Accidental console exposure or production use. | H2 console disabled outside test/dev configs; tests isolate paths. | Keep H2 strictly test-only. |
| Maven | Build/dependency management. | Vulnerable plugins/dependencies, supply-chain risk. | Dependency-Check, CycloneDX SBOM, pinned plugins. | Regularly update plugins and triage CVEs. |
| GitHub Actions | CI/CD and DevSecOps evidence. | Overbroad permissions, secret exposure in logs, untrusted PR execution risks. | `contents: read` by default, artifacts, GitHub secrets, Gitleaks. | Protect branches and review workflow changes carefully. |
| OWASP Dependency-Check | Dependency vulnerability scanner. | False positives/false negatives, NVD availability, feed instability. | Evidence/manual triage mode, multiple report formats, optional NVD API key. | Add triage notes for findings used in the final report. |
| SpotBugs | Java static analysis. | Limited rule coverage, false positives. | SAST artifact in pipeline. | Add PMD/Checkstyle if time allows; triage findings. |
| CodeQL | Semantic SAST. | Requires GitHub code scanning access; findings need context. | Dedicated workflow with Java analysis. | Review alerts during PR demo. |
| Gitleaks | Secret scanning. | Allowlist mistakes, cannot rotate leaked secrets. | Repository-root scan and narrow placeholder allowlist. | Keep allowlist small and rotate real leaks immediately. |
| OWASP ZAP | Baseline DAST scanner. | Unauthenticated baseline misses protected flows; active scan can be intrusive. | Passive baseline scan against live app; artifacts uploaded. | Add authenticated contexts for ADMIN/ANALYST/AUDITOR. |
| Docker / Compose | Local container execution. | Root containers, writable filesystem, leaked env vars. | Non-root app user, read-only app container, named volumes, `no-new-privileges`. | Add image scanning and production TLS/reverse-proxy setup. |
| Static HTML/CSS/JS frontend | Simple browser interface served by Spring Boot. | XSS, unsafe inline scripts, weak CSP. | CSP and static same-origin serving. | Remove inline scripts/styles to strengthen CSP. |
| Lombok | Compile-time boilerplate generation. | Hidden generated methods can obscure review. | Limited use through Maven annotation processing. | Prefer explicit records/DTOs for API contracts. |

## Pipeline Evidence

Stack risks are monitored through:

- Dependency-Check reports for known vulnerable dependencies.
- CycloneDX SBOM for dependency inventory.
- SpotBugs and CodeQL for code-level vulnerabilities.
- Gitleaks for accidental secrets.
- ZAP for runtime/browser-facing findings.

## Presentation Message

Use this wording:

> We reviewed the risks of each technology and mapped them to mitigations in
> code and in the pipeline. Some controls are implemented as blocking gates,
> while others intentionally produce evidence for manual triage during Sprint 2.
