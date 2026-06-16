# Matriz completa de autorização e endpoints

Este documento é o anexo técnico de endpoints. Complementa o relatório principal com uma visão exata das rotas expostas pelo backend e dos controlos de acesso aplicados.

## 1. Modelo de acesso

| Actor               | Autenticação         | Permissões principais                                                                                               |
| ------------------- | -------------------- | ------------------------------------------------------------------------------------------------------------------- |
| Denunciante anónimo | Sem conta/JWT        | Submeter denúncia, verificar tracking code, enviar/listar/descarregar anexos autorizados por tracking code.         |
| `ANALYST`           | Password + MFA + JWT | Trabalhar casos elegíveis/atribuídos, atualizar estado/prioridade/notas, consultar anexos internos e gerar pacotes. |
| `AUDITOR`           | Password + MFA + JWT | Consultar auditoria, alertas, casos fechados, verificar packages e backups.                                         |
| `ADMIN`             | Password + MFA + JWT | Gerir utilizadores, consultar auditoria/alertas, oversight de analyst routes, gerir backups.                        |

Não existe a role `USER` ativa. A base de dados pode conter dados legados, mas a constraint e a aplicação usam apenas `ADMIN`, `ANALYST` e `AUDITOR`.

## 2. Regras Spring Security

| Grupo                               | Regra                                                                  |
| ----------------------------------- | ---------------------------------------------------------------------- |
| Static pages/assets                 | Permitidos publicamente.                                               |
| `POST /auth/login`                  | Público.                                                               |
| `POST /auth/mfa/verify`             | Público com challenge válido.                                          |
| `POST /auth/password-reset/request` | Público com resposta genérica.                                         |
| `POST /auth/password-reset/confirm` | Público com token válido.                                              |
| `POST /reports/**` público          | Permitido, mas validado por tracking code/rate limit quando aplicável. |
| `/admin/**`                         | `hasRole("ADMIN")`.                                                    |
| `/analyst/**`                       | `hasAnyRole("ANALYST", "ADMIN")`.                                      |
| `/audit/**`                         | `hasAnyRole("AUDITOR", "ADMIN")`.                                      |
| Outros endpoints                    | Autenticação exigida.                                                  |

## 3. Endpoints de autenticação

| Método | Endpoint                       | Request                            | Acesso                | Controlos                                                                                                      |
| ------ | ------------------------------ | ---------------------------------- | --------------------- | -------------------------------------------------------------------------------------------------------------- |
| POST   | `/auth/login`                  | `LoginRequest` JSON                | Público               | Rate limit por username/IP, password via AuthenticationManager, bloqueio de inactive user, audit log de falha. |
| POST   | `/auth/mfa/verify`             | `MfaVerifyRequest` JSON            | Público com challenge | Challenge de uso único, TTL curto, role ainda ativa, JWT só depois de MFA.                                     |
| POST   | `/auth/logout`                 | Bearer JWT                         | Interno               | Revoga o token e regista audit log.                                                                            |
| POST   | `/auth/password/change`        | `ChangePasswordRequest` JSON       | Interno               | Exige a password atual e uma nova password válida.                                                             |
| POST   | `/auth/password-reset/request` | `PasswordResetRequest` JSON        | Público               | Resposta genérica para evitar enumeração.                                                                      |
| POST   | `/auth/password-reset/confirm` | `PasswordResetConfirmRequest` JSON | Público               | Token válido, não expirado/reutilizado, password policy.                                                       |

## 4. Endpoints públicos de denúncia

