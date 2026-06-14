# Matriz de autorização

Esta matriz documenta o modelo de acesso actual do GhostReport. Reflecte os
grupos de rotas configurados no Spring Security e as verificações de propriedade
nos serviços.

## Roles

| Actor | Descrição |
| --- | --- |
| Denunciante anónimo | Utilizador público sem JWT. Pode submeter e acompanhar denúncias por código de tracking. |
| `ANALYST` | Responsável interno por análise de casos. Pode consultar casos elegíveis e actualizar casos atribuídos. |
| `AUDITOR` | Revisor interno de evidência. Pode consultar auditoria, alertas, casos fechados e integridade de backups. |
| `ADMIN` | Utilizador administrativo. Pode gerir utilizadores, consultar evidência e gerir backups. Login admin requer MFA antes da emissão do JWT. |

Não existe role `USER` activa no modelo protegido actual.

## Endpoints públicos

| Grupo de endpoints | Acesso | Notas |
| --- | --- | --- |
| `GET /`, HTML/CSS/JS estáticos | Público | Serve a interface. Páginas estáticas não concedem acesso a APIs. |
| `POST /auth/login` | Público | Emite JWT para não-admin depois de password válida; devolve desafio MFA para admin. |
| `POST /auth/mfa/verify` | Público com desafio | Completa MFA admin e emite JWT. |
| `POST /auth/password-reset/request` | Público | Resposta genérica para evitar enumeração. |
| `POST /auth/password-reset/confirm` | Público | Valida token e política de password. |
| `POST /reports` | Público | Criação de denúncia anónima. |
| `POST /reports/verify` | Público | Verificação por tracking code. |
| `POST /reports/{id}/attachments` | Público com contexto de denúncia | Upload de evidência validada. |
| `POST /reports/{id}/attachments/list` | Público com contexto de denúncia | Lista metadados de anexos. |
| `POST /reports/download` | Público com tracking code | Gera download da denúncia. |

## Endpoints internos

| Grupo | `ADMIN` | `ANALYST` | `AUDITOR` | Notas |
| --- | --- | --- | --- | --- |
| `/admin/**` | Permitido | Negado | Negado | Gestão de utilizadores, painel admin, auditoria/segurança e backups. |
| `/analyst/**` | Permitido | Permitido | Negado | Workbench de analista; serviços aplicam regras de propriedade. |
| `/audit/**` | Permitido | Negado | Permitido | Logs, alertas, evidência de casos fechados e verificação de backups. |
| `/auth/logout` | Permitido | Permitido | Permitido | Revoga o JWT actual. |
| `/auth/password/change` | Permitido | Permitido | Permitido | Requer JWT autenticado. |

## Verificações nos serviços

| Fluxo | Controlo |
| --- | --- |
| Actualizações por analista | O analista deve estar atribuído ou autorizado pelas regras do serviço. |
| Pacotes de evidência | Limitados a fluxos internos e condições de caso fechado. |
| Criar/repor/descarregar backups | Rotas admin-only. |
| Verificar backups | Rotas de admin e auditor. |
| Auditoria/alertas | Grupos admin e auditor. |

## Notas de segurança

- A protecção de rotas é centralizada em `SecurityConfig`.
- DTOs, validação e regras de serviço continuam necessários depois da
  autorização por rota.
- Páginas estáticas são públicas, mas dados protegidos só chegam por APIs
  autenticadas.
- MFA admin é um controlo de autenticação, não substitui RBAC.
