# GhostReport Coding Standards

These standards are intended to reduce style drift during Sprint 2 and final
delivery. They favor consistency with the current Spring Boot codebase over
large refactors.

## Naming

| Element | Convention | Example |
| --- | --- | --- |
| Controllers | `*Controller` | `AdminController` |
| Services | `*Service` | `FileStorageService` |
| Repositories | `*Repository` | `UserRepository` |
| DTO requests | `*Request` | `CreateUserRequest` |
| DTO responses | `*Response` | `LoginResponse` |
| Tests | `ClassOrFeatureTest` | `AdminAuthorizationTest` |
| Integration tests | `FeatureIntegrationTest` | `BackupServiceIntegrationTest` |
| Packages | lowercase by layer/domain | `com.ghostreport.service` |

Names must describe the business action, not the implementation detail. Prefer
`verifyTrackingCode` over `checkCode`, and `createEvidencePackage` over
`makeZip`.

## Packages

Keep the current structure:

```text
controller
service
repository
model
dto
domain
security
config
exception
```

Do not create new top-level packages unless the feature clearly needs a new
boundary. Security-sensitive domain primitives such as `SafeFilename` and
`TrackingCode` should stay in `domain`.

## Controllers

Controllers should:

- expose HTTP concerns only;
- validate request DTOs with `@Valid`;
- delegate business rules to services;
- avoid direct repository access;
- return DTOs or response records instead of JPA entities;
- avoid leaking stack traces or internal exception messages.

Endpoint names should be stable and descriptive. Protected endpoints must be
consistent with `SecurityConfig`.

## Services

Services should:

- contain business rules and security checks that depend on domain state;
- use repositories through constructor injection;
- write audit logs for critical state changes;
- avoid returning JPA entities directly when a DTO is more appropriate;
- keep file-system logic in dedicated services.

## Errors and API Responses

Error messages should be useful but not revealing. Prefer:

```text
Invalid request
Access denied
Resource not found
Too many requests
```

Avoid exposing:

- SQL details;
- filesystem paths;
- stack traces;
- whether a tracking code exists;
- raw secrets or tokens.

## Comments

Use comments when they explain security intent or non-obvious trade-offs.
Avoid comments that repeat the method name or describe obvious Java syntax.

Good:

```java
// Keep the resolved path inside the configured storage directory.
```

Avoid:

```java
// Set the username field.
```

## Tests

Test names should describe behavior:

```java
analystCannotAccessCaseAssignedToAnotherAnalyst()
rejectsExecutableUpload()
returnsTooManyRequestsAfterLimit()
```

Use Given/When/Then structure inside tests:

```java
// given
// when
// then
```

Each security control mentioned in the report should have at least one of:

- unit test;
- integration test;
- pipeline artifact;
- ASVS evidence entry.

## Security Documentation Rule

Do not claim a control as implemented unless it exists in code and has evidence.
Use "planned" or "future work" for malware scanning, storage quotas, distributed
rate limiting, MFA, tamper-proof logs and full admin user lifecycle management
until those features are actually implemented.

## Pull Request Rules

Every PR should:

- use `.github/pull_request_template.md`;
- receive at least one teammate approval;
- pass the CI build/tests/coverage workflow;
- include or reference security evidence when a security control changes;
- update ASVS or security documentation when a security claim changes.

Branch names should be descriptive and use prefixes such as:

```text
feature/security-configuration-assessment
security/input-validation-hardening
docs/asvs-level2-evidence
ci/dependency-check-artifacts
```

## DTO and Sanitization Rules

- Do not expose entities directly from controllers.
- Use response records for immutable API responses when practical.
- Validate request DTOs with Bean Validation and service/domain rules.
- Trim and normalize user-controlled text before storing when the domain rule
  requires it.
- Sanitize audit and security event details before persistence.
- Never log passwords, JWTs, raw secrets or full uploaded file contents.

## Workflow and Documentation Standards

GitHub Actions workflows should:

- use clear stage-oriented names;
- include `workflow_dispatch` for manual evidence regeneration;
- use minimum required permissions;
- publish artifacts with stable names;
- explain whether the workflow is blocking or evidence/manual triage;
- avoid changing backend code in DevSecOps-only branches.

Documentation should link claims to one of:

- source code;
- automated tests;
- GitHub Actions artifacts;
- ASVS evidence;
- known limitations/future work.
