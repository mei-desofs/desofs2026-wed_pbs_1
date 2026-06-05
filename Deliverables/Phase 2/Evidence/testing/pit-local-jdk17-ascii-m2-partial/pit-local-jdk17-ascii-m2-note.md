# PIT Local Validation Note - JDK 17 and ASCII Maven Repository

Date: 2026-06-05

Local PIT was retried with a temporary Temurin JDK 17 and an ASCII-only Maven repository path:

```powershell
$env:JAVA_HOME = "$env:TEMP\ghostreport-tools\jdk17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
./mvnw '-Dmaven.repo.local=C:\Projetos\m2-pit' '-Djacoco.skip=true' org.pitest:pitest-maven:mutationCoverage
```

Findings:

- The previous immediate `CoverageMinion` classpath failure did not occur with the ASCII Maven repository.
- PIT started generating report output under `ghostreport/target/pit-reports`.
- The local run did not finish within the available execution window and was stopped to avoid leaving orphan Java processes.
- This remains evidence-review, not a blocking gate.

Recommended next step: run Stage 05 in GitHub Actions on Ubuntu/Java 17 and archive the generated `pit-mutation-testing-report` artifact. If runtime remains too high, reduce PIT scope further to a smaller package/class set for Sprint 2 evidence.
