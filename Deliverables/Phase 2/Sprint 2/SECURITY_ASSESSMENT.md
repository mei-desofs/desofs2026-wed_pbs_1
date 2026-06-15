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
| Uploads | malware, path traversal, content spoofing | Allowlist, MIME/magic bytes, scanner, nomes gerados, quarentena. |
| Analyst workflows | EoP, IDOR, alteracao indevida | RBAC, ownership, workflow validation, optimistic locking. |
| Auditoria | repudiation/tampering | correlationId, integrityHash, DTOs sem segredos. |
| Backups/packages | tampering, ZIP Slip, path traversal | HMAC/hash manifests, canonical path checks, verify before restore. |
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
- verificacao de backups antes de restore;
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
- tokens nao persistidos em browser storage;
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
| Upload hardening | Implementado | MIME/magic bytes/scanner/path checks. |
| Auditoria/alertas | Implementado | Logs e alertas com integrity metadata. |
| Backups | Implementado | Manifestos, hashes, HMAC, verify/restore. |
| SCA/SBOM | Implementado | Dependency-Check e CycloneDX. |
| SAST | Implementado como evidencia | CodeQL, SpotBugs, SonarCloud. |
| DAST | Implementado baseline | ZAP baseline e runtime probes. |
| IAST | Parcial | Evidencia runtime/IAST-like, nao agente completo. |
| Instalacao segura | Documentado | Secrets, perfis, PostgreSQL, checklist. |

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

## 6. Conclusao

Para o ambito DESOFS, o projecto demonstra uma abordagem completa: ameacas
foram identificadas, mitigacoes foram implementadas, testes validam comportamento
esperado e a pipeline produz evidencia. As limitacoes restantes sao claras e
maioritariamente operacionais.

## 7. Revisao critica final

Pontos fortes preservados da revisao final:

- dominio claro: denuncia anonima, analise interna e evidencia de auditoria;
- modelo de roles pequeno e facil de rever: `ADMIN`, `ANALYST`, `AUDITOR` e
  denunciante anonimo;
- separacao entre fluxos publicos e APIs internas protegidas;
- mitigacoes concretas para uploads, path traversal, ZIP Slip, backups e
  pacotes de evidencia;
- auditoria, alertas e integridade adicionam rastreabilidade alem de CRUD;
- pipeline com build/testes, SCA, SAST, DAST baseline, SBOM e secrets scan.

Claims que devem continuar delimitados:

| Claim | Limite correcto |
| --- | --- |
| MFA | Implementado para roles internas; canal real de producao ainda e futuro. |
| DAST | ZAP baseline e probes runtime; nao e pentest autenticado completo. |
| IAST | Evidencia runtime/IAST-like; sem agente IAST completo. |
| Producao | Ha guia prod-like, mas faltam controlos operacionais externos. |
| ASVS | Mapa Sprint 2 em Markdown; spreadsheet Sprint 1 e historica. |

Com esta consolidacao, `FINAL_PROJECT_REVIEW.md` deixou de ser necessario como
ficheiro solto: o seu conteudo util fica distribuido entre este documento e o
relatorio principal [PHASE2_SPRINT2_REPORT.md](PHASE2_SPRINT2_REPORT.md).
