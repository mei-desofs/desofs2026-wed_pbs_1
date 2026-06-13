# GhostReport Coding Standards

These standards keep the codebase consistent and make security review easier.

## Naming

| Element | Convention | Example |
| --- | --- | --- |
| Controllers | `*Controller` | `AdminController` |
| Services | `*Service` | `FileStorageService` |
| Repositories | `*Repository` | `UserRepository` |
| DTO requests | `*Request` | `CreateUserRequest` |
| DTO responses | `*Response` | `AuthResponse` |
| Tests | `ClassOrFeatureTest` | `AdminAuthorizationTest` |
| Integration tests | `FeatureIntegrationTest` | `BackupServiceIntegrationTest` |
| Packages | lowercase by layer/domain | `com.ghostreport.service` |

Names should describe the business action. Prefer `verifyTrackingCode` over
`checkCode`, and `createEvidencePackage` over `makeZip`.

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

Security-sensitive domain primitives such as `SafeFilename`, `TrackingCode` and
`ReportDescription` stay in `domain`.

## Controllers

Controllers should:

- expose HTTP concerns only;
- validate request DTOs with `@Valid`;
- delegate business rules to services;
- avoid direct repository access;
- return DTOs or response records instead of JPA entities;
- avoid leaking stack traces or internal exception messages.

Protected endpoints must remain consistent with `SecurityConfig`.

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

## Tests

Test names should describe behavior:

```java
analystCannotAccessCaseAssignedToAnotherAnalyst()
rejectsExecutableUpload()
returnsTooManyRequestsAfterLimit()
```

Each security control described in documentation should have at least one of:

- unit test;
- integration test;
- pipeline artifact;
- ASVS evidence entry.

## Pull Request Rules

Every PR should:

- use `.github/pull_request_template.md`;
- receive at least one teammate approval;
- pass the CI build/tests/coverage workflow;
- include or reference security evidence when a security control changes;
- update ASVS or security documentation when a security claim changes.

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
- explain whether the workflow is blocking or evidence review;
- avoid changing backend code in documentation-only branches.

Documentation should link claims to one of:

- source code;
- automated tests;
- GitHub Actions artifacts;
- ASVS evidence;
- configuration files.
