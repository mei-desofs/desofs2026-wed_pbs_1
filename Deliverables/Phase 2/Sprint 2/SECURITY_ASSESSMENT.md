# Avaliacao final de seguranca

## 1. Sumario executivo

GhostReport implementa uma arquitectura coerente de seguranca para o ambito
academico: fluxo anonimo protegido por tracking code, roles internas com
password + MFA + JWT, RBAC, ownership, validacao, upload hardening, auditoria,
alertas, backups verificaveis, SCA/SAST/DAST e documentacao ASVS.

O sistema nao e declarado production-ready. A avaliacao separa controlos
implementados, controlos parcialmente dependentes de operacao e trabalho futuro.

## 2. Superficies de ataque

| Superficie | Risco | Controlos |
| --- | --- | --- |
| Login interno | brute force, spoofing, inactive users | Rate limiting, BCrypt, MFA, audit log, inactive block. |
| JWT | token tampering, replay, role spoofing | Assinatura, issuer/audience/kid, expiracao, revogacao, role match. |
| Tracking code | enumeracao, acesso indevido | Formato, rate limiting, erros genericos, nao colocar em URL. |
| Uploads | malware de teste, path traversal, content spoofing | Allowlist, MIME/magic bytes, scanner local EICAR, nomes gerados, quarentena para rejeicoes. |
| Analyst workflows | EoP, IDOR, alteracao indevida | RBAC, ownership, workflow validation, optimistic locking. |
| Auditoria | repudiation/tampering | correlationId, integrityHash, DTOs sem segredos. |
| Backups/packages | tampering, ZIP Slip, path traversal, uso indevido de restore | HMAC/hash manifests, canonical path checks, verify before restore, reautenticacao admin e respostas sem paths internos. |
| CI/CD | secrets, dependencias vulneraveis, regressao | Gitleaks, Dependency-Check, CycloneDX, SAST, testes. |

## 3. STRIDE aplicado

### Spoofing

Ameacas:

- atacante tenta autenticar-se como role interna;
- token JWT manipulado para alterar `role`;
- utilizador inactivo tenta continuar a usar credenciais.

Mitigacoes:

- password hashing com BCrypt;
- MFA para `ADMIN`, `ANALYST` e `AUDITOR`;
- JWT assinado e validado;
- token role deve corresponder ao utilizador actual;
- inactive users sao bloqueados;
- rate limiting e alertas de brute force.

Evidencia: `AdminMfaAuthenticationTest`, `JwtServiceSecurityTest`,
`AuthenticationSecurityIntegrationTest`, `LoginRateLimitSecurityTest`.

### Tampering

Ameacas:

- alterar backups;
- alterar pacotes de evidencia;
- alterar logs/alertas;
- alterar ficheiros no filesystem por traversal.

Mitigacoes:

- manifestos com hashes/HMAC;
- verificacao de backups antes de restore e reautenticacao do admin;
- rejeicao de manifest tampering;
- `integrityHash` em auditoria/alertas;
- canonical path checks;
- nomes de ficheiro gerados.

Evidencia: `BackupServiceIntegrationTest`, `CasePackageServiceIntegrationTest`,
`FileStorageServiceTest`.

### Repudiation

Ameacas:

- admin nega alteracao de utilizador;
- analyst nega alteracao de caso;
- actor nega logout/login/MFA.

Mitigacoes:

- audit logs para operacoes criticas;
- actor/action/target/details/correlationId;
- security alerts em eventos suspeitos;
- nao guardar segredos nos logs.

Evidencia: `AuditLogSecurityTest`, `RuntimeSecurityEventLoggingTest`,
`AdminUserManagementSecurityTest`.

### Information Disclosure

Ameacas:

- leak de tokens/passwords/tracking codes;
- leak de paths internos;
- XSS no frontend;
- auditor receber dados anonimos sensiveis em excesso.

Mitigacoes:

- DTOs de resposta;
- redaction de logs;
- erros genericos;
- frontend sem sinks perigosos;
- JWT guardado apenas em `sessionStorage` durante a sessao do browser no
  frontend academico, limpo no logout e nao persistido em `localStorage`;
- tracking code nao colocado no URL;
- auditor ve metadados adequados.

Evidencia: `AnonymousDataLoggingTest`, `FrontendXssDataExposureTest`,
`ErrorHandlingSecurityTest`, `AuditorAuthorizationTest`.

### Denial of Service

Ameacas:

- brute force login;
- spam de tracking code;
- uploads grandes/muitos ficheiros;
- abuso de downloads.

Mitigacoes:

- rate limiting por fluxo;
- limite de tamanho;
- limite de ficheiros por request;
- rejeicao antecipada de conteudo invalido;
- respostas controladas.

Evidencia: `RateLimiterServiceTest`, `LoginRateLimitSecurityTest`,
`ReportControllerAttachmentUploadTest`.

### Elevation of Privilege

Ameacas:

- analyst usa endpoints admin/auditor;
- auditor altera casos;
- analyst acede a caso de outro analyst;
- ultimo admin activo e removido;
- role invalida criada por admin.

Mitigacoes:

- regras Spring Security;
- service-level ownership;
- role allowlist;
- proteccao do ultimo admin activo;
- testes negativos por role.

Evidencia: `RbacAuthorizationMatrixTest`, `AnalystCaseOwnershipTest`,
`AuditorAuthorizationTest`, `AdminUserManagementSecurityTest`.

