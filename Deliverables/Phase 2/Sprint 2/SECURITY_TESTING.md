# Testes de segurança

Este documento consolida a evidência de testes e validação do Sprint 2.
Substitui as notas antigas da raiz sobre security testing e validation rules.

## Resultado local mais recente

Validação local da linha actual:

```powershell
cd ghostreport
.\mvnw.cmd test
```

Resultado: 180 testes executados, 0 falhas.

## Áreas cobertas

| Área | Evidência |
| --- | --- |
| Autenticação | Login com sucesso/falha, utilizador inactivo, emissão de JWT e logout. |
| MFA | Desafio admin, verificação, expiração/reutilização e emissão final de JWT. |
| RBAC | Rotas admin, analyst e auditor com casos positivos e negativos. |
| Propriedade de casos | Actualizações permitidas e negadas para analistas. |
| Denúncias anónimas | Criação, tracking code e verificação pública. |
| Uploads | Tamanho, extensão, MIME/assinatura, nomes gerados e rejeição de traversal. |
| Auditoria/alertas | Operações críticas produzem evidência. |
| Backups/evidência | Manifestos e verificações de integridade. |
| Frontend | Comportamento seleccionado de navbar, tracking code e navegação por role. |

## Matriz de validação

| Fluxo | Regras principais | Objectivo de segurança |
| --- | --- | --- |
| `POST /auth/login` | Username/password obrigatórios, erros genéricos, rate limiting. | Reduzir brute force e enumeração. |
| `POST /auth/mfa/verify` | Desafio/código obrigatórios, uso único, TTL curto. | Evitar bypass e replay de MFA nas roles internas. |
| Password reset | Resposta genérica, validação de token e política de password. | Reduzir enumeração e passwords fracas. |
| `POST /reports` | Título/descrição/categoria obrigatórios, limites de tamanho, DTOs. | Evitar dados malformados e mass assignment. |
| `POST /reports/verify` | Formato e existência do tracking code. | Evitar enumeração ruidosa. |
| Uploads | Allowlist de extensão, MIME/assinatura, tamanho, nomes gerados. | Reduzir ficheiros maliciosos e path traversal. |
| Actualizações de analista | Estado/prioridade/notas e ownership. | Impedir alterações não autorizadas. |
| Downloads/pacotes | Caminhos canónicos e protecção ZIP Slip. | Impedir fuga do filesystem. |

## Evidência manual e pipeline

| Check | Comando ou fonte |
| --- | --- |
| Testes unitários/integração | `.\mvnw.cmd test` |
| JaCoCo | `.\mvnw.cmd test jacoco:report` |
| SAST | Job `sast`: CodeQL, SonarCloud, SpotBugs. |
| SCA | Job `dependency-scanning`: Dependency-Check e CycloneDX. |
| DAST | Job `dast-scan`: runtime checks e ZAP baseline. |
| Mutation testing | Workflow `pit-mutation-testing`. |

## Limites

- ZAP baseline não substitui teste de penetração completo.
- MFA está implementado e testado para `ADMIN`, `ANALYST` e `AUDITOR`.
- Rate limiting é em memória.
- Evidência runtime é IAST-like; não há agente IAST completo.
