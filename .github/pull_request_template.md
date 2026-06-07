## Summary

- What changed:
- Why it changed:
- Security impact:

## Acceptance Criteria

- [ ] Build passes.
- [ ] Automated tests pass.
- [ ] Security-relevant tests or evidence were added/updated.
- [ ] API responses use DTOs and do not expose JPA/domain entities directly.
- [ ] Inputs are validated with Bean Validation and/or domain primitives.
- [ ] Errors avoid stack traces, internal paths, SQL details, secrets and tokens.
- [ ] Logs/audit entries avoid passwords, JWTs, raw secrets and sensitive payloads.
- [ ] Dependency or workflow changes were reviewed for security impact.
- [ ] Documentation/ASVS evidence was updated when the security claim changed.

## Reviewer Checklist

- [ ] At least one reviewer approved the PR.
- [ ] Reviewer checked authorization rules for new/changed endpoints.
- [ ] Reviewer checked validation, sanitization and safe error handling.
- [ ] Reviewer checked that the DevSecOps workflows produced evidence artifacts.
- [ ] Any residual risk is documented as accepted risk or project scope boundary.
