# Contributing to GhostReport

This guide defines the working rules for Sprint 2 and final delivery. The goal
is to keep the code, documentation, ASVS evidence and report coherent while
three people work in parallel.

## Branches

Use short feature branches with a clear scope:

```text
feature/admin-security-hardening
feature/devsecops-report-hardening
fix/upload-validation
docs/asvs-evidence
```

Avoid mixing backend security changes with report/pipeline changes in the same
branch unless they are directly related.

## Commits

Use focused commits:

```text
ci: clarify DevSecOps workflows
docs: add coding standards
test: cover admin authorization
security: add login rate limiting
```

Each commit should compile or at least leave the touched area understandable.
Do not commit generated local backups, temporary extracted text files or local
IDE metadata.

## Pull Requests

Each pull request should include:

- What changed.
- Why it changed.
- How it was tested.
- Evidence artifacts or screenshots when relevant.
- ASVS controls affected, if applicable.

For DevSecOps/documentation pull requests, also include:

- workflows changed;
- expected artifacts;
- whether the workflow is blocking or evidence/manual triage;
- documentation updated to match the pipeline behavior.

Before requesting review, run the relevant command:

```powershell
cd ghostreport
.\mvnw.cmd test
```

For pipeline/documentation branches, verify YAML indentation and artifact paths.

Useful validation command:

```powershell
python - <<'PY'
import yaml
from pathlib import Path
for path in Path('.github/workflows').glob('*.yml'):
    yaml.safe_load(path.read_text(encoding='utf-8'))
    print(f'valid yaml: {path}')
PY
```

## Review Checklist

- Does the implementation match the report wording?
- Are endpoint roles aligned with `SecurityConfig`?
- Are errors generic enough to avoid leaking internals?
- Are new security claims supported by tests or artifacts?
- Are limitations documented instead of overstated?

## Documentation Rules

When a security control is added or changed, update at least one of:

- `docs/ASVS_EVIDENCE.md`
- `docs/ASVS_LEVEL2_EVIDENCE.md`
- `docs/SECURITY_ASSESSMENT.md`
- `docs/SECURITY_CONFIGURATION_ASSESSMENT.md`
- `docs/DEVSECOPS_PIPELINE.md`
- `docs/PIPELINE_FLOW.md`
- `docs/PIPELINE_ARTIFACTS.md`
- final report chapter
- ASVS tracker spreadsheet

The report should describe only what is implemented, validated or explicitly
planned as future work.
