# Technology Stack Security Review

This review maps GhostReport's technology stack to typical security risks,
implemented mitigations and pipeline evidence.

| Technology | Description | Typical risks | Existing mitigations |
| --- | --- | --- | --- |
| Spring Boot 3.5.x | Main backend framework | Misconfiguration, outdated dependencies, accidental endpoint exposure | Maven-managed dependencies, SCA workflow, explicit security configuration |
| Spring Security | Authentication, authorization and headers | Broken access control, weak headers, incorrect session assumptions | Stateless JWT, centralized RBAC, security headers and authorization tests |
| JWT / HS256 | Stateless API token | Weak secrets, token leakage, long-lived tokens | Secret length validation, expiry, role validation and invalid-token alerts |
| PostgreSQL 16 | Runtime database | Weak credentials, schema drift, excessive privileges | Environment credentials, production-like `ddl-auto=validate`, Docker healthcheck |
| H2 | Test database | Accidental non-test use | Restricted to automated tests through profile configuration |
| Maven | Build and dependency management | Vulnerable dependencies/plugins, supply-chain risk | Dependency-Check, CycloneDX SBOM and pinned security tooling |
| GitHub Actions | CI/CD automation | Overbroad permissions, secret exposure in logs, unclear evidence | Minimal permissions, GitHub secrets, stable artifact uploads |
| OWASP Dependency-Check | Dependency vulnerability scanner | CVE false positives/negatives, external feed availability | Evidence artifacts in multiple formats and manual review |
| SpotBugs | Java static analysis | Limited rule coverage, contextual findings | SAST workflow and report artifact |
| CodeQL | Semantic static analysis | Findings require exploitability review | Dedicated CodeQL workflow, GitHub Code Scanning evidence and archiveable run summary |
| Gitleaks | Secret scanning | Allowlist mistakes, secret rotation requirements | Repository-root scan and narrow placeholder allowlist |
| OWASP ZAP | Baseline DAST scanner | Unauthenticated baseline does not cover all protected flows | Passive baseline against a live CI runtime and artifacts |
| Contrast Java Agent | Optional JVM IAST agent | Requires tenant configuration and CI secrets | Workflow readiness checks and runtime security evidence |
| Docker / Compose | Local container execution | Root containers, writable filesystem, leaked env vars | Non-root app user, read-only app container, named volumes, `no-new-privileges` |
| Static HTML/CSS/JS | Browser interface served by Spring Boot | XSS, weak CSP, unsafe inline code | Same-origin serving, security headers and CSP baseline with `form-action 'self'`; inline code remains a documented baseline item |
| Angus Activation / Jakarta Activation | Transitive activation API implementation pulled by JAXB/Hibernate paths | Dependency-Check may flag CVEs or ecosystem risk when transitive versions lag | Managed through Spring Boot dependency management; triage against Dependency-Check report before adding an override |
| Lombok | Compile-time code generation | Generated methods can obscure review | Limited use; API contracts prefer explicit DTOs/records |

## Pipeline Evidence

Stack risks are monitored through:

- Dependency-Check reports for known vulnerable dependencies.
- CycloneDX SBOM for dependency inventory.
- SpotBugs and CodeQL for code-level vulnerability patterns.
- Gitleaks for accidental secrets.
- ZAP for runtime-facing HTTP findings.
- Runtime security/IAST readiness workflow for security-focused runtime behavior.

## Current Triage Notes

- CodeQL primary findings are stored in GitHub Code Scanning. The workflow also
  uploads `sast-codeql-evidence-summary` so the run can be archived locally.
- PIT mutation testing is evidence review. A non-zero PIT exit code is preserved
  in the artifact and reviewed manually instead of blocking all Sprint 2
  evidence.
- Gitleaks JSON output of `[]` is valid evidence that no leaks were detected in
  the scanned repository content.
- The frontend CSP still allows `'unsafe-inline'` for current static assets.
  `form-action 'self'` is enforced, and removal of inline script/style is a
  future hardening item.
- `org.eclipse.angus:angus-activation` is treated as a transitive dependency
  triage item. Do not suppress or override it without linking the
  Dependency-Check finding, current resolved version and compatibility impact.

## Scope Boundaries

Security tooling findings are reviewed with application context before being
treated as confirmed vulnerabilities. This keeps the pipeline useful for
assessment while avoiding unsupported claims.
