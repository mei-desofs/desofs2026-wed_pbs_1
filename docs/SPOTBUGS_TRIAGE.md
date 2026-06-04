# SpotBugs Triage

Source report: `Deliverables/Phase 2/Evidence/sast/sast-spotbugs-report (1)/spotbugsXml.xml`.

The original SpotBugs evidence reported 35 findings. After the remediation pass,
`./mvnw com.github.spotbugs:spotbugs-maven-plugin:spotbugs` reports 21 findings.
The post-remediation XML report is archived under
`Deliverables/Phase 2/Evidence/sast/spotbugs-post-remediation/spotbugsXml.xml`.

| Finding family | Previous evidence | Action | Status |
| --- | --- | --- | --- |
| `EI_EXPOSE_REP` / `EI_EXPOSE_REP2` in `RateLimitProperties` | Mutable nested `Limit` objects exposed by getters/setters | Getters and setters now copy `Limit` values defensively. | Fixed |
| Mutable JPA relationship exposure | `Report.attachments` and `User.assignedCases` returned mutable lists | Collection getters now return unmodifiable views; setters copy or use controlled add logic. | Mitigated |
| `REC_CATCH_EXCEPTION` in `JwtService` | Generic exception catch around JWT parsing/signing | Replaced broad catches with `JsonProcessingException`, `IOException` and `GeneralSecurityException` where applicable. | Fixed |
| `VA_FORMAT_STRING_USES_NEWLINE` | Newline in formatted output path | Response/document formatting now uses `System.lineSeparator()`. | Fixed |
| Spring dependency injection `EI_EXPOSE_REP2` | Controllers/services store injected beans and repositories | Accepted as framework-managed dependency injection rather than externally mutable user data. | Accepted residual risk |
| JPA entity bidirectional references | Some entity setters accept framework-managed mutable entities | Full aggregate immutability is a larger domain refactor; current mitigation focuses on public collection exposure. | Residual risk |
| Constructor throws (`CT_CONSTRUCTOR_THROW`) | Services validate required configuration in constructors | Accepted where fail-fast configuration prevents insecure runtime startup. | Accepted residual risk |

## Post-Remediation Finding Count

| SpotBugs type | Remaining count | Triage |
| --- | ---: | --- |
| `EI_EXPOSE_REP2` | 14 | Mostly Spring dependency injection and JPA bidirectional references. Accepted/triaged where framework-managed. |
| `EI_EXPOSE_REP` | 4 | JPA entity references. Residual risk; requires deeper aggregate refactor. |
| `CT_CONSTRUCTOR_THROW` | 3 | Fail-fast configuration validation in services. Accepted residual risk for this sprint. |

## Verification Plan

1. Run `./mvnw test`.
2. Run `./mvnw com.github.spotbugs:spotbugs-maven-plugin:spotbugs`.
3. Archive the new XML/HTML report under `Deliverables/Phase 2/Evidence/sast`
   or as a GitHub Actions artifact.
4. Update this triage with the new count and any remaining findings.

## ASVS Mapping

- V13 Configuration: fail-fast validation and secure defaults.
- V15 Secure Coding and Architecture: static analysis, dependency injection
  boundaries and mutable state.
- V16 Security Logging and Error Handling: controlled error formatting.
