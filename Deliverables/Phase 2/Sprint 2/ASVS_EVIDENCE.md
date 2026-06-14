# Evidência ASVS

Este ficheiro é a fonte actual de rastreabilidade ASVS do Sprint 2. A
spreadsheet do Sprint 1 permanece evidência histórica e não é renomeada porque
não existe, nesta branch, uma spreadsheet específica do Sprint 2.

## Mapa de evidência

| Área ASVS | Evidência GhostReport | Estado |
| --- | --- | --- |
| Arquitectura e threat modelling | Relatório Phase 1, DDD, trust boundaries e relatório Sprint 2. | Implementado/documentado |
| Autenticação | JWT, BCrypt, bloqueio de utilizadores inactivos e logout. | Implementado |
| MFA | Desafio MFA antes de JWT admin. | Parcial: apenas admin |
| Autorização | Regras de rota e ownership nos serviços. | Implementado |
| Validação | DTOs, Bean Validation, uploads e tracking code. | Implementado |
| Ficheiros | Nomes gerados, path checks e protecção ZIP Slip. | Implementado |
| Criptografia/secrets | JWT secret, HMAC de backup e orientação de configuração. | Parcial: sem KMS/secret manager |
| Erros e logging | Erros genéricos, auditoria e alertas. | Implementado |
| Protecção de dados | PostgreSQL e integridade de auditoria/backups. | Parcial: encriptação/retenção imutável são futuro |
| Comunicação | HTTPS exigido operacionalmente. | Documentado, dependente do ambiente |
| Dependências/código malicioso | Dependency-Check, CycloneDX, SpotBugs, CodeQL, SonarCloud e Gitleaks. | Evidência implementada |
| Configuração | Instalação segura e avaliação de configuração. | Implementado/documentado |

## Controlos Sprint 2

| Objectivo | Evidência | Notas |
| --- | --- | --- |
| Separação de roles | [AUTHORIZATION_MATRIX.md](AUTHORIZATION_MATRIX.md) | Cobre público, admin, analyst e auditor. |
| Testes de segurança | [SECURITY_TESTING.md](SECURITY_TESTING.md) | Consolida testes e validação. |
| Entrega segura | [DEVSECOPS_PIPELINE.md](DEVSECOPS_PIPELINE.md) | CI e artefactos. |
| Dependências | [SCA_TRIAGE.md](SCA_TRIAGE.md) | Spring Security e suppressions. |
| SAST | [SPOTBUGS_TRIAGE.md](SPOTBUGS_TRIAGE.md) | Papel da ferramenta e triagem. |
| Runtime/IAST-like | [IAST_RUNTIME_SECURITY.md](IAST_RUNTIME_SECURITY.md) | Não há agente IAST completo. |
| Instalação | [SECURE_INSTALLATION.md](SECURE_INSTALLATION.md) | Perfis, secrets, BD e storage. |
| Postura final | [SECURITY_ASSESSMENT.md](SECURITY_ASSESSMENT.md) | Controlos e riscos residuais. |

## Notas Level 2

Suportado por evidência:

- estado de autenticação validado por filtros JWT;
- login admin com segundo factor antes do token;
- controlo de acesso deny-by-default em APIs protegidas;
- acesso directo a objectos restringido por serviços quando necessário;
- validação de input com DTOs e Bean Validation;
- uploads controlados e guardados com nomes gerados;
- eventos de auditoria/segurança para fluxos críticos;
- SCA e SAST incluídos na pipeline.

Parcial:

- MFA não é obrigatório para todas as roles internas;
- secrets são externalizados, mas sem secret manager dedicado;
- integridade de backups existe, mas encriptação e retenção imutável são futuras;
- evidência runtime é IAST-like, não IAST agent-based.

## Nota sobre spreadsheet

Não foi encontrada uma spreadsheet ASVS específica de Sprint 2 nesta branch. A
spreadsheet de Sprint 1 deve permanecer histórica. Se a submissão final exigir
um `.xlsx` para Sprint 2, deve ser gerado a partir deste mapa com o nome
`ASVS_5.0_Tracker_Phase_2_Sprint_2.xlsx`.
