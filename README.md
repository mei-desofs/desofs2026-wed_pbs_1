# GhostReport

GhostReport is a Spring Boot web application for anonymous reporting, internal
case analysis and audit evidence management. The project was developed for the
DESOFS secure software development coursework.

## Main Capabilities

- Anonymous report submission.
- Tracking code based report verification.
- Evidence upload with file validation and safe storage.
- JWT based authentication for internal users.
- Role based access control for `ADMIN`, `ANALYST` and `AUDITOR`.
- Analyst case ownership controls.
- Audit logs and security alerts.
- Evidence package generation for closed cases.
- Backup generation and integrity verification.
- DevSecOps evidence through GitHub Actions.

## Current Role Model

| Role | Implemented capabilities |
| --- | --- |
| Anonymous reporter | Submit reports, verify tracking codes and upload evidence. |
| Analyst | View eligible cases, claim cases, update assigned cases and generate evidence packages for closed cases. |
| Auditor | View audit/security evidence and verify evidence packages and backups. |
| Admin | Create/list/activate/deactivate users, view audit/security information and manage backup operations. |

Admin user management is intentionally described as basic lifecycle management
in the current implementation. Editing user details, deleting users, changing
roles and password resets are planned future hardening work.

## Security Controls

- Stateless JWT authentication.
- Login rate limiting and brute-force security alerts.
- Inactive users are blocked from login.
- BCrypt password hashing.
- Centralized Spring Security authorization rules.
- Domain validation for tracking codes, report descriptions and filenames.
- Upload restrictions for size, extension, MIME type and file signatures.
- Path traversal and zip slip protections.
- In-memory rate limiting for login and public abuse-sensitive flows.
- SHA-256 hashes for attachments, evidence packages and backup manifests.
- Security headers configured in Spring Security.
- Audit logs for critical operations.

Known limitations are documented in the report and ASVS evidence: malware
scanning, storage quotas, tamper-proof audit logs and distributed rate limiting
are not implemented yet.

## Running Locally

From the Spring Boot module:

```powershell
cd ghostreport
$env:SPRING_PROFILES_ACTIVE="dev"
$env:DB_PASSWORD="user"
$env:JWT_SECRET="dev-local-secret-with-at-least-32-chars"
.\mvnw.cmd spring-boot:run
```

The application uses port `8081` by default.

For local Docker execution with PostgreSQL:

```powershell
$env:DB_PASSWORD="<local-database-password>"
$env:JWT_SECRET="<random-secret-at-least-32-characters>"
docker compose up --build
```

## Test and Evidence Commands

```powershell
cd ghostreport
.\mvnw.cmd test
.\mvnw.cmd test jacoco:report
.\mvnw.cmd -DskipTests compile com.github.spotbugs:spotbugs-maven-plugin:4.8.6.6:spotbugs -Dspotbugs.xmlOutput=true
.\mvnw.cmd org.owasp:dependency-check-maven:12.1.0:check -Dformat=ALL -DossindexAnalyzerEnabled=false -DfailOnError=false -DfailBuildOnCVSS=11
```

## Documentation

- [Coding standards](docs/CODING_STANDARDS.md)
- [Contribution guide](CONTRIBUTING.md)
- [Secure installation](docs/SECURE_INSTALLATION.md)
- [Security assessment](docs/SECURITY_ASSESSMENT.md)
- [IAST and runtime security instrumentation](docs/IAST_RUNTIME_SECURITY.md)
- [DevSecOps pipeline evidence](docs/DEVSECOPS_PIPELINE.md)
- [ASVS evidence mapping](docs/ASVS_EVIDENCE.md)
- [Phase 2 evidence folder](Deliverables/Phase%202/Evidence/README.md)

## Authors

- Alexandre Vieira
- Barbara Silva
- Sofia Marques
