# Software Composition Analysis Triage

Source report: `Deliverables/Phase 2/Evidence/sca/dependency-check-sca-json/dependency-check-report.json`.

The previous Dependency-Check evidence reported 44 vulnerabilities, including
critical and high findings. This triage records the remediation decision for
the affected dependency families. A new Dependency-Check run must be used as the
final evidence after the dependency update commit.

| Component | Reported issue | Previous version | Action | Status | Evidence / residual risk |
| --- | --- | ---: | --- | --- | --- |
| Spring Boot parent and starters | Critical/high CVEs reported against Spring Boot artifacts | 3.5.13 | Upgraded Spring Boot parent to 3.5.14, the latest stable 3.5.x patch found by Maven metadata. | Mitigated pending scan | Avoided 4.1.0-RC1 because it is a release candidate and too risky for Sprint 2. Re-run Dependency-Check. |
| Tomcat embed core/websocket/el | Critical/high Tomcat CVEs | 10.1.53 | Overrode Boot-managed Tomcat to 10.1.55, latest stable 10.1.x from Maven metadata. | Mitigated pending scan | Must be validated by tests and Dependency-Check. |
| PostgreSQL JDBC | CVE affecting versions before 42.7.11 | 42.7.10 | Set `postgresql.version` to 42.7.11. | Fixed pending scan | Maven metadata reports 42.7.11 as latest/release. |
| Log4j API / log4j-to-slf4j | Medium CVEs reported for Log4j API 2.24.3 | 2.24.3 | Set `log4j2.version` to 2.26.0, latest stable 2.x line observed in Maven metadata. | Mitigated pending scan | Application uses Spring Boot logging through SLF4J/Logback; Log4j API is transitively present through `log4j-to-slf4j`. |
| Angus Activation | Dependency-Check medium finding | 2.0.3 | No stable compatible upgrade selected; Maven metadata only showed 2.1.0-M1 as newer. | Accepted residual risk / applicability review | Transitive dependency via JAXB/Hibernate. GhostReport does not implement email/SMTP attachment processing. Revisit if a stable 2.1.x release is available. |
| Hibernate Validator | Dependency-Check medium finding | 8.0.3.Final | No direct upgrade selected in this phase. | Accepted residual risk / applicability review | Managed by Spring Boot validation stack. Upgrade to 9.x may imply Jakarta Validation stack changes; re-run after Boot patch first. |

## Verification Plan

1. Run `./mvnw clean test`.
2. Run `./mvnw verify`.
3. Run `./mvnw org.owasp:dependency-check-maven:check`.
4. Replace or archive the old Dependency-Check artifact with the new report.
5. Update `docs/ASVS_EVIDENCE.md` and the ASVS tracker if the new report changes
   the compliance state.

Local note for this remediation pass: `./mvnw clean test` and `./mvnw verify`
passed after the dependency updates. A local Dependency-Check run was attempted
but exceeded the 10-minute execution window before producing a report; the next
GitHub Actions SCA run should be used as the authoritative post-remediation
evidence.

## ASVS Mapping

- V13 Configuration: dependency and version management.
- V15 Secure Coding and Architecture: third-party component inventory and
  remediation time frames.
- V14 Data Protection: indirect impact where vulnerable components process
  user-submitted data.
