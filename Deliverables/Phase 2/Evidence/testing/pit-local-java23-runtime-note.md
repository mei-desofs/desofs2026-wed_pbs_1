# PIT Local Runtime Note

Generated during Phase 2 Sprint 2 remediation.

Local command attempted:

```powershell
./mvnw '-Djacoco.skip=true' org.pitest:pitest-maven:mutationCoverage
```

Result: PIT plugin `1.25.3` started but failed before report generation on the
local Java 23 runtime with `MINION_DIED` and `CoverageMinion` startup failure.

This is preserved as local evidence because it explains why no local HTML/XML
PIT report was produced on this workstation. The GitHub Actions PIT workflow
uses Java 17 and remains the expected environment for the real archiveable PIT
artifact.

Status: evidence review, not hidden.