## 4. Avaliacao por controlo

| Controlo | Estado | Observacao |
| --- | --- | --- |
| Denuncia anonima | Implementado | Sem conta de reporter; tracking code controla acompanhamento. |
| Autenticacao interna | Implementado | Password + MFA + JWT. |
| MFA | Implementado | Roles internas exigem MFA por omissao. |
| RBAC | Implementado | Spring Security + testes. |
| Ownership | Implementado | Analistas limitados a casos elegiveis/atribuidos. |
| Validacao input | Implementado | DTOs, Bean Validation, allowlists. |
| Upload hardening | Implementado | MIME/magic bytes, scanner local EICAR, quarantine e path checks. |
| Auditoria/alertas | Implementado | Logs e alertas com integrity metadata. |
| Backups | Implementado | Manifestos, hashes, HMAC, verify/restore para staging com reautenticacao e sem exposicao de path interno na resposta. |
| SCA/SBOM | Implementado | Dependency-Check e CycloneDX. |
| SAST | Implementado como evidencia | CodeQL, SpotBugs, SonarCloud. |
| DAST | Implementado baseline | ZAP baseline e runtime probes; validacao local expandida com 101 probes, 101 passed, 0 failed e 0 skipped. |
| IAST | Parcial | Evidencia runtime/IAST-like, nao agente completo. |
| Instalacao segura | Documentado | Secrets, perfis, PostgreSQL, checklist. |

## 4.1 Avaliacao por cenario demonstravel

Esta tabela resume cenarios que aparecem em testes automatizados, runtime probes
ou documentos de evidencia desta Sprint 2. Nao substitui a matriz completa de
endpoints; serve como guia rapido para apresentacao oral.

| Fluxo | Cenario | Controlo esperado | Resultado/Evidencia |
| --- | --- | --- | --- |
| Denuncia anonima | Submissao sem login | Permitida e gera tracking code sem criar conta de reporter. | Pass: `PublicReportFlowIntegrationTest` e runtime probe `POST /reports`. |
| Tracking code | Codigo invalido ou repetido | Erro controlado e evidencia de anti-enumeracao/rate limit. | Pass: `TrackingCodeEnumerationTest` e probes `POST /reports/verify`. |
| Login interno | Password valida de role interna | Inicia MFA e nao emite JWT final antes do codigo. | Pass: `AdminMfaAuthenticationTest` e probes de `ADMIN`, `ANALYST`, `AUDITOR`. |
| MFA | Challenge reutilizado | Rejeitado apos uso ou invalidacao. | Pass: runtime probes de reutilizacao MFA por role. |
| RBAC admin | `ANALYST` tenta `/admin/users` | Resposta `403`. | Pass: `RbacAuthorizationMatrixTest` e runtime probe. |
| RBAC auditor | `ANALYST` tenta `/audit/logs` | Resposta `403`. | Pass: runtime probe e testes de auditoria/autorizacao. |
| Upload | Extensao proibida, MIME incoerente ou traversal | Pedido rejeitado sem path interno. | Pass: `ReportControllerAttachmentUploadTest`, `FileStorageServiceTest` e runtime probes. |
| Backups | Filename com path traversal | Rejeicao controlada; restore destrutivo nao executado no probe. | Pass: `AdminBackupControllerSecurityTest`, `BackupServiceIntegrationTest` e probes de verify/restore invalido. |
| Headers/browser | Paginas publicas | CSP/HSTS/COOP/COEP/CORP e ausencia de tokens/tracking code em HTML publico. | Pass: `SecurityHeadersTest`, `FrontendXssDataExposureTest` e probes publicos. |
| Logs/erros | Erro ou alerta de seguranca | Sem passwords, bearer tokens, tracking codes, paths internos ou stack traces. | Pass: `ErrorHandlingSecurityTest`, `AnonymousDataLoggingTest` e runtime log sanitization. |

## 5. Riscos residuais

| Risco | Impacto | Mitigacao futura |
| --- | --- | --- |
| Canal MFA real nao integrado | MFA dev usa logs quando activado | Integrar TOTP, email/SMS seguro ou IdP. |
| Rate limiting em memoria | Multi-no nao partilha counters | Redis/API gateway/WAF. |
| Sem Flyway/Liquibase | Drift de schema em producao | Migrações versionadas. |
| Sem SIEM/WORM | Retencao/auditoria operacional limitada | Exportar logs para storage imutavel/SIEM. |
| ZAP nao autenticado | Cobertura DAST incompleta | Contextos autenticados por role. |
| Sem IAST agent-based | Menos visibilidade runtime interna | Integrar ferramenta IAST. |
| Sem secret manager externo | Gestao manual de secrets | Vault/cloud secret manager. |
| Backups sem encriptacao/retencao externa | ZIPs protegidos por integridade, mas nao por confidencialidade aplicacional nem politica externa de retencao | Encriptacao/retencao em storage externo ou processo operacional dedicado. |
| Malware scanning local limitado | Scanner actual cobre assinatura EICAR/local; nao substitui AV/sandbox empresarial | Integrar scanner externo/servico de analise em producao. |

## 6. Conclusao

Para o ambito DESOFS, o projecto demonstra uma abordagem completa: ameacas
foram identificadas, mitigacoes foram implementadas, testes validam comportamento
esperado e a pipeline produz evidencia. As limitacoes restantes sao claras e
maioritariamente operacionais.