| Método | Endpoint                         | Request                           | Acesso                    | Controlos                                                                                    |
| ------ | -------------------------------- | --------------------------------- | ------------------------- | -------------------------------------------------------------------------------------------- |
| POST   | `/reports`                       | `CreateReportRequest` JSON        | Público                   | Bean Validation, DTO, criação de tracking code, não exige identidade.                        |
| POST   | `/reports/verify`                | `VerifyTrackingCodeRequest` JSON  | Público                   | Rate limit de tracking, formato do código, erro controlado.                                  |
| POST   | `/reports/{id}/attachments`      | Multipart `files`, `trackingCode` | Público com tracking code | Rate limit de upload, max files, extensão/MIME/magic bytes, nome gerado, scanner/quarentena. |
| POST   | `/reports/{id}/attachments/list` | `VerifyTrackingCodeRequest` JSON  | Público com tracking code | Rate limit e verificação de posse por tracking code.                                         |
| POST   | `/reports/download`              | `DownloadRequest` JSON            | Público com tracking code | Rate limit, attachmentId positivo, tracking code, path canonical.                            |

## 5. Endpoints de analista

| Método | Endpoint                                       | Roles              | Finalidade                     | Controlos adicionais                                     |
| ------ | ---------------------------------------------- | ------------------ | ------------------------------ | -------------------------------------------------------- |
| GET    | `/analyst/panel`                               | `ANALYST`, `ADMIN` | Health/access check do painel. | JWT válido.                                              |
| GET    | `/analyst/reports`                             | `ANALYST`, `ADMIN` | Listar denúncias elegíveis.    | Redação/ownership para analistas.                        |
| POST   | `/analyst/reports/{id}/assign`                 | `ANALYST`, `ADMIN` | Assumir caso.                  | Impede assumir caso já atribuído indevidamente.          |
| PATCH  | `/analyst/reports/{id}/status`                 | `ANALYST`, `ADMIN` | Alterar estado.                | Validação enum, workflow, ownership, optimistic locking. |
| PATCH  | `/analyst/reports/{id}/priority`               | `ANALYST`, `ADMIN` | Alterar prioridade.            | Validação enum e ownership.                              |
| PATCH  | `/analyst/reports/{id}/notes`                  | `ANALYST`, `ADMIN` | Alterar notas internas.        | Limites/DTO e ownership.                                 |
| GET    | `/analyst/reports/{id}/case-review`            | `ANALYST`, `ADMIN` | Consultar review.              | Analista não vê caso de outro analista.                  |
| GET    | `/analyst/my-cases`                            | `ANALYST`, `ADMIN` | Listar casos atribuídos.       | Escopo por utilizador autenticado.                       |
| GET    | `/analyst/reports/{id}/attachments`            | `ANALYST`, `ADMIN` | Listar anexos internos.        | Ownership.                                               |
| GET    | `/analyst/attachments/{attachmentId}/download` | `ANALYST`, `ADMIN` | Descarregar anexo.             | Ownership e path canonical.                              |
| POST   | `/analyst/reports/{id}/case-package`           | `ANALYST`, `ADMIN` | Gerar pacote de evidência.     | Caso fechado/autorização; admin tem oversight.           |

## 6. Endpoints de auditor

| Método | Endpoint                                          | Roles              | Finalidade                   | Controlos adicionais             |
| ------ | ------------------------------------------------- | ------------------ | ---------------------------- | -------------------------------- |
| GET    | `/audit/logs`                                     | `AUDITOR`, `ADMIN` | Consultar audit logs.        | DTO sem segredos.                |
| GET    | `/audit/security-alerts`                          | `AUDITOR`, `ADMIN` | Consultar alertas.           | DTO sem tokens/passwords.        |
| GET    | `/audit/cases/closed`                             | `AUDITOR`, `ADMIN` | Histórico de casos fechados. | Apenas metadados relevantes.     |
| GET    | `/audit/cases/{reportId}/evidence-package/verify` | `AUDITOR`, `ADMIN` | Verificar package.           | Sem expor paths/nomes sensíveis. |
| GET    | `/audit/backups`                                  | `AUDITOR`, `ADMIN` | Listar backups.              | Read-only para auditor.          |
| GET    | `/audit/backups/{filename}/verify`                | `AUDITOR`, `ADMIN` | Verificar backup.            | Rejeita traversal/tampering.     |
| GET    | `/audit/backups/{filename}/manifest`              | `AUDITOR`, `ADMIN` | Consultar manifesto.         | Read-only.                       |

