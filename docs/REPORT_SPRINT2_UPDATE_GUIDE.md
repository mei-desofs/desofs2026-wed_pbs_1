# Sprint 2 Report Update Guide

Use this guide when updating the final report. It keeps the wording aligned
with the code and avoids overclaiming features that are not implemented yet.

## Sections to Update

| Report section | Required update |
| --- | --- |
| Admin functionality | Say that ADMIN can create/list/activate/deactivate users, view audit/security data and manage backup operations. Do not claim edit/delete/role changes/password reset. |
| DevSecOps pipeline | Describe separate workflows: CI tests/coverage, SpotBugs, Dependency-Check, Gitleaks and ZAP. |
| Pipeline flow | Explain the sequence: secret scanning, CI build/tests/coverage, SAST/SCA, DAST and evidence collection. |
| ASVS | Reference `docs/ASVS_EVIDENCE.md` and the Sprint 2 tracker. |
| Security controls | Reference `docs/SECURITY_ASSESSMENT.md`, `docs/SECURITY_CONFIGURATION_ASSESSMENT.md` and `docs/ASVS_LEVEL2_EVIDENCE.md` for control-to-code/test mapping. |
| Testing | Include Maven tests, JaCoCo coverage, security tests and pipeline artifacts. |
| DAST | State that GhostReport runs on `localhost:8081` on the GitHub runner and ZAP runs in Docker with host networking. |
| Limitations | Keep malware scanning, quotas, MFA, tamper-proof logs, distributed rate limiting and authenticated DAST as future work. |

## Correct Wording

Use:

> The administrator can create and list users, access administrative audit and
> security information, activate or deactivate users, and execute protected
> backup operations.

Avoid:

> The administrator fully manages users and permissions.

Use:

> Login protection includes generic authentication errors, rate limiting for
> repeated failed attempts, audit logging for inactive-user login attempts and
> security alerts for brute-force patterns.

Avoid:

> The authentication system has enterprise-grade account protection.

Use:

> The pipeline produces SAST, SCA, secret scanning and DAST evidence for manual
> triage.

Avoid:

> The pipeline blocks every vulnerable dependency automatically.

Use:

> Upload security is based on validation of filename, extension, MIME type,
> magic bytes, size and storage path.

Avoid:

> Uploaded files are scanned for malware.

## Evidence to Include

- GitHub Actions run summary screenshots.
- Artifact list screenshots for CI, SpotBugs, Dependency-Check, Gitleaks and ZAP.
- Pipeline flow diagram from `docs/PIPELINE_FLOW.md`.
- JaCoCo coverage screenshot.
- Maven test success screenshot.
- ASVS tracker screenshot or export.
- Postman screenshots for key endpoints when useful.

## Sprint 1 to Sprint 2 Narrative

Frame Sprint 2 as consolidation and hardening:

1. Sprint 1 implemented the core security mechanisms.
2. Sprint 2 organizes evidence, strengthens CI visibility and aligns ASVS,
   report claims and implemented code.
3. Remaining production-grade controls are listed as future work rather than
   claimed as complete.

## DevSecOps Flow Wording

Use:

> The pipeline strategy follows a staged DevSecOps flow: repository secret
> scanning, build/tests/coverage, SAST/SCA evidence, baseline DAST and artifact
> collection for ASVS and report evidence.

Avoid:

> All security pipelines fully block vulnerable code.
