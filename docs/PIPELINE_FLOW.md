# DevSecOps Pipeline Flow

This document explains the intended validation flow for GhostReport in Phase 2
Sprint 2. The workflows are kept separate for clarity and faster feedback, but
they are documented as one coherent DevSecOps process.

## Flow Overview

```mermaid
flowchart TD
    A["00 - Secret Scanning"] --> B["01 - CI Build, Tests and Coverage"]
    B --> C["02A - SAST SpotBugs"]
    B --> D["02B - SCA Dependency-Check"]
    C --> E["03 - DAST OWASP ZAP Baseline"]
    D --> E
    E --> F["Evidence for ASVS and Final Report"]
```

## Stage 00 - Secret Scanning

Workflow: `.github/workflows/secret-scan-gitleaks.yml`

Purpose:

- scan the repository for hardcoded secrets;
- produce a JSON report;
- provide early feedback before deeper validation.

Recommended interpretation:

- a confirmed secret finding should be treated as blocking;
- an empty JSON array is valid evidence that no secrets were found in the
  scanned repository content.

## Stage 01 - CI Build, Tests and Coverage

Workflow: `.github/workflows/ci-tests.yml`

Purpose:

- compile the Spring Boot application;
- run automated tests;
- generate JaCoCo coverage evidence;
- enforce conservative JaCoCo baseline thresholds;
- use PostgreSQL 16 as a service container.

Recommended interpretation:

- this is the main blocking quality gate;
- later SAST, SCA and DAST evidence is only meaningful if the application can
  build and the automated test suite passes.

## Stage 02A - SAST SpotBugs

Workflow: `.github/workflows/sast-spotbugs.yml`

Purpose:

- run static analysis on Java code;
- identify suspicious patterns, bad practices and hardening opportunities;
- publish an XML artifact for manual triage.

Recommended interpretation:

- Sprint 2 mode is evidence/manual triage;
- findings should be fixed, documented as accepted risk, or justified as
  framework-managed behavior.

## Stage 02B - SCA OWASP Dependency-Check

Workflow: `.github/workflows/sca-dependency-check.yml`

Purpose:

- identify known vulnerabilities in dependencies;
- generate HTML, JSON, XML and SARIF reports;
- support dependency risk triage.

Recommended interpretation:

- Sprint 2 mode is evidence/manual triage;
- the build is not failed automatically while the team validates CVEs and false
  positives;
- `NVD_API_KEY` is provided through GitHub Actions secrets.

## Stage 03 - DAST OWASP ZAP Baseline

Workflow: `.github/workflows/dast-zap.yml`

Purpose:

- start GhostReport on the GitHub runner at `http://localhost:8081`;
- run OWASP ZAP in Docker with host networking;
- generate baseline/passive DAST reports.

Recommended interpretation:

- DAST is runtime evidence;
- the scan is unauthenticated, passive and non-intrusive;
- warnings are triaged manually;
- authenticated DAST and active scans are future work.

## Blocking vs Evidence Mode

| Stage | Blocking? | Reason |
| --- | --- | --- |
| Secret scanning | Yes for confirmed secrets | Secrets must not enter the repository. |
| CI build/tests | Yes | Broken builds or failing tests block delivery confidence. |
| JaCoCo | Yes for baseline threshold regressions | Coverage is measured, published and kept above conservative minimum thresholds. |
| SpotBugs | Evidence | Findings need manual triage during Sprint 2. |
| Dependency-Check | Evidence | CVEs require false-positive and applicability analysis. |
| ZAP baseline | Evidence | Baseline warnings are hardening opportunities, not necessarily exploitable vulnerabilities. |

## Why Workflows Are Separate

The workflows are separated to make each security activity easier to understand,
rerun and explain:

- fast feedback for secrets and CI;
- independent reruns for long SCA/DAST jobs;
- clearer artifacts per security activity;
- easier mapping to ASVS controls and report evidence.

This gives the project a professional DevSecOps flow without adding unnecessary
pipeline complexity before final delivery.
