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
| Angus Activation | CVE-2025-7962 | 2.0.3 | No stable compatible upgrade selected; Maven metadata only showed 2.1.0-M1 as newer. | Accepted residual risk / applicability review | Transitive dependency via JAXB/Hibernate. GhostReport does not implement email/SMTP attachment processing. Revisit if a stable 2.1.x release is available. |
| Hibernate Validator | CVE-2025-15104 | 8.0.3.Final | No direct upgrade selected in this phase. | Accepted residual risk / applicability review | Managed by Spring Boot validation stack. Upgrade to 9.x may imply Jakarta Validation stack changes; keep under manual triage and reassess with future Spring Boot patches. |

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

## ASVS Mapping

- V13 Configuration: dependency and version management.
- V15 Secure Coding and Architecture: third-party component inventory and
  remediation time frames.
- V14 Data Protection: indirect impact where vulnerable components process
  user-submitted data.
