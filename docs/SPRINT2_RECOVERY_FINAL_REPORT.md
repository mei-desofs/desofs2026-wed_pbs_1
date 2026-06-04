# Sprint 2 Recovery Final Report

## Alterações realizadas

- Created a Sprint 2 GAP analysis tied to the professor's feedback.
- Added explicit OWASP ASVS Level 2 evidence mapping.
- Added a security configuration assessment for the current branch.
- Added technology stack security review with risks and mitigations.
- Added final presentation and practical demo guides.
- Added PR template, code review guidelines and branch protection rules.
- Strengthened coding standards with DTO, sanitization and PR governance rules.
- Corrected audit/security evidence endpoints to return DTO response records
  instead of JPA entities.
- Updated README and report update guide links.

## Ficheiros criados

- `.github/pull_request_template.md`
- `docs/ASVS_LEVEL2_EVIDENCE.md`
- `docs/BRANCH_PROTECTION_RULES.md`
- `docs/CODE_REVIEW_GUIDELINES.md`
- `docs/DEMO_SCRIPT.md`
- `docs/FINAL_PRESENTATION_GUIDE.md`
- `docs/SECURITY_CONFIGURATION_ASSESSMENT.md`
- `docs/SPRINT2_GAP_ANALYSIS.md`
- `docs/SPRINT2_RECOVERY_FINAL_REPORT.md`
- `docs/TECH_STACK_SECURITY_REVIEW.md`
- `ghostreport/src/main/java/com/ghostreport/dto/AuditLogResponse.java`
- `ghostreport/src/main/java/com/ghostreport/dto/SecurityAlertResponse.java`

## Ficheiros alterados

- `README.md`
- `CONTRIBUTING.md`
- `docs/CODING_STANDARDS.md`
- `docs/REPORT_SPRINT2_UPDATE_GUIDE.md`
- `ghostreport/src/main/java/com/ghostreport/controller/AdminController.java`
- `ghostreport/src/main/java/com/ghostreport/controller/AuditController.java`

## Melhorias implementadas

- PR governance is now demonstrable through a template, review checklist and
  branch protection rule documentation.
- Sprint 2 evidence is organized around the exact feedback: pipeline,
  Pull Requests, ASVS, stack vulnerabilities and secure coding.
- API evidence endpoints no longer expose persistence entities directly.
- ASVS claims now distinguish implemented controls from evidence/manual triage
  and future work.

## Commits realizados

- `aa44e5c security: return audit evidence through DTOs`
- `8e1ab53 docs: add pull request governance guidelines`
- `0aeb6c3 docs: add sprint 2 security evidence package`

## Evidências produzidas

- Maven tests: 106 tests passed, 0 failures, 0 errors.
- JaCoCo report generated and coverage checks met.
- GitHub Actions workflow YAML parsed successfully with PyYAML.
- SpotBugs local SAST command completed successfully.

## Problemas encontrados

- Existing untracked `.docx`/`.pdf` files were present in the repository root
  before this work and were left untouched.
- PowerShell does not include `ConvertFrom-Yaml` in this environment, so YAML
  validation used Python/PyYAML.
- The first local SpotBugs command failed because PowerShell parsed
  `-Dspotbugs.xmlOutput=true` incorrectly; rerunning with the argument quoted
  succeeded.
- Branch protection cannot be committed as code; it must be configured in
  GitHub settings and shown during the demo.

## Trabalho ainda pendente

- Configure branch protection in GitHub settings.
- Open a real PR and capture workflow artifacts/screenshots.
- Triage Dependency-Check, CodeQL, SpotBugs and ZAP outputs from GitHub Actions.
- Add authenticated DAST for role-specific flows if time allows.
- Add MFA, tamper-proof audit logs, distributed rate limiting, malware scanning
  and migrations as future hardening.

## Recomendações para a apresentação final

- Start with the Sprint 1 criticism: the pipeline was not demonstrated well.
- Show the PR first, then branch protection and required checks.
- Open Actions artifacts and map them to ASVS evidence.
- Show one secure coding example: DTO responses, domain invariants or RBAC.
- Be honest about evidence/manual triage and future work.
