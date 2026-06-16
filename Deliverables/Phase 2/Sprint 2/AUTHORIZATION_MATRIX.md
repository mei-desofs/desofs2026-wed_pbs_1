# Matriz completa de autorizacao e endpoints

Este documento e o anexo tecnico de endpoints. Complementa o relatorio principal
com uma visao exacta das rotas expostas pelo backend e dos controlos de acesso
aplicados.

## 1. Modelo de acesso

| Actor | Autenticacao | Permissoes principais |
| --- | --- | --- |
| Denunciante anonimo | Sem conta/JWT | Submeter denuncia, verificar tracking code, enviar/listar/descarregar anexos autorizados por tracking code. |
| `ANALYST` | Password + MFA + JWT | Trabalhar casos elegiveis/atribuidos, actualizar estado/prioridade/notas, consultar anexos internos e gerar pacotes. |
| `AUDITOR` | Password + MFA + JWT | Consultar auditoria, alertas, casos fechados, verificar packages e backups. |
| `ADMIN` | Password + MFA + JWT | Gerir utilizadores, consultar auditoria/alertas, oversight de analyst routes, gerir backups. |

Nao existe role `USER` activa. A base de dados pode conter dados legados, mas a
constraint e a aplicacao usam apenas `ADMIN`, `ANALYST` e `AUDITOR`.

## 2. Regras Spring Security

| Grupo | Regra |
| --- | --- |
| Static pages/assets | Permitidos publicamente. |
| `POST /auth/login` | Publico. |
| `POST /auth/mfa/verify` | Publico com challenge valido. |
| `POST /auth/password-reset/request` | Publico com resposta generica. |
| `POST /auth/password-reset/confirm` | Publico com token valido. |
| `POST /security/csp-report` | Publico, sem CSRF, para relatorios CSP sanitizados. |
| `POST /reports/**` publico | Permitido, mas validado por tracking code/rate limit quando aplicavel. |
| `/admin/**` | `hasRole("ADMIN")`. |
| `/analyst/**` | `hasAnyRole("ANALYST", "ADMIN")`. |
| `/audit/**` | `hasAnyRole("AUDITOR", "ADMIN")`. |
| Outros endpoints | Autenticacao exigida. |

## 3. Endpoints de autenticacao

| Metodo | Endpoint | Request | Acesso | Controlos |
| --- | --- | --- | --- | --- |
| POST | `/auth/login` | `LoginRequest` JSON | Publico | Rate limit por username/IP, password via AuthenticationManager, bloqueio de inactive user, audit log de falha. |
| POST | `/auth/mfa/verify` | `MfaVerifyRequest` JSON | Publico com challenge | Challenge de uso unico, TTL curto, role ainda activa, JWT so depois de MFA. |
| POST | `/auth/logout` | Bearer JWT | Interno | Revoga token e regista audit log. |
| POST | `/auth/password/change` | `ChangePasswordRequest` JSON | Interno | Exige password actual e nova password valida. |
| POST | `/auth/password-reset/request` | `PasswordResetRequest` JSON | Publico | Resposta generica para evitar enumeracao. |
| POST | `/auth/password-reset/confirm` | `PasswordResetConfirmRequest` JSON | Publico | Token valido, nao expirado/reutilizado, password policy. |

## 3.1 Endpoint de security reporting

| Metodo | Endpoint | Request | Acesso | Controlos |
| --- | --- | --- | --- | --- |
| POST | `/security/csp-report` | CSP report body | Publico | Ignorado por CSRF para permitir reports do browser; regista alerta sanitizado e resposta generica com correlation id. |

## 4. Endpoints publicos de denuncia

| Metodo | Endpoint | Request | Acesso | Controlos |
| --- | --- | --- | --- | --- |
| POST | `/reports` | `CreateReportRequest` JSON | Publico | Bean Validation, DTO, criacao de tracking code, nao exige identidade. |
| POST | `/reports/verify` | `VerifyTrackingCodeRequest` JSON | Publico | Rate limit de tracking, formato de codigo, erro controlado. |
| POST | `/reports/{id}/attachments` | Multipart `files`, `trackingCode` | Publico com tracking code | Rate limit de upload, max files, extensao/MIME/magic bytes, nome gerado, scanner local EICAR/quarentena. |
| POST | `/reports/{id}/attachments/list` | `VerifyTrackingCodeRequest` JSON | Publico com tracking code | Rate limit e verificacao de posse por tracking code. |
| POST | `/reports/download` | `DownloadRequest` JSON | Publico com tracking code | Rate limit, attachmentId positivo, tracking code, path canonical. |

## 5. Endpoints de analista

| Metodo | Endpoint | Roles | Finalidade | Controlos adicionais |
| --- | --- | --- | --- | --- |
| GET | `/analyst/panel` | `ANALYST`, `ADMIN` | Health/access check do painel. | JWT valido. |
| GET | `/analyst/reports` | `ANALYST`, `ADMIN` | Listar denuncias elegiveis. | Redaccao/ownership para analistas. |
| POST | `/analyst/reports/{id}/assign` | `ANALYST`, `ADMIN` | Assumir caso. | Impede assumir caso ja atribuido indevidamente. |
| PATCH | `/analyst/reports/{id}/status` | `ANALYST`, `ADMIN` | Alterar estado. | Validacao enum, workflow, ownership, optimistic locking. |
| PATCH | `/analyst/reports/{id}/priority` | `ANALYST`, `ADMIN` | Alterar prioridade. | Validacao enum e ownership. |
| PATCH | `/analyst/reports/{id}/notes` | `ANALYST`, `ADMIN` | Alterar notas internas. | Limites/DTO e ownership. |
| GET | `/analyst/reports/{id}/case-review` | `ANALYST`, `ADMIN` | Consultar review. | Analista nao ve caso de outro analista. |
| GET | `/analyst/my-cases` | `ANALYST`, `ADMIN` | Listar casos atribuidos. | Escopo por utilizador autenticado. |
| GET | `/analyst/reports/{id}/attachments` | `ANALYST`, `ADMIN` | Listar anexos internos. | Ownership. |
| GET | `/analyst/attachments/{attachmentId}/download` | `ANALYST`, `ADMIN` | Descarregar anexo. | Ownership e path canonical. |
| POST | `/analyst/reports/{id}/case-package` | `ANALYST`, `ADMIN` | Gerar pacote de evidencia. | Caso fechado/autorizacao; admin tem oversight. |