## 7. Endpoints de admin

| Método | Endpoint                           | Roles   | Finalidade                 | Controlos adicionais                                     |
| ------ | ---------------------------------- | ------- | -------------------------- | -------------------------------------------------------- |
| GET    | `/admin/panel`                     | `ADMIN` | Health/access check.       | JWT + MFA prévia.                                        |
| GET    | `/admin/users`                     | `ADMIN` | Listar utilizadores.       | DTO sem password hash.                                   |
| POST   | `/admin/users`                     | `ADMIN` | Criar utilizador.          | Role allowlist, password policy, active flag controlado. |
| PUT    | `/admin/users/{id}`                | `ADMIN` | Editar utilizador.         | Impede remover/demover o último admin ativo.             |
| PATCH  | `/admin/users/{id}/activate`       | `ADMIN` | Ativar utilizador.         | Audit log.                                               |
| PATCH  | `/admin/users/{id}/deactivate`     | `ADMIN` | Desativar utilizador.      | Proteção do último admin ativo.                          |
| DELETE | `/admin/users/{id}`                | `ADMIN` | Remoção lógica.            | Desativação em vez de delete físico.                     |
| POST   | `/admin/users/{id}/password-reset` | `ADMIN` | Iniciar reset de password. | O admin não escolhe nem vê a nova password.              |
| GET    | `/admin/audit-logs`                | `ADMIN` | Consultar logs.            | DTO com integrity hash.                                  |
| GET    | `/admin/security-alerts`           | `ADMIN` | Consultar alertas.         | DTO com integrity hash.                                  |

## 8. Endpoints de backups admin

| Método | Endpoint                             | Roles   | Finalidade                 | Controlos adicionais                                                                                             |
| ------ | ------------------------------------ | ------- | -------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| POST   | `/admin/backups`                     | `ADMIN` | Criar backup.              | Manifesto, hashes, HMAC.                                                                                         |
| GET    | `/admin/backups`                     | `ADMIN` | Listar backups.            | Apenas ficheiros válidos no diretório base.                                                                      |
| GET    | `/admin/backups/{filename}/download` | `ADMIN` | Descarregar backup.        | Content-Disposition seguro e path canonical.                                                                     |
| POST   | `/admin/backups/{filename}/verify`   | `ADMIN` | Verificar backup.          | Deteta tampering/manifest mismatch.                                                                              |
| POST   | `/admin/backups/{filename}/restore`  | `ADMIN` | Repor backup para staging. | Validação antes do restore, CSRF, JWT admin e reautenticação por `X-Reauth-Password`; resposta sem path interno. |

## 9. Matriz resumida por role

| Funcionalidade          | Público | Analyst            | Auditor           | Admin             |
| ----------------------- | ------- | ------------------ | ----------------- | ----------------- |
| Submeter denúncia       | Sim     | Sim, como público  | Sim, como público | Sim, como público |
| Verificar tracking code | Sim     | Sim, como público  | Sim, como público | Sim, como público |
| Login password + MFA    | N/A     | Sim                | Sim               | Sim               |
| Painel analista         | Não     | Sim                | Não               | Sim               |
| Atualizar casos         | Não     | Sim, com ownership | Não               | Sim               |
| Gerar package           | Não     | Sim, se autorizado | Não               | Sim               |
| Ver auditoria           | Não     | Não                | Sim               | Sim               |
| Verificar backup        | Não     | Não                | Sim               | Sim               |
| Criar/restaurar backup  | Não     | Não                | Não               | Sim               |
| Gerir utilizadores      | Não     | Não                | Não               | Sim               |

## 10. Testes que suportam a matriz

* `RbacAuthorizationMatrixTest`
* `AdminMfaAuthenticationTest`
* `AuditorAuthorizationTest`
* `AnalystCaseOwnershipTest`
* `AdminAuthorizationTest`
* `AdminUserManagementSecurityTest`
* `AuthenticationSecurityIntegrationTest`
* `AdminBackupControllerSecurityTest`
