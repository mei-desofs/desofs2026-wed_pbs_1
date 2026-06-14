# Avaliação final de segurança

## Avaliação global

GhostReport implementa os principais controlos esperados para a entrega DESOFS:
denúncia anónima pública, roles internas protegidas, autenticação JWT, MFA
admin, validação, hardening de uploads, evidência de auditoria, integridade de
backups e evidência DevSecOps.

Os riscos restantes são sobretudo operacionais e de produção, fora do âmbito
principal do repositório académico.

## Avaliação por área

| Área | Estado | Evidência |
| --- | --- | --- |
| Denúncia anónima | Implementado | Fluxos públicos de submissão e tracking code. |
| Autenticação | Implementado | JWT, BCrypt, logout e testes. |
| MFA | Parcial | Implementado para `ADMIN`; não implementado para `ANALYST`/`AUDITOR`. |
| Autorização | Implementado | [AUTHORIZATION_MATRIX.md](AUTHORIZATION_MATRIX.md), testes RBAC e ownership. |
| Validação | Implementado | DTOs, Bean Validation e testes. |
| Uploads | Implementado | Extensão, MIME/assinatura, tamanho e path checks. |
| Auditoria/alertas | Implementado | Registos com metadados de integridade. |
| Backups | Implementado | Manifestos e verificação. |
| SCA/SBOM | Implementado | [SCA_TRIAGE.md](SCA_TRIAGE.md). |
| SAST | Evidência implementada | CodeQL, SonarCloud e SpotBugs. |
| DAST | Baseline implementado | Runtime checks e ZAP baseline. |
| IAST | Parcial | Evidência runtime/IAST-like apenas. |
| Instalação segura | Documentado | [SECURE_INSTALLATION.md](SECURE_INSTALLATION.md). |

## Cobertura de ameaças

| Ameaça | Mitigação actual |
| --- | --- |
| Acesso admin não autorizado | RBAC, MFA admin, JWT e utilizadores inactivos bloqueados. |
| Escalada de analista | Regras de rota e ownership nos serviços. |
| Abuso de tracking code | Validação de formato, erros genéricos e rate limiting. |
| Upload malicioso | Allowlists, assinatura, tamanho e nomes seguros. |
| Path traversal/ZIP Slip | Caminhos canónicos e tratamento seguro de ZIP. |
| Alteração de auditoria | Metadados de integridade e verificação. |
| Dependências vulneráveis | Dependency-Check, SBOM e Spring Security `6.5.11`. |

## Riscos residuais

- MFA é admin-only.
- Não existe agente IAST completo.
- ZAP é baseline e não DAST autenticado completo.
- Rate limiting é em memória.
- TLS, SIEM, retenção imutável e secret manager são responsabilidades
  operacionais.
- Migrações formais de BD não estão incluídas.

## Juízo final

Para o âmbito da unidade curricular, o projecto é coerente e avaliável:
controlos implementados têm suporte em código, testes, pipeline ou documentação
explícita. Antes de produção real, os controlos operacionais residuais devem ser
implementados e validados no ambiente alvo.
