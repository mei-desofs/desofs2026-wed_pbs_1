# Evidencia ASVS

O tracker ASVS principal do Sprint 2 e o ficheiro Excel:

- [ASVS_5.0_Tracker_Phase_2_Sprint_2.xlsx](ASVS_5.0_Tracker_Phase_2_Sprint_2.xlsx)

O ficheiro foi criado como copia estrutural do tracker ASVS da Phase 2 Sprint
1, mantendo as mesmas folhas, formulas, capitulos e formato geral. O conteudo
foi actualizado para reflectir a evidencia factual do Sprint 2. Este Markdown
e apenas o resumo explicativo da evidencia; nao substitui o XLSX.

## Base factual usada

| Evidencia | Resultado confirmado |
| --- | --- |
| Testes Maven | `250` testes, `0` falhas, `0` erros, `0` skipped. |
| Runtime probes locais | `101` probes, `101` passed, `0` failed, `0` skipped. |
| Spring Security | `6.5.11` via Spring Boot `3.5.15`. |
| SCA CVEs remediados | CVE-2026-40988, CVE-2026-41694, CVE-2026-41003. |
| Suppressions SCA | CVE-2025-15104 e CVE-2025-7962 documentados como nao aplicaveis/falso positivo para os componentes usados. |

## Mapa de evidencia

| Area ASVS | Evidencia GhostReport | Estado |
| --- | --- | --- |
| Arquitectura e threat modelling | Relatorio Phase 1, DDD, trust boundaries e relatorio Sprint 2. | Implementado/documentado |
| Autenticacao | BCrypt, login interno, JWT, bloqueio de utilizadores inactivos e logout/revogacao. | Implementado |
| MFA | Desafio MFA antes de JWT para `ADMIN`, `ANALYST` e `AUDITOR`. | Implementado; canal de producao e futuro |
| Autorizacao | Regras de rota em `SecurityConfig`, RBAC e ownership nos servicos. | Implementado |
| Denunciante anonimo/tracking | Sem conta de reporter; tracking code controla verificacao/listagem/download. | Implementado |
| Validacao | DTOs, Bean Validation, enums/allowlists, tracking code e contratos API. | Implementado |
| Ficheiros/uploads | Extensao/MIME/magic bytes, tamanho, nomes gerados, path checks e ZIP Slip em backups/packages. | Implementado |
| Erros e logging | Erros genericos, correlation id, audit logs, security alerts e sanitizacao. | Implementado |
| Backups/evidencia | ZIPs com hashes, HMAC, manifesto, verificacao e restore para staging. | Implementado |
| SCA/SAST/DAST/runtime | Dependency-Check, CycloneDX, CodeQL, SpotBugs, SonarCloud, Gitleaks, ZAP baseline e runtime evidence. | Implementado como evidencia |
| Configuracao | Secrets por ambiente, PostgreSQL fora de testes, validacao de configuracao e guia seguro. | Implementado/documentado |

## Limitacoes ASVS registadas

- Nao existe agente IAST real; a evidencia e runtime security evidence /
  IAST-like academic evidence.
- ZAP e baseline/passivo e nao cobre contexto autenticado completo.
- MFA em dev/test pode expor codigo em log para demonstracao; producao precisa
  de canal real.
- Rate limiting e em memoria.
- Secret manager, SIEM/WORM, TLS operacional, KMS e Flyway/Liquibase ficam como
  controlos futuros/operacionais.

## Documentos relacionados

- [PHASE2_SPRINT2_REPORT.md](PHASE2_SPRINT2_REPORT.md)
- [AUTHORIZATION_MATRIX.md](AUTHORIZATION_MATRIX.md)
- [SECURITY_TESTING.md](SECURITY_TESTING.md)
- [DEVSECOPS_PIPELINE.md](DEVSECOPS_PIPELINE.md)
- [SCA_TRIAGE.md](SCA_TRIAGE.md)
- [IAST_RUNTIME_SECURITY.md](IAST_RUNTIME_SECURITY.md)
- [SECURITY_ASSESSMENT.md](SECURITY_ASSESSMENT.md)
