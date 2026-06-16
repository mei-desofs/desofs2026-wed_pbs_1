# Documentacao final Phase 2 Sprint 2

Esta pasta contem a documentacao final do GhostReport para a Phase 2 Sprint 2.
O objectivo deste README e servir como indice rapido da entrega para avaliacao.

## Leitura recomendada

| Pergunta | Ficheiro principal |
| --- | --- |
| Onde esta o relatorio principal? | [PHASE2_SPRINT2_REPORT.md](PHASE2_SPRINT2_REPORT.md) |
| Onde esta o tracker ASVS? | [ASVS_5.0_Tracker_Phase_2_Sprint_2.xlsx](ASVS_5.0_Tracker_Phase_2_Sprint_2.xlsx) |
| Onde estao os testes? | [SECURITY_TESTING.md](SECURITY_TESTING.md) |
| Onde esta a pipeline? | [DEVSECOPS_PIPELINE.md](DEVSECOPS_PIPELINE.md) |
| Onde esta a SCA/CVEs? | [SCA_TRIAGE.md](SCA_TRIAGE.md) |
| Onde esta a evidencia runtime/IAST-like? | [IAST_RUNTIME_SECURITY.md](IAST_RUNTIME_SECURITY.md) |
| Onde esta a seguranca/configuracao? | [SECURITY_CONFIGURATION_ASSESSMENT.md](SECURITY_CONFIGURATION_ASSESSMENT.md) |

## 1. Relatorio principal

- [PHASE2_SPRINT2_REPORT.md](PHASE2_SPRINT2_REPORT.md) - relatorio principal e narrativa final da Sprint 2, cobrindo arquitectura, dominio, roles, autenticacao/MFA/JWT, RBAC, validacao, uploads, auditoria, backups, pipeline, scanners, testes, ASVS, limitacoes e conclusao.

## 2. ASVS

- [ASVS_5.0_Tracker_Phase_2_Sprint_2.xlsx](ASVS_5.0_Tracker_Phase_2_Sprint_2.xlsx) - tracker ASVS principal da Sprint 2. Este ficheiro e a fonte principal para estados, classificacoes e percentagens ASVS.
- [ASVS_EVIDENCE.md](ASVS_EVIDENCE.md) - resumo explicativo do tracker ASVS, da evolucao Sprint 1 -> Sprint 2 e dos links para evidencia de suporte. Nao substitui o XLSX.

## 3. Testes e qualidade

- [SECURITY_TESTING.md](SECURITY_TESTING.md) - estrategia de testes, classes JUnit/MockMvc e validacao dos controlos de seguranca.
- [SPOTBUGS_TRIAGE.md](SPOTBUGS_TRIAGE.md) - papel de SpotBugs no SAST e regras de triagem.

Validacao factual documentada: 286 testes Maven, 0 falhas, 0 erros e 0 skipped.

## 4. Pipeline DevSecOps

- [DEVSECOPS_PIPELINE.md](DEVSECOPS_PIPELINE.md) - fluxo CI/CD, code review, naming/coding standards leves, gates, artefactos, ferramentas de seguranca e diagrama do workflow.

Workflows principais:

- `.github/workflows/dev.yml` - build/testes, JaCoCo, Gitleaks, SAST, SCA/SBOM, runtime evidence e ZAP baseline.
- `.github/workflows/pit.yml` - mutation testing PIT em workflow dedicado.

## 5. SCA, CVEs, DAST e runtime evidence

- [SCA_TRIAGE.md](SCA_TRIAGE.md) - triagem SCA, CVEs Spring Security, suppressions justificadas e SBOM.
- [IAST_RUNTIME_SECURITY.md](IAST_RUNTIME_SECURITY.md) - documento principal de seguranca runtime e evidencia IAST-like. A entrega nao afirma IAST agent-based completo.
- [iast-runtime-evidence.md](iast-runtime-evidence.md) - espelho documental do sumario runtime gerado pela CI.
- [runtime-endpoints.md](runtime-endpoints.md) - endpoints e probes exercitados durante a validacao runtime.
- [runtime-log-sanitization.md](runtime-log-sanitization.md) - verificacao de logs contra passwords, tokens, secrets e stack traces.

Validacao runtime expandida confirmada localmente: 101 probes, 101 passed, 0 failed e 0 skipped. Os artefactos exactos sao gerados em `target/iast-evidence/` por cada run do workflow.

## 6. Seguranca, arquitectura e autorizacao

- [SECURITY_ASSESSMENT.md](SECURITY_ASSESSMENT.md) - avaliacao final de seguranca, STRIDE, riscos residuais, cenarios demonstraveis e revisao critica.
- [AUTHORIZATION_MATRIX.md](AUTHORIZATION_MATRIX.md) - matriz de acesso por role, endpoint e controlo adicional.
- [SECURITY_CONFIGURATION_ASSESSMENT.md](SECURITY_CONFIGURATION_ASSESSMENT.md) - avaliacao de configuracao segura e diferencas dev/prod-like.
- [SECURE_INSTALLATION.md](SECURE_INSTALLATION.md) - instalacao segura, perfis, secrets, base de dados, TLS/proxy e storage.
- [CRYPTOGRAPHIC_INVENTORY.md](CRYPTOGRAPHIC_INVENTORY.md) - inventario dos usos criptograficos, material de chave/segredo e politica de alteracao.
- [DANGEROUS_FUNCTIONALITY.md](DANGEROUS_FUNCTIONALITY.md) - inventario de operacoes sensiveis/perigosas e respectivas mitigacoes/testes.
