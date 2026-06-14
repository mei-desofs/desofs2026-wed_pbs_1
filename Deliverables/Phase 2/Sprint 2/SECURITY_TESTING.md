# Testes de seguranca e validacao

## 1. Resultado local

Comando executado na linha actual:

```powershell
cd ghostreport
.\mvnw.cmd test
```

Resultado: 180 testes, 0 falhas, 0 erros, 0 skipped.

## 2. Estrategia

Os testes do GhostReport foram organizados para validar nao so "happy paths",
mas tambem abuso, autorizacao negativa, erros genericos, integridade e limites
operacionais. Isto segue a linha da Phase 1: cada ameaca STRIDE deve ter pelo
menos uma mitigacao implementada e, sempre que possivel, um teste que a prove.

## 3. Categorias de testes

| Categoria | Testes/classes | O que validam |
| --- | --- | --- |
| Contexto/config | `GhostreportApplicationTests`, `SecurityConfigurationValidatorTest`, `DataInitializerDisabledTest`, `SchemaMigrationScriptTest` | Arranque, secrets fracos, seed users, schema metadata. |
| Autenticacao/MFA/JWT | `AdminMfaAuthenticationTest`, `AuthenticationSecurityIntegrationTest`, `JwtServiceSecurityTest`, `JwtRevocationPersistenceIntegrationTest` | Password login, MFA para roles internas, token claims, revogacao, expiracao, kid, issuer/audience. |
| Password reset/policy | `PasswordPolicyAndResetSecurityTest` | Password comprometida, reutilizacao, token expirado/reutilizado. |
| RBAC | `RbacAuthorizationMatrixTest`, `AdminAuthorizationTest`, `AuditorAuthorizationTest` | Endpoints permitidos/negados por role. |
| Admin lifecycle | `AdminUserManagementSecurityTest` | Activar/desactivar, editar roles, ultimo admin activo, audit logs. |
| Analyst ownership | `AnalystCaseOwnershipTest`, `BusinessLogicWorkflowSecurityTest` | Ownership, casos de outro analista, transitions, optimistic locking. |
| Public reports | `PublicReportFlowIntegrationTest`, `TrackingCodeEnumerationTest` | Criacao anonima, tracking code, erros seguros, enumeracao. |
| Uploads/files | `ReportControllerAttachmentUploadTest`, `FileStorageServiceTest`, `SafeFilenameSecurityTest`, `SafeFilenameTest` | MIME, magic bytes, traversal, malware/quarantine, limites, paths. |
| Auditoria/logging | `AuditLogSecurityTest`, `AnonymousDataLoggingTest`, `RuntimeSecurityEventLoggingTest` | Nao guardar passwords/tokens/tracking code, alertas e correlationId. |
| Backups/packages | `BackupServiceIntegrationTest`, `AdminBackupControllerSecurityTest`, `CasePackageServiceIntegrationTest` | Manifestos, tampering, restore, traversal, packages. |
| Frontend | `FrontendXssDataExposureTest`, `FrontendNavbarVisibilityTest`, `CsrfCookieAttributesTest` | XSS sinks, tokens em storage, tracking code em URL, navs escondidas, CSRF cookie. |
| Headers/erros | `SecurityHeadersTest`, `ErrorHandlingSecurityTest`, `ApiValidationContractTest` | Headers, JSON errors genericos, validacao de contratos. |
| Rate limiting | `RateLimiterServiceTest`, `LoginRateLimitSecurityTest` | Limites, reset de janela, brute force alert. |

## 4. Matriz de validacao de endpoints

| Fluxo | Validacoes | Testes |
| --- | --- | --- |
| `/auth/login` | Campos obrigatorios, rate limit, inactive user, erros genericos. | `AuthenticationSecurityIntegrationTest`, `LoginRateLimitSecurityTest`. |
| `/auth/mfa/verify` | Challenge obrigatorio, codigo de 6 digitos, TTL, uso unico, role activa. | `AdminMfaAuthenticationTest`. |
| `/auth/password-reset/*` | Resposta generica, token expirado/reutilizado, password policy. | `PasswordPolicyAndResetSecurityTest`. |
| `/reports` | Titulo/descricao/categoria, DTO, resposta sem hash interno. | `PublicReportFlowIntegrationTest`, `ApiValidationContractTest`. |
| `/reports/verify` | Tracking code format, erro seguro, rate limit/enumeracao. | `TrackingCodeEnumerationTest`, `PublicReportFlowIntegrationTest`. |
| `/reports/{id}/attachments` | Max files, size, MIME, magic bytes, filename, tracking code. | `ReportControllerAttachmentUploadTest`, `FileStorageServiceTest`. |
| `/analyst/**` | Role, ownership, workflow, status/priority/notes. | `RbacAuthorizationMatrixTest`, `AnalystCaseOwnershipTest`, `BusinessLogicWorkflowSecurityTest`. |
| `/audit/**` | Auditor/admin read-only, sem paths/filenames sensiveis. | `AuditorAuthorizationTest`. |
| `/admin/users/**` | Role allowlist, password policy, ultimo admin activo. | `AdminUserManagementSecurityTest`. |
| `/admin/backups/**` | Admin-only, path traversal, verify before restore. | `AdminBackupControllerSecurityTest`, `BackupServiceIntegrationTest`. |

## 5. STRIDE e testes

| STRIDE | Testes associados |
| --- | --- |
| Spoofing | `AdminMfaAuthenticationTest`, `JwtServiceSecurityTest`, `LoginRateLimitSecurityTest`. |
| Tampering | `BackupServiceIntegrationTest`, `CasePackageServiceIntegrationTest`, `JwtServiceSecurityTest`. |
| Repudiation | `AuditLogSecurityTest`, `RuntimeSecurityEventLoggingTest`. |
| Information Disclosure | `AnonymousDataLoggingTest`, `FrontendXssDataExposureTest`, `ErrorHandlingSecurityTest`. |
| Denial of Service | `RateLimiterServiceTest`, upload size/max files tests. |
| Elevation of Privilege | `RbacAuthorizationMatrixTest`, `AnalystCaseOwnershipTest`, `AdminUserManagementSecurityTest`. |

## 6. Testes frontend

O frontend e estatico, mas tambem foi validado:

- nao usa `innerHTML`/sinks perigosos para dados externos;
- renderiza dados atraves de text nodes/helpers;
- nao guarda bearer tokens em `localStorage`/`sessionStorage`;
- nao coloca tracking code em URLs;
- navs autenticadas começam escondidas;
- MFA existe em admin, analyst e auditor.

## 7. Testes de seguranca runtime/pipeline

Na pipeline, alem de `./mvnw verify`, existe job `dast-scan` que:

- corre testes runtime seleccionados;
- arranca a aplicacao em `localhost:8081`;
- faz probes HTTP;
- verifica logs para fuga de dados sensiveis;
- corre ZAP baseline;
- publica evidencia.

## 8. Limitacoes

- Testes automatizados nao substituem pentest manual.
- ZAP baseline e passivo e nao autenticado.
- IAST e evidencia runtime/IAST-like, nao agent-based.
- Rate limiting e em memoria; ambiente multi-no exigiria mecanismo externo.
