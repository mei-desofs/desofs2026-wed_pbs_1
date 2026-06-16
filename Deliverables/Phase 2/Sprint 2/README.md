# Documentacao final Phase 2 Sprint 2

Esta pasta contem a documentacao final do GhostReport para a Phase 2 Sprint 2.
A estrutura foi mantida como relatorio principal com anexos tecnicos, evitando
renomeacoes ou reorganizacao agressiva.

## 1. Relatorio principal

- [PHASE2_SPRINT2_REPORT.md](PHASE2_SPRINT2_REPORT.md) - relatorio principal da Sprint 2, cobrindo arquitectura, seguranca, testes, pipeline, ASVS, limitacoes e conclusao.

## 2. Seguranca, arquitectura e autorizacao

- [SECURITY_ASSESSMENT.md](SECURITY_ASSESSMENT.md) - avaliacao final de seguranca, STRIDE, riscos residuais, cenarios demonstraveis e revisao critica.
- [AUTHORIZATION_MATRIX.md](AUTHORIZATION_MATRIX.md) - matriz de acesso por role, endpoint e controlo adicional.
- [SECURITY_CONFIGURATION_ASSESSMENT.md](SECURITY_CONFIGURATION_ASSESSMENT.md) - avaliacao de configuracao segura e diferencas dev/prod-like.
- [CRYPTOGRAPHIC_INVENTORY.md](CRYPTOGRAPHIC_INVENTORY.md) - inventario dos usos criptograficos, material de chave/segredo e politica de alteracao.
- [DANGEROUS_FUNCTIONALITY.md](DANGEROUS_FUNCTIONALITY.md) - inventario de operacoes sensiveis/perigosas e respetivas mitigacoes/testes.

## 3. Testes e validacao

- [SECURITY_TESTING.md](SECURITY_TESTING.md) - estrategia de testes, classes de teste e validacao de controlos.
- [SPOTBUGS_TRIAGE.md](SPOTBUGS_TRIAGE.md) - papel de SpotBugs no SAST e regras de triagem.

Validacao factual documentada: 292 testes Maven, 0 falhas, 0 erros e 0 skipped.

## 4. Pipeline DevSecOps

- [DEVSECOPS_PIPELINE.md](DEVSECOPS_PIPELINE.md) - fluxo CI/CD, code review, regras de codificacao/naming, gates, artefactos, ferramentas de seguranca e diagrama do workflow.

Workflows principais:

- `.github/workflows/dev.yml` - build/testes, JaCoCo, Gitleaks, SAST, SCA/SBOM, runtime evidence e ZAP baseline.
- `.github/workflows/pit.yml` - mutation testing PIT em workflow dedicado.

## 5. SCA, DAST e runtime evidence

- [SCA_TRIAGE.md](SCA_TRIAGE.md) - triagem SCA, CVEs Spring Security, suppressions e SBOM.
- [IAST_RUNTIME_SECURITY.md](IAST_RUNTIME_SECURITY.md) - documento principal de seguranca runtime e evidencia IAST-like.
- [iast-runtime-evidence.md](iast-runtime-evidence.md) - espelho documental do sumario runtime gerado pela CI.
- [runtime-endpoints.md](runtime-endpoints.md) - endpoints e probes exercitados durante a validacao runtime.
- [runtime-log-sanitization.md](runtime-log-sanitization.md) - verificacao de logs contra passwords, tokens, secrets e stack traces.

Validacao runtime expandida confirmada localmente: 101 probes, 101 passed, 0
failed e 0 skipped. Os artefactos exactos sao gerados em `target/iast-evidence/`
por cada run do workflow.

## 6. ASVS

- [ASVS_5.0_Tracker_Phase_2_Sprint_2.xlsx](ASVS_5.0_Tracker_Phase_2_Sprint_2.xlsx) - tracker ASVS principal da Sprint 2 em Excel, copiado estruturalmente do tracker Sprint 1 e actualizado com a evidencia factual do projecto.
- [ASVS_EVIDENCE.md](ASVS_EVIDENCE.md) - resumo explicativo do tracker ASVS, evolucao Sprint 1 -> Sprint 2 e links para evidencia de suporte.

O XLSX e a fonte principal para estados e percentagens ASVS; o Markdown e apenas
o resumo narrativo.

## 7. Demonstracao

- [FINAL_DEMO_GUIDE.md](FINAL_DEMO_GUIDE.md) - guiao pratico para demonstrar a aplicacao e apontar para as evidencias principais.

## 8. Instalacao segura

- [SECURE_INSTALLATION.md](SECURE_INSTALLATION.md) - instalacao segura, perfis, secrets, base de dados e storage.

## Notas de organizacao

- `PHASE2_SPRINT2_REPORT.md` e a fonte narrativa principal.
- `ASVS_5.0_Tracker_Phase_2_Sprint_2.xlsx` e a fonte ASVS principal.
- `DEVSECOPS_PIPELINE.md` e a fonte principal para pipeline, code review, standards leves de codigo e artefactos.
- `IAST_RUNTIME_SECURITY.md` e a fonte principal para runtime security evidence / IAST-like academic evidence; os restantes ficheiros runtime sao anexos de evidencia.
- A revisao final antiga foi incorporada em [PHASE2_SPRINT2_REPORT.md](PHASE2_SPRINT2_REPORT.md) e [SECURITY_ASSESSMENT.md](SECURITY_ASSESSMENT.md), para evitar um ficheiro solto redundante.
- Os relatorios de Phase 1 e Sprint 1 permanecem fora desta pasta e devem ser usados apenas como contexto historico.
