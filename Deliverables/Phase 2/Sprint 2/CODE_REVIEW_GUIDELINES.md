# Code Review Guidelines

These guidelines define how GhostReport Pull Requests are reviewed.

## Required Review Rules

| Rule | Requirement |
| --- | --- |
| Mandatory approval | At least one teammate approves before merge. |
| CI required | Build, tests and coverage workflow passes. |
| Security evidence | Relevant security workflows run or are manually triggered. |
| No self-merge without review | The author is not the only reviewer. |
| Finding review | SAST, SCA, DAST and IAST findings are reviewed before claims are made. |

## Secure Review Checklist

Reviewers check:

- Controllers expose DTOs, not entities.
- Request DTOs use `@Valid` and validation annotations.
- Domain rules are enforced by domain primitives or services.
- Role checks match `SecurityConfig`.
- Analyst ownership checks remain enforced in services.
- File handling uses safe names, normalized paths and type validation.
- Error responses do not reveal stack traces, SQL, filesystem paths or secrets.
- Audit logs and security alerts avoid passwords, JWTs and raw payloads.
- New dependencies are justified and covered by SCA/SBOM evidence.
- Workflow changes keep minimum permissions and upload useful artifacts.

## Review Outcomes

| Outcome | Meaning |
| --- | --- |
| Approve | Requirements and security checks are satisfied. |
| Request changes | A bug, security risk or unsupported claim must be corrected. |
| Comment | Clarification or minor improvement that does not block merge. |

## Evidence to Capture

For assessment, keep screenshots or artifacts showing:

- PR checks running.
- CI build/tests/coverage result.
- SAST/SCA/secret scanning/DAST/IAST artifacts.
- Reviewer approval and branch protection status.
