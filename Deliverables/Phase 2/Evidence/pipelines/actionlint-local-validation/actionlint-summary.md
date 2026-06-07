# GitHub Actions Workflow Validation

Validated locally with `actionlint` 1.7.12 on 2026-06-05.

Command:

```powershell
$files = Get-ChildItem -LiteralPath .github/workflows -Filter *.yml | ForEach-Object { $_.FullName }
& "$env:TEMP\ghostreport-tools\actionlint\actionlint.exe" -color=false @files
```

Result: no workflow syntax or semantic errors reported.

Scope: local validation of the workflow files present in the current branch. This does not replace a real GitHub Actions run, but it is useful evidence that the YAML/workflow structure is valid before pushing.