## 6. Endpoints de auditor

| Metodo | Endpoint | Roles | Finalidade | Controlos adicionais |
| --- | --- | --- | --- | --- |
| GET | `/audit/logs` | `AUDITOR`, `ADMIN` | Consultar audit logs. | DTO sem segredos. |
| GET | `/audit/security-alerts` | `AUDITOR`, `ADMIN` | Consultar alertas. | DTO sem tokens/passwords. |
| GET | `/audit/cases/closed` | `AUDITOR`, `ADMIN` | Historico de casos fechados. | Apenas metadados relevantes. |
| GET | `/audit/cases/{reportId}/evidence-package/verify` | `AUDITOR`, `ADMIN` | Verificar package. | Sem expor paths/nomes sensiveis. |
| GET | `/audit/backups` | `AUDITOR`, `ADMIN` | Listar backups. | Read-only para auditor. |
| GET | `/audit/backups/{filename}/verify` | `AUDITOR`, `ADMIN` | Verificar backup. | Rejeita traversal/tampering. |
| GET | `/audit/backups/{filename}/manifest` | `AUDITOR`, `ADMIN` | Consultar manifesto. | Read-only. |

## 7. Endpoints de admin

| Metodo | Endpoint | Roles | Finalidade | Controlos adicionais |
| --- | --- | --- | --- | --- |
| GET | `/admin/panel` | `ADMIN` | Health/access check. | JWT + MFA previa. |
| GET | `/admin/users` | `ADMIN` | Listar utilizadores. | DTO sem password hash. |
| POST | `/admin/users` | `ADMIN` | Criar utilizador. | Role allowlist, password policy, active flag controlado. |
| PUT | `/admin/users/{id}` | `ADMIN` | Editar utilizador. | Impede remover/demover ultimo admin activo. |
| PATCH | `/admin/users/{id}/activate` | `ADMIN` | Activar utilizador. | Audit log. |
| PATCH | `/admin/users/{id}/deactivate` | `ADMIN` | Desactivar utilizador. | Proteccao ultimo admin activo. |
| DELETE | `/admin/users/{id}` | `ADMIN` | Remocao logica. | Desactivacao em vez de delete fisico. |
| POST | `/admin/users/{id}/password-reset` | `ADMIN` | Iniciar reset de password. | Admin nao escolhe nem ve a nova password. |
| GET | `/admin/audit-logs` | `ADMIN` | Consultar logs. | DTO com integrity hash. |
| GET | `/admin/security-alerts` | `ADMIN` | Consultar alertas. | DTO com integrity hash. |

## 8. Endpoints de backups admin

| Metodo | Endpoint | Roles | Finalidade | Controlos adicionais |
| --- | --- | --- | --- | --- |
| POST | `/admin/backups` | `ADMIN` | Criar backup. | Manifesto, hashes, HMAC. |
| GET | `/admin/backups` | `ADMIN` | Listar backups. | Apenas ficheiros validos no directorio base. |
| GET | `/admin/backups/{filename}/download` | `ADMIN` | Descarregar backup. | Content-Disposition seguro e path canonical. |
| POST | `/admin/backups/{filename}/verify` | `ADMIN` | Verificar backup. | Detecta tampering/manifest mismatch. |
| POST | `/admin/backups/{filename}/restore` | `ADMIN` | Repor backup para staging. | Validacao antes de restore, CSRF, JWT admin e reautenticacao por `X-Reauth-Password`; resposta sem path interno. |

## 9. Matriz resumida por role

| Funcionalidade | Publico | Analyst | Auditor | Admin |
| --- | --- | --- | --- | --- |
| Submeter denuncia | Sim | Sim, como publico | Sim, como publico | Sim, como publico |
| Verificar tracking code | Sim | Sim, como publico | Sim, como publico | Sim, como publico |
| Login password + MFA | N/A | Sim | Sim | Sim |
| Painel analista | Nao | Sim | Nao | Sim |
| Actualizar casos | Nao | Sim, com ownership | Nao | Sim |
| Gerar package | Nao | Sim, se autorizado | Nao | Sim |
| Ver auditoria | Nao | Nao | Sim | Sim |
| Verificar backup | Nao | Nao | Sim | Sim |
| Criar/restaurar backup | Nao | Nao | Nao | Sim |
| Gerir utilizadores | Nao | Nao | Nao | Sim |

## 10. Testes que suportam a matriz

- `RbacAuthorizationMatrixTest`
- `AdminMfaAuthenticationTest`
- `AuditorAuthorizationTest`
- `AnalystCaseOwnershipTest`
- `AdminAuthorizationTest`
- `AdminUserManagementSecurityTest`
- `AuthenticationSecurityIntegrationTest`
- `AdminBackupControllerSecurityTest`
- `SecurityHeadersTest`
- `SecurityMonitoringServiceTest`
