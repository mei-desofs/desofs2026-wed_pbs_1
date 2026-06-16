# Documentação final Phase 2 Sprint 2

Esta pasta contém a documentação final do GhostReport para a Phase 2 Sprint 2.
O objetivo deste README é servir como índice rápido da entrega para avaliação.

## 1. Relatório principal

* [PHASE2_SPRINT2_REPORT.md](PHASE2_SPRINT2_REPORT.md) - relatório principal e narrativa final da Sprint 2, cobrindo arquitetura, domínio, roles, autenticação/MFA/JWT, RBAC, validação, uploads, auditoria, backups, pipeline, scanners, testes, ASVS, limitações e conclusão.

## 2. ASVS

* [ASVS_5.0_Tracker_Phase_2_Sprint_2.xlsx](ASVS_5.0_Tracker_Phase_2_Sprint_2.xlsx) - tracker ASVS principal da Sprint 2. Este ficheiro é a fonte principal para estados, classificações e percentagens ASVS.
* [ASVS_EVIDENCE.md](ASVS_EVIDENCE.md) - resumo explicativo do tracker ASVS, da evolução Sprint 1 -> Sprint 2 e dos links para evidência de suporte. Não substitui o XLSX.

## 3. Testes e qualidade

* [SECURITY_TESTING.md](SECURITY_TESTING.md) - estratégia de testes, classes JUnit/MockMvc e validação dos controlos de segurança.
* [SPOTBUGS_TRIAGE.md](SPOTBUGS_TRIAGE.md) - papel de SpotBugs no SAST e regras de triagem.

Validacao factual documentada: 299 testes Maven, 0 falhas, 0 erros e 0 skipped.

## 4. Pipeline DevSecOps

* [DEVSECOPS_PIPELINE.md](DEVSECOPS_PIPELINE.md) - fluxo CI/CD, code review, naming/coding standards leves, gates, artefactos, ferramentas de segurança e diagrama do workflow.

Workflows principais:

* `.github/workflows/dev.yml` - config-validation, build, testes/JaCoCo, Gitleaks, SAST, SCA, SBOM, Trivy image scan, runtime evidence e ZAP baseline.
* `.github/workflows/pit.yml` - mutation testing PIT em workflow dedicado.

## 5. SCA, CVEs, DAST e runtime evidence

* [SCA_TRIAGE.md](SCA_TRIAGE.md) - triagem SCA, CVEs Spring Security, suppressions justificadas e SBOM.
* [IAST_RUNTIME_SECURITY.md](IAST_RUNTIME_SECURITY.md) - documento principal de segurança runtime e evidência IAST-like. A entrega não afirma IAST agent-based completo.
* [iast-runtime-evidence.md](iast-runtime-evidence.md) - espelho documental do sumário runtime gerado pela CI.
* [runtime-endpoints.md](runtime-endpoints.md) - endpoints e probes exercitados durante a validação runtime.
* [runtime-log-sanitization.md](runtime-log-sanitization.md) - verificação de logs contra passwords, tokens, secrets e stack traces.

Validação runtime expandida confirmada localmente: 101 probes, 101 passed, 0 failed e 0 skipped. Os artefactos exatos são gerados em `target/iast-evidence/` por cada run do workflow.

## 6. Segurança, arquitetura e autorização

* [SECURITY_ASSESSMENT.md](SECURITY_ASSESSMENT.md) - avaliação final de segurança, STRIDE, riscos residuais, cenários demonstráveis e revisão crítica.
* [AUTHORIZATION_MATRIX.md](AUTHORIZATION_MATRIX.md) - matriz de acesso por role, endpoint e controlo adicional.
* [SECURITY_CONFIGURATION_ASSESSMENT.md](SECURITY_CONFIGURATION_ASSESSMENT.md) - avaliação de configuração segura e diferenças dev/prod-like.
* [SECURE_INSTALLATION.md](SECURE_INSTALLATION.md) - instalação segura, perfis, secrets, base de dados, TLS/proxy e storage.
* [CRYPTOGRAPHIC_INVENTORY.md](CRYPTOGRAPHIC_INVENTORY.md) - inventário dos usos criptográficos, material de chave/segredo e política de alteração.
* [DANGEROUS_FUNCTIONALITY.md](DANGEROUS_FUNCTIONALITY.md) - inventário de operações sensíveis/perigosas e respetivas mitigações/testes.

## 7. Evidencia visual

As imagens em [imagens/](imagens/) são anexos visuais para apoio à apresentação. Não substituem os relatórios, testes ou artefactos gerados pela pipeline; servem para tornar a leitura rápida mais clara:

* [imagens/asvs.png](imagens/asvs.png) - snapshot do resumo ASVS por capítulo e nível.
* [imagens/asvs2.png](imagens/asvs2.png) - gráfico visual da evolução/cobertura ASVS.
* [imagens/dependency_check.png](imagens/dependency_check.png) - resumo do Dependency-Check.
* [imagens/jacoco.png](imagens/jacoco.png) - resumo de cobertura JaCoCo.
* [imagens/pit.png](imagens/pit.png) - resumo PIT de mutation testing e test strength.
* [imagens/pl.png](imagens/pl.png) - evidência visual de pull request/checks.
* [imagens/zap.png](imagens/zap.png) - resumo do ZAP baseline.
