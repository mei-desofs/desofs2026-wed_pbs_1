# Software Composition Analysis Triage

Original source report: `Deliverables/Phase 2/Evidence/sca/dependency-check-sca-json/dependency-check-report.json`.
Post-remediation source report: `Deliverables/Phase 2/Evidence/sca/dependency-check-post-remediation/dependency-check-report.json`.

The previous Dependency-Check evidence reported 44 vulnerabilities, including
critical and high findings. This triage records the remediation decision for
the affected dependency families. A post-remediation Dependency-Check run on
2026-06-04 completed successfully and now reports two remaining findings:
`angus-activation-2.0.3` and `hibernate-validator-8.0.3.Final`.

| Component | Reported issue | Previous version | Action | Status | Evidence / residual risk |
| --- | --- | ---: | --- | --- | --- |
| Spring Boot parent and starters | Critical/high CVEs reported against Spring Boot artifacts | 3.5.13 | Upgraded Spring Boot parent to 3.5.14, the latest stable 3.5.x patch found by Maven metadata. | Fixed by post-remediation scan | Avoided 4.1.0-RC1 because it is a release candidate and too risky for Sprint 2. |
| Tomcat embed core/websocket/el | Critical/high Tomcat CVEs | 10.1.53 | Overrode Boot-managed Tomcat to 10.1.55, latest stable 10.1.x from Maven metadata. | Fixed by post-remediation scan | No Tomcat finding remains in the local post-remediation Dependency-Check report. |
| PostgreSQL JDBC | CVE affecting versions before 42.7.11 | 42.7.10 | Set `postgresql.version` to 42.7.11. | Fixed by post-remediation scan | No PostgreSQL JDBC finding remains in the local post-remediation Dependency-Check report. |
| Log4j API / log4j-to-slf4j | Medium CVEs reported for Log4j API 2.24.3 | 2.24.3 | Set `log4j2.version` to 2.26.0, latest stable 2.x line observed in Maven metadata. | Fixed by post-remediation scan | Application uses Spring Boot logging through SLF4J/Logback; no Log4j finding remains in the local post-remediation report. |
| Spring Framework core/web | CVE-2026-41840, CVE-2026-41841, CVE-2026-41842, CVE-2026-41843, CVE-2026-41850, CVE-2026-41851 | 6.2.18 | Set `spring-framework.version` to 6.2.19, the compatible 6.2.x patch available while Spring Boot parent remains 3.5.14. | Fixed locally; pending CI SCA confirmation | Avoided Spring Framework 7.x / Spring Boot 4.x because that would be a major stack change for Sprint 2. |
| Angus Activation | CVE-2025-7962 | 2.0.3 | No stable compatible upgrade selected; Maven metadata only showed 2.1.0-M1 as newer. | Accepted residual risk / applicability review | Transitive dependency via JAXB/Hibernate. GhostReport does not implement email/SMTP attachment processing. Revisit if a stable 2.1.x release is available. |
| Hibernate Validator | CVE-2025-15104 | 8.0.3.Final | No direct upgrade selected in this phase. | Accepted residual risk / applicability review | Managed by Spring Boot validation stack. Upgrade to 9.x may imply Jakarta Validation stack changes; keep under manual triage and reassess with future Spring Boot patches. |

## Current Code Scanning Review

Local dependency-tree evidence shows:

```text
org.springframework.boot:spring-boot-starter-data-jpa:3.5.14
\- org.hibernate.orm:hibernate-core:6.6.49.Final
   \- org.glassfish.jaxb:jaxb-runtime:4.0.6
      \- org.eclipse.angus:angus-activation:2.0.3

org.springframework.boot:spring-boot-starter-validation:3.5.14
\- org.hibernate.validator:hibernate-validator:8.0.3.Final
```

The two current Dependency-Check code scanning alerts are therefore not direct
application dependencies. They are handled with a time-bounded suppression file
at `ghostreport/owasp-dependency-check-suppressions.xml`:

- `CVE-2025-15104`: NVD describes The Nu Html Checker (vnu), not Hibernate
  Validator. Hibernate Validator 8.0.3.Final is the Spring Boot 3.5.14-managed
  Jakarta Bean Validation implementation.
- `CVE-2025-7962`: NVD describes SMTP injection in Jakarta Mail / Angus SMTP.
  GhostReport does not depend on `org.eclipse.angus:smtp` and does not send
  email. `angus-activation` is present only as a JAXB/Hibernate runtime
  transitive dependency.

These suppressions are not a replacement for future maintenance. Reassess them
before the `until` date in the suppression XML, after Spring Boot dependency
management changes, or if GhostReport adds email/SMTP processing.

## Verification Plan

1. Run `./mvnw clean test`.
2. Run `./mvnw verify`.
3. Run `./mvnw org.owasp:dependency-check-maven:check`.
4. Archive the post-remediation reports under
   `Deliverables/Phase 2/Evidence/sca/dependency-check-post-remediation`.
5. Keep the remaining two findings as explicit residual risk until a compatible
   stable remediation exists.

Local note for this remediation pass: `./mvnw clean test`, `./mvnw verify` and
`./mvnw org.owasp:dependency-check-maven:check` passed after the dependency
updates. The local post-remediation report is archived in the Evidence folder;
the next GitHub Actions SCA run should still be downloaded for presentation
timeline evidence.

Additional note on 2026-06-09: Dependency-Check reported newly published Spring
Framework CVEs against `spring-core` and `spring-web` 6.2.18. Maven metadata
showed Spring Boot 3.5.14 still as the latest 3.5.x parent, while Spring
Framework 6.2.19 was available. The project therefore uses the compatible
`spring-framework.version` patch override and should keep it until the Spring
Boot parent manages the same or newer fixed framework version.

## ASVS Mapping

- V13 Configuration: dependency and version management.
- V15 Secure Coding and Architecture: third-party component inventory and
  remediation time frames.
- V14 Data Protection: indirect impact where vulnerable components process
  user-submitted data.
