# Code Review Guidelines

These guidelines define how GhostReport pull requests should be reviewed during
Sprint 2 and final delivery.

## Required Review Rules

| Rule | Requirement | Evaluation value |
| --- | --- | --- |
| Mandatory approval | At least one teammate must approve before merge. | Shows governance and review discipline. |
| CI required | Build, tests and coverage workflow must pass. | Demonstrates the pipeline running on PRs. |
| Security evidence | Security workflows must run or be manually triggered for evidence. | Addresses the Sprint 1 pipeline criticism. |
| No self-merge without review | Author cannot be the only reviewer. | Makes review meaningful. |
| Document residual risk | Unfixed SAST/SCA/DAST findings must be triaged. | Avoids unsupported security claims. |

## Secure Review Checklist

Reviewers should check:

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
| Request changes | A bug, security risk or missing evidence must be fixed. |
| Comment | Clarification or minor improvement that does not block merge. |

## Evidence to Capture

For the final presentation, keep screenshots or artifacts showing:

- PR checks running.
- CI build/tests/coverage result.
- SAST/SCA/secret scanning/DAST artifacts.
- Reviewer approval and branch protection status.
