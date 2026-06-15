# GhostReport - Relatorio Final Phase 2 Sprint 2

## 1. Introducao

GhostReport e uma plataforma web para submissao anonima de denuncias,
acompanhamento por codigo de tracking, analise interna por analistas, consulta
de auditoria por auditores e administracao segura da aplicacao. O projecto foi
desenvolvido no contexto de DESOFS com foco em engenharia de software segura:
modelacao de ameacas, desenho orientado ao dominio, controlos defensivos,
automatizacao DevSecOps e evidencia tecnica verificavel.

Este documento e o relatorio principal da Phase 2 Sprint 2. Ao contrario de um
sumario curto, pretende explicar o sistema final de ponta a ponta: requisitos,
arquitectura, roles, endpoints, pipeline, automacoes, testes, STRIDE,
mitigacoes, SCA/SAST/DAST/IAST-like, configuracao segura, evidencias ASVS,
limitacoes e trabalho futuro.

Os documentos complementares desta pasta funcionam como anexos tecnicos:

- [AUTHORIZATION_MATRIX.md](AUTHORIZATION_MATRIX.md)
- [SECURITY_TESTING.md](SECURITY_TESTING.md)
- [DEVSECOPS_PIPELINE.md](DEVSECOPS_PIPELINE.md)
- [SCA_TRIAGE.md](SCA_TRIAGE.md)
- [SPOTBUGS_TRIAGE.md](SPOTBUGS_TRIAGE.md)
- [IAST_RUNTIME_SECURITY.md](IAST_RUNTIME_SECURITY.md)
- [SECURE_INSTALLATION.md](SECURE_INSTALLATION.md)
- [SECURITY_CONFIGURATION_ASSESSMENT.md](SECURITY_CONFIGURATION_ASSESSMENT.md)
- [SECURITY_ASSESSMENT.md](SECURITY_ASSESSMENT.md)
- [ASVS_EVIDENCE.md](ASVS_EVIDENCE.md)
- [FINAL_DEMO_GUIDE.md](FINAL_DEMO_GUIDE.md)

## 2. Relacao com Phase 1 e Sprint 1

Na Phase 1 foi definida a base de seguranca do GhostReport: actores, trust
boundaries, abuse cases, attack trees, DFDs, modelo DDD e STRIDE. O objectivo
principal era identificar ameacas antes da implementacao, especialmente:

- exposicao da identidade do denunciante;
- enumeracao ou abuso de codigos de tracking;
- acesso indevido a dados internos;
- upload de ficheiros maliciosos;
- path traversal e ZIP Slip;
- alteracao de evidencias, logs ou backups;
- uso indevido de roles administrativas.

Na Phase 2 Sprint 1 foram implementados os controlos base: autenticacao JWT,
RBAC, DTOs/validacao, submissao anonima, tracking code, upload seguro,
auditoria, alertas de seguranca, backups e pacotes de evidencia.

Na Sprint 2 o trabalho passou de "funcionalidade segura" para "entrega segura".
Foram reforcados MFA para todas as roles internas, pipeline DevSecOps,
SCA/SAST/DAST, evidencia runtime/IAST-like, instalacao segura, avaliacao de
configuracao, documentacao ASVS e revisao final.

## 3. Objectivos da Sprint 2

Os objectivos concretos da Sprint 2 foram:

| Objectivo | Resultado |
| --- | --- |
| Consolidar a aplicacao final | Fluxos anonimos, analista, auditor e admin documentados e testados. |
| Reforcar autenticacao | MFA antes da emissao de JWT para `ADMIN`, `ANALYST` e `AUDITOR`. |
| Validar autorizacao | Matriz de endpoints e testes RBAC/ownership. |
| Produzir evidencia DevSecOps | Workflows com build, testes, SCA, SAST, DAST, SBOM, secrets scan e PIT. |
| Documentar seguranca runtime | Evidencia IAST-like sem afirmar IAST agent-based. |
| Corrigir dependencias vulneraveis | Spring Security `6.5.10` substituido por `6.5.11` via Spring Boot BOM. |
| Organizar entrega final | Pasta Sprint 2 limpa, com relatorio principal e anexos uteis. |

## 4. Requisitos do projecto e cumprimento

| Requisito | Implementacao no GhostReport |
| --- | --- |
| Backend web API | Spring Boot REST controllers. |
| Base de dados relacional | PostgreSQL em runtime/dev/prod-like; H2 apenas em testes. |
| Pelo menos tres agregados DDD | `Report`, `CaseReview`, `User`; entidades de auditoria, alertas, anexos, tokens e backups complementam o dominio. |
| Pelo menos tres roles | `ADMIN`, `ANALYST`, `AUDITOR`. |
| Funcionalidade de sistema operativo no backend | Uploads em filesystem, downloads, pacotes ZIP de evidencia, backups ZIP, verificacao de manifestos. |
| Desenvolvimento seguro | Validacao, RBAC, MFA, auditoria, SCA/SAST/DAST, testes e documentacao ASVS. |

## 5. Arquitectura final

### 5.1 Camadas

| Camada | Responsabilidade |
| --- | --- |
| Frontend estatico | Paginas HTML/CSS/JS para submissao, tracking, paineis internos e MFA. |
| Controllers REST | Entrada HTTP, DTOs, validacao, resposta JSON/ficheiros. |
| Services | Regras de negocio, ownership, audit logging, backup, packages, uploads. |
| Repositories | Persistencia JPA. |
| Security | Spring Security, JWT filter, RBAC, MFA, CSRF, headers e rate limiting. |
| Storage | PostgreSQL para dados; filesystem para anexos, evidencias e backups. |

### 5.2 Stack

| Area | Tecnologia |
| --- | --- |
| Linguagem/runtime | Java 17 |
| Backend | Spring Boot `3.5.15` |
| Seguranca | Spring Security `6.5.11`, JWT, BCrypt, MFA, CSRF token cookie |
| Persistencia | Spring Data JPA, Hibernate, PostgreSQL |
| Testes | JUnit 5, MockMvc, AssertJ, JaCoCo, PIT |
| DevSecOps | GitHub Actions, CodeQL, SonarCloud, SpotBugs, OWASP Dependency-Check, CycloneDX, Gitleaks, OWASP ZAP |
| Frontend | HTML/CSS/JavaScript estatico |

## 6. Modelo de dominio

### 6.1 Report

`Report` representa a denuncia anonima. Contem titulo, descricao, categoria,
estado, tracking code e associacao a anexos. O tracking code e o unico mecanismo
publico para o denunciante acompanhar ou recuperar evidencia, evitando contas de
reporter e reduzindo exposicao de identidade.

Controlos associados:

- tracking code validado por formato;
- erros genericos para evitar enumeracao;
- rate limiting em submissao publica, verificacao e download;
- DTOs para impedir mass assignment;
- descricao e categoria validadas.

### 6.2 CaseReview

`CaseReview` representa o trabalho interno de analise. Liga uma denuncia a um
analista, prioridade, notas internas e estado do caso.

Controlos associados:

- analista so consulta/actualiza casos elegiveis ou atribuidos;
- auditor nao pode modificar casos;
- admin tem acesso de oversight;
- casos fechados nao devem ser alterados parcialmente;
- alteracoes relevantes geram auditoria.

### 6.3 User

`User` representa utilizadores internos. As roles activas sao `ADMIN`,
`ANALYST` e `AUDITOR`. Nao existe role geral `USER` no modelo protegido actual.

Controlos associados:

- password com BCrypt;
- politica de password;
- utilizadores inactivos bloqueados;
- MFA obrigatorio por omissao para as tres roles internas;
- lifecycle admin com proteccao contra remover o ultimo admin activo.

## 7. Actores e roles

| Actor | Tipo | Capacidades |
| --- | --- | --- |
| Denunciante anonimo | Publico | Submeter denuncia, verificar tracking code, enviar anexos, descarregar evidencia autorizada por tracking code. |
| `ANALYST` | Interno | Consultar casos elegiveis, assumir casos, actualizar estado/prioridade/notas e gerar pacotes de evidencia quando permitido. |
| `AUDITOR` | Interno | Consultar logs, alertas, casos fechados, verificar pacotes de evidencia e backups. |
| `ADMIN` | Interno | Gerir utilizadores, consultar auditoria/alertas, gerir backups, aceder a funcoes de oversight. |

Todas as roles internas passam por password + MFA antes da emissao de JWT.

## 8. Autenticacao, MFA e sessao

O fluxo de autenticacao tem duas fases para roles internas:

1. `POST /auth/login` valida username/password, aplica rate limiting por
   utilizador/IP e, se a role exigir MFA, devolve `mfaRequired=true` e um
   `mfaChallengeId` sem emitir JWT.
2. `POST /auth/mfa/verify` valida o codigo de utilizacao unica e curta duracao,
   com invalidacao do challenge apos tentativas invalidas repetidas.
   So depois da verificacao e emitido o JWT.

Controlos implementados:

- BCrypt para passwords;
- JWT stateless com claims de role;
- validacao de issuer/audience/kid nos testes de JWT;
- revogacao de token em logout;
- MFA para `ADMIN`, `ANALYST`, `AUDITOR`;
- desafios MFA expiram e nao podem ser reutilizados;
- em ambiente dev/test, o codigo pode ser exposto em log para demonstracao;
- em producao, o canal real de entrega deve ser externo.

## 9. Autorizacao e endpoints

A autorizacao esta centralizada em `SecurityConfig` e documentada em detalhe em
[AUTHORIZATION_MATRIX.md](AUTHORIZATION_MATRIX.md). Em resumo:

- endpoints publicos: paginas estaticas, login/MFA/password reset, submissao
  anonima, tracking, uploads publicos associados a tracking code e download por
  tracking code;
- `/admin/**`: apenas `ADMIN`;
- `/analyst/**`: `ANALYST` e `ADMIN`, com ownership nos servicos;
- `/audit/**`: `AUDITOR` e `ADMIN`;
- `/auth/logout` e `/auth/password/change`: qualquer role interna autenticada.

Os endpoints principais estao agrupados abaixo.

### 9.1 Auth

| Metodo | Endpoint | Acesso | Finalidade |
| --- | --- | --- | --- |
| POST | `/auth/login` | Publico | Validar password e iniciar MFA ou emitir JWT se MFA nao for exigido. |
| POST | `/auth/mfa/verify` | Publico com desafio | Validar MFA e emitir JWT. |
| POST | `/auth/logout` | Autenticado | Revogar JWT. |
| POST | `/auth/password/change` | Autenticado | Alterar password com password actual. |
| POST | `/auth/password-reset/request` | Publico | Pedir reset com resposta generica. |
| POST | `/auth/password-reset/confirm` | Publico | Confirmar reset por token. |

### 9.2 Denuncia publica

| Metodo | Endpoint | Acesso | Finalidade |
| --- | --- | --- | --- |
| POST | `/reports` | Publico | Criar denuncia anonima. |
| POST | `/reports/verify` | Publico | Validar tracking code e consultar estado. |
| POST | `/reports/{id}/attachments` | Publico com tracking code | Enviar anexos. |
| POST | `/reports/{id}/attachments/list` | Publico com tracking code | Listar metadados de anexos. |
| POST | `/reports/download` | Publico com tracking code | Descarregar anexo autorizado. |

### 9.3 Analista

| Metodo | Endpoint | Acesso | Finalidade |
| --- | --- | --- | --- |
| GET | `/analyst/panel` | `ANALYST`, `ADMIN` | Validar acesso ao painel. |
| GET | `/analyst/reports` | `ANALYST`, `ADMIN` | Listar denuncias elegiveis. |
| POST | `/analyst/reports/{id}/assign` | `ANALYST`, `ADMIN` | Assumir/atribuir caso. |
| PATCH | `/analyst/reports/{id}/status` | `ANALYST`, `ADMIN` | Actualizar estado. |
| PATCH | `/analyst/reports/{id}/priority` | `ANALYST`, `ADMIN` | Actualizar prioridade. |
| PATCH | `/analyst/reports/{id}/notes` | `ANALYST`, `ADMIN` | Actualizar notas internas. |
| GET | `/analyst/reports/{id}/case-review` | `ANALYST`, `ADMIN` | Consultar review do caso. |
| GET | `/analyst/my-cases` | `ANALYST`, `ADMIN` | Consultar casos atribuidos ao analista. |
| GET | `/analyst/reports/{id}/attachments` | `ANALYST`, `ADMIN` | Listar anexos internos. |
| GET | `/analyst/attachments/{attachmentId}/download` | `ANALYST`, `ADMIN` | Descarregar anexo interno. |
| POST | `/analyst/reports/{id}/case-package` | `ANALYST`, `ADMIN` | Gerar pacote de evidencia. |

### 9.4 Auditor

| Metodo | Endpoint | Acesso | Finalidade |
| --- | --- | --- | --- |
| GET | `/audit/logs` | `AUDITOR`, `ADMIN` | Consultar logs de auditoria. |
| GET | `/audit/security-alerts` | `AUDITOR`, `ADMIN` | Consultar alertas. |
| GET | `/audit/cases/closed` | `AUDITOR`, `ADMIN` | Consultar historico de casos fechados. |
| GET | `/audit/cases/{reportId}/evidence-package/verify` | `AUDITOR`, `ADMIN` | Verificar pacote de evidencia. |
| GET | `/audit/backups` | `AUDITOR`, `ADMIN` | Listar backups. |
| GET | `/audit/backups/{filename}/verify` | `AUDITOR`, `ADMIN` | Verificar backup. |
| GET | `/audit/backups/{filename}/manifest` | `AUDITOR`, `ADMIN` | Consultar manifesto. |

### 9.5 Admin

| Metodo | Endpoint | Acesso | Finalidade |
| --- | --- | --- | --- |
| GET | `/admin/panel` | `ADMIN` | Validar painel admin. |
| GET | `/admin/users` | `ADMIN` | Listar utilizadores. |
| POST | `/admin/users` | `ADMIN` | Criar utilizador. |
| PUT | `/admin/users/{id}` | `ADMIN` | Editar utilizador. |
| PATCH | `/admin/users/{id}/activate` | `ADMIN` | Activar utilizador. |
| PATCH | `/admin/users/{id}/deactivate` | `ADMIN` | Desactivar utilizador. |
| DELETE | `/admin/users/{id}` | `ADMIN` | Remocao logica por desactivacao. |
| GET | `/admin/audit-logs` | `ADMIN` | Consultar logs. |
| GET | `/admin/security-alerts` | `ADMIN` | Consultar alertas. |
| POST | `/admin/backups` | `ADMIN` | Criar backup. |
| GET | `/admin/backups` | `ADMIN` | Listar backups. |
| GET | `/admin/backups/{filename}/download` | `ADMIN` | Descarregar backup. |
| POST | `/admin/backups/{filename}/verify` | `ADMIN` | Verificar backup. |
| POST | `/admin/backups/{filename}/restore` | `ADMIN` | Repor backup validado. |

## 10. Validacao, uploads e seguranca de input

A aplicacao usa DTOs com Bean Validation e evita binding directo de entidades.
As validacoes principais incluem:

- campos obrigatorios e limites em `CreateReportRequest`;
- formato de tracking code;
- status e priority por enum/allowlist;
- roles apenas `ADMIN`, `ANALYST`, `AUDITOR`;
- password policy no servico, incluindo comprimento, complexidade, passwords
  comprometidas, reutilizacao e palavras contextuais;
- `attachmentId` positivo e tracking code valido em downloads;
- content type esperado em endpoints JSON;
- rejeicao de `TRACE`, headers com caracteres de controlo e `Authorization`
  excessivamente grande antes de chegar aos controllers;
- rejeicao de parametros escalares duplicados para mitigar HTTP parameter
  pollution fora de multipart;
- validacao Fetch Metadata/Origin para bloquear pedidos unsafe cross-site;
- CSP `report-uri /security/csp-report` para receber relatorios de violacao
  do browser sem expor tokens ou tracking codes em respostas.

Uploads sao uma superficie critica. Mitigacoes:

- limite de tamanho;
- limite de ficheiros por pedido;
- quota acumulada de ficheiros por denuncia;
- extensoes permitidas;
- validacao MIME e magic bytes;
- rejeicao de executaveis renomeados;
- quarentena/log em caso de malware scanner finding;
- nomes de ficheiro gerados no servidor;
- rejeicao de nomes com path traversal;
- respostas controladas sem paths internos.

## 11. Auditoria, alertas e evidencias

O projecto inclui logs de auditoria e alertas de seguranca para suportar
accountability. Eventos cobertos:

- login com sucesso/falha;
- utilizador inactivo bloqueado;
- MFA challenge, sucesso, expiracao e rejeicao;
- logout e revogacao de token;
- alteracoes de utilizadores;
- acessos proibidos;
- ownership violations;
- uploads rejeitados;
- tracking code enumeration;
- CSP violations reportadas pelo browser;
- operacoes de backup;
- geracao/verificacao de pacotes de evidencia.

Os registos incluem `correlationId` e `integrityHash`, reduzindo risco de
alteracao silenciosa. A retencao imutavel e exportacao SIEM ficam como trabalho
operacional futuro.

## 12. Backups e pacotes de evidencia

Backups e pacotes de evidencia sao os principais pontos onde o backend usa o
sistema operativo:

- cria directorios e ficheiros;
- gera ZIPs;
- calcula hashes;
- valida manifestos;
- bloqueia path traversal em nomes de backup;
- rejeita ZIPs com entradas nao assinadas/tampered;
- permite restaurar apenas depois de validacao.

Auditores podem verificar evidencia; admins podem criar, descarregar, verificar
e repor backups. Analistas podem gerar pacotes de evidencia para casos fechados
quando autorizados.

## 13. STRIDE aplicado ao GhostReport

| STRIDE | Ameaça no GhostReport | Mitigacoes implementadas | Evidencia |
| --- | --- | --- | --- |
| Spoofing | Atacante tenta autenticar-se como admin/analyst/auditor. | BCrypt, MFA para roles internas, JWT assinado, issuer/audience/kid, inactive users bloqueados, rate limiting. | `AdminMfaAuthenticationTest`, `JwtServiceSecurityTest`, `LoginRateLimitSecurityTest`. |
| Tampering | Alteracao de logs, backups, packages ou ficheiros. | Hashes, manifestos HMAC, verificacao de backups, integridade em audit/security records, rejeicao de ZIP tampering. | `BackupServiceIntegrationTest`, audit/security DTOs. |
| Repudiation | Utilizador nega operacao critica. | Audit logs com accao, actor, target, correlationId e integrityHash. | `AuditLogSecurityTest`, `RuntimeSecurityEventLoggingTest`. |
| Information Disclosure | Exposicao de tracking code, token, password, paths ou dados anonimos. | Erros genericos, redaction de logs, DTOs, ausencia de tokens em browser storage, tracking code nao vai em URL. | `AnonymousDataLoggingTest`, `FrontendXssDataExposureTest`, `ErrorHandlingSecurityTest`. |
| Denial of Service | Abuso de login, tracking, upload/download, headers excessivos ou ficheiros grandes. | Rate limiting por fluxo, limites de upload, max files per request, limites Tomcat/Hikari, rejeicao antecipada. | `RateLimiterServiceTest`, upload tests, `SecurityHeadersTest`, `SecurityConfigurationValidatorTest`. |
| Elevation of Privilege | Analyst tenta agir como admin/auditor ou aceder a caso de outro analyst. | RBAC central, service-level ownership, tests para forbidden routes, proteccao do ultimo admin activo. | `RbacAuthorizationMatrixTest`, `AnalystCaseOwnershipTest`, `AdminUserManagementSecurityTest`. |

## 14. Code review e controlo antes de merge

O processo de revisao usado no projecto e baseado em branches curtas, pull
requests e validacao automatica. O objectivo nao e apenas rever estilo: cada PR
deve confirmar que a alteracao continua alinhada com o modelo de seguranca do
GhostReport.

Fluxo esperado:

1. O developer cria uma branch com ambito claro.
2. Implementa a alteracao e corre localmente os testes relevantes; para backend,
   o comando base e `.\mvnw.cmd test`.
3. Abre pull request com resumo, motivacao, impacto de seguranca e evidencia.
4. A equipa revê codigo, endpoints, DTOs, validacao, autorizacao, logs e erros.
5. A pipeline executa build, testes, JaCoCo, SAST, SCA, Gitleaks, SBOM e, quando
   aplicavel, DAST/runtime evidence.
6. Findings criticos ou confirmados sao corrigidos antes de merge.
7. Findings nao exploraveis no ambito academico podem ser aceites apenas com
   justificacao documentada em triagem ou limitacoes.

Critérios usados na revisao:

| Area | Validacao |
| --- | --- |
| Branch/PR | Scope pequeno, descricao clara e impacto de seguranca indicado. |
| Autorizacao | Novas rotas alinhadas com `SecurityConfig` e, quando necessario, ownership em service. |
| Inputs/DTOs | Sem binding directo de entidades; Bean Validation, allowlists e limites revistos. |
| Erros/logs | Sem stack traces, paths internos, passwords, JWTs, secrets ou tracking codes em logs/respostas. |
| Dependencias | Dependency-Check, SBOM e suppressions revistos quando `pom.xml` muda. |
| Testes | Testes unitarios/integracao actualizados; JaCoCo e PIT usados como evidencia de qualidade. |
| Scanners | CodeQL, SpotBugs, SonarCloud, Gitleaks, ZAP e Dependency-Check avaliados de acordo com severidade e explorabilidade. |
| Documentacao | Claims de seguranca actualizados em `PHASE2_SPRINT2_REPORT.md`, anexos e ASVS quando mudam. |

Os checks mais bloqueantes no codigo actual sao build/testes/JaCoCo, falhas de
Gitleaks confirmadas e falhas tecnicas dos jobs necessarios para gerar
evidencia. SAST, SCA, DAST baseline e IAST-like tambem entram na decisao, mas
alguns resultados sao tratados como triagem: um falso positivo ou risco fora do
ambito pode ser aceite se ficar justificado em [SCA_TRIAGE.md](SCA_TRIAGE.md),
[SPOTBUGS_TRIAGE.md](SPOTBUGS_TRIAGE.md), [DEVSECOPS_PIPELINE.md](DEVSECOPS_PIPELINE.md)
ou nas limitacoes finais.

Esta disciplina evita merge de codigo inseguro porque combina revisao humana,
testes negativos, gates automaticos, evidencias arquivadas e documentacao de
risco residual. Nao substitui branch protection configurada no GitHub nem
aprovacoes obrigatorias formais; essas configuracoes sao operacionais e devem
ser confirmadas no repositorio remoto.

## 15. Pipeline DevSecOps e automacoes

O workflow principal `dev` corre em `push`, `pull_request` e `workflow_dispatch`.
Tem `concurrency` para cancelar execucoes antigas da mesma ref. As automacoes
principais sao:

| Job | Automacao | Resultado esperado |
| --- | --- | --- |
| `build-test` | `./mvnw verify` | Build, testes e JaCoCo; publica Surefire e coverage. |
| `security-secrets` | Gitleaks em Docker | Detecta secrets commitados; publica JSON. |
| `sast` | CodeQL init/analyze, SpotBugs, SonarCloud | Evidencia SAST e upload de artefactos. |
| `dependency-scanning` | Dependency-Check + CycloneDX | SARIF para code scanning, HTML/XML/JSON e SBOM. |
| `dast-scan` | Runtime tests, arranque da app, probes HTTP, ZAP baseline | Evidencia runtime/IAST-like e DAST baseline. |

O workflow `pit-mutation-testing` corre em `workflow_dispatch`, PRs e alteracoes
relevantes em `main`. Gera `target/pit-reports/index.html` e artefacto
`pit-mutation-testing-report`.

A pipeline esta descrita em detalhe em [DEVSECOPS_PIPELINE.md](DEVSECOPS_PIPELINE.md).

## 16. SCA e dependencia Spring Security

O GitHub Security/Code scanning reportou alertas do OWASP Dependency-Check para
Spring Security `6.5.10`:

- CVE-2026-40988;
- CVE-2026-41694;
- CVE-2026-41003.

A correcao foi feita actualizando o parent/BOM Spring Boot para `3.5.15`, o que
resolve Spring Security para `6.5.11` sem declarar versoes manuais de modulos
Spring Security. A decisao evita misturar versoes de `spring-security-core`,
`web`, `config` e restantes modulos.

Documentacao de triagem: [SCA_TRIAGE.md](SCA_TRIAGE.md).

O mesmo ficheiro documenta tambem os findings suprimidos por nao aplicabilidade
ao componente usado: CVE-2025-15104 para `hibernate-validator` e CVE-2025-7962
para `angus-activation`.

A execucao local do Dependency-Check em 2026-06-15 gerou relatorios
HTML/XML/JSON/SARIF e confirmou 0 vulnerabilidades nao suprimidas e 2
suppressions documentadas.

## 17. SAST, DAST e IAST-like

### SAST

SAST combina:

- CodeQL para analise semantica Java e upload para GitHub Code Scanning;
- SpotBugs para padroes Java suspeitos;
- SonarCloud quando `SONAR_TOKEN` esta configurado.

### DAST

DAST e feito por OWASP ZAP baseline contra `http://localhost:8081` apos a
aplicacao arrancar em CI. O scan e baseline/passivo, nao substitui um teste de
penetracao autenticado.

### IAST-like

Nao existe agente IAST completo. A evidencia IAST-like combina:

- testes runtime de seguranca;
- probes HTTP contra app real;
- logs da aplicacao;
- verificacao de fuga de dados sensiveis;
- correlacao com ZAP baseline.

Esta distincao e importante para nao exagerar claims. O detalhe esta em
[IAST_RUNTIME_SECURITY.md](IAST_RUNTIME_SECURITY.md), com artefactos
complementares em [iast-runtime-evidence.md](iast-runtime-evidence.md),
[runtime-endpoints.md](runtime-endpoints.md) e
[runtime-log-sanitization.md](runtime-log-sanitization.md).

Na pipeline, os probes live exercitam:

- criacao publica de denuncia valida;
- denuncia invalida e campos obrigatorios;
- caracteres perigosos tratados como dados;
- tentativa de mass assignment;
- tracking code valido, invalido e repetido;
- upload permitido;
- upload com extensao proibida;
- upload com content-type/assinatura suspeita;
- filename com tentativa de path traversal;
- paginas publicas (`/`, `/index.html`, `/submit.html`, `/track.html`,
  `/admin.html`, `/analyst.html`, `/auditor.html`);
- endpoint admin sem token;
- endpoint admin com JWT invalido;
- login real de admin, analyst e auditor com MFA;
- acesso autorizado e negado com tokens reais por role;
- user lifecycle admin, backups list/download/verify e filename invalido;
- analyst assign, priority/status/notes, attachments e case-package;
- auditor logs, alerts, closed cases, backups e package verification;
- casos negativos para metodo errado, JSON malformado, content type errado,
  Authorization malformado, JWT invalido e token ausente.

A validacao local expandida do probe confirmou 101 probes: 101 passed, 0 failed
e 0 skipped. `GET /login.html` e tratado como controlo de exposicao: `401/404`
confirma que nao existe pagina publica separada. O restore destrutivo de backup
continua fora do probe runtime; a evidencia executa validacao segura de
filename/path traversal e os testes automatizados cobrem restore para staging.

JWT expirado, backups, ZIP Slip e tamanho maximo de upload sao cobertos pela
seleccao de testes Maven executada no mesmo job `dast-scan`.

## 18. Testes automatizados

A suite local mais recente correu:

```powershell
cd ghostreport
.\mvnw.cmd test
```

Resultado confirmado em 2026-06-15: 272 testes, 0 falhas, 0 erros, 0 skipped.

Categorias cobertas:

- contexto Spring Boot;
- validacao de configuracao;
- autenticacao, JWT, MFA e password reset;
- RBAC e endpoint matrix;
- CSRF e security headers;
- CSP/HSTS/COOP/COEP/CORP, CSP reporting, Fetch Metadata e request-boundary checks;
- uploads, MIME, magic bytes, malware/quarantine e traversal;
- quotas de anexos por pedido e por denuncia;
- tracking code e enumeracao;
- tracking codes gerados por `SecureRandom` sob carga academica moderada;
- rate limit de submissao publica anonima;
- analista ownership e workflow de casos;
- auditor read-only;
- admin user lifecycle;
- backups, restore e integridade;
- frontend: XSS sinks, scripts inline, tokens em storage, tracking code em URL, navs escondidas.

Resumo detalhado: [SECURITY_TESTING.md](SECURITY_TESTING.md).

## 19. Instalacao e configuracao segura

Secrets devem vir do ambiente:

- `JWT_SECRET`;
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`;
- `BACKUP_HMAC_SECRET`, `BACKUP_HMAC_KEY_ID`;
- `GHOSTREPORT_MFA_REQUIRED_ROLES`;
- `GHOSTREPORT_TRANSPORT_TLS_MODE`;
- `GHOSTREPORT_TRUSTED_PROXY_ENABLED`;
- `SERVER_FORWARD_HEADERS_STRATEGY`;
- `SERVER_SSL_ENABLED`, `SERVER_SSL_KEY_STORE`, `SERVER_SSL_KEY_STORE_PASSWORD`;
- `SERVER_SSL_ENABLED_PROTOCOLS`, `SERVER_SSL_CIPHERS`;
- `DB_POOL_MAX_SIZE`, `DB_CONNECTION_TIMEOUT_MS`, `SERVER_MAX_CONNECTIONS`,
  `SERVER_TOMCAT_THREADS_MAX`;
- directorios de upload, evidence e backups.

O perfil `dev` pode expor codigo MFA em logs para demonstracao. Isto deve estar
desligado em ambientes partilhados/prod-like. O perfil prod-like usa PostgreSQL,
validacao de schema, modo TLS explicito e rejeita `DB_URL` PostgreSQL sem
`sslmode=verify-ca` ou `sslmode=verify-full`; ainda falta Flyway/Liquibase.

Guias:

- [SECURE_INSTALLATION.md](SECURE_INSTALLATION.md)
- [SECURITY_CONFIGURATION_ASSESSMENT.md](SECURITY_CONFIGURATION_ASSESSMENT.md)

## 20. ASVS e rastreabilidade

O tracker ASVS principal esta em
[ASVS_5.0_Tracker_Phase_2_Sprint_2.xlsx](ASVS_5.0_Tracker_Phase_2_Sprint_2.xlsx).
O ficheiro [ASVS_EVIDENCE.md](ASVS_EVIDENCE.md) e apenas o resumo explicativo.
O projecto cobre de forma implementada ou documentada areas como autenticacao,
autorizacao, validacao, file handling, logging, configuracao, dependency control
e runtime testing.

Na revisao L2 adicional foram melhorados ou reclassificados com evidencia
verificavel controlos de business limits, anti-automation, malware scanning,
password policy, MFA, JWT audience, tokens self-contained, crypto integrity,
resource limits, data protection/no-store, producao minima e logging.

Na revisao L3 adicional foram reforcados ou corrigidos controlos com evidencia
real: CSP violation reporting, HSTS preload, validacao de utilizador activo em
JWT, comparacao constante de assinatura JWT, superficie estatica limitada,
no-store em endpoints sensiveis, quota acumulada de uploads, defesa contra HTTP
parameter pollution, teste de `SecureRandom` sob carga, algoritmos
criptograficos aprovados e reclassificacao de CSV/spreadsheet injection como
nao aplicavel por ausencia de exports CSV/XLSX/ODS.

Os capitulos fora do desenho implementado, como OAuth/OIDC e WebRTC, continuam
marcados como `Not Applicable` no tracker em vez de `Compliant`.

Pontos parciais ou dependentes de operacao:

- canal MFA real de producao;
- secret manager externo;
- SIEM/retencao imutavel;
- migrations formais;
- certificado publico/TLS operacional e canal MFA real;
- IAST agent-based;
- DAST autenticado completo.

## 21. Avaliacao final

GhostReport cumpre o objectivo de prototipo academico seguro com evidencia
tecnica. O sistema nao e apresentado como produto production-ready; e uma base
coerente, testada e documentada que demonstra aplicacao pratica de threat
modelling, secure coding e DevSecOps.

Pontos fortes:

- separacao clara entre fluxo anonimo e fluxo interno;
- MFA para todas as roles internas;
- RBAC com testes negativos;
- upload hardening concreto;
- auditoria e alertas;
- backups verificaveis;
- pipeline com varios tipos de scanning;
- documentacao organizada e rastreavel.

Claims finais correctamente delimitados:

| Claim | Redaccao correcta para a entrega |
| --- | --- |
| MFA | Implementado para `ADMIN`, `ANALYST` e `AUDITOR`; em dev/test o codigo pode ser exposto em logs para demonstracao. |
| DAST | OWASP ZAP baseline/passivo e probes runtime; nao e pentest autenticado completo. |
| IAST | Evidencia runtime/IAST-like; nao existe agente IAST com taint tracking. |
| Producao | Existe guia prod-like com TLS/proxy/PostgreSQL TLS/resource limits validados no arranque; faltam controlos operacionais externos como certificado publico, secret manager, SIEM/WORM e canal MFA real. |
| ASVS | Tracker Sprint 2 em XLSX actualizado; Markdown funciona como resumo explicativo. |

Limitacoes:

- MFA precisa de canal/IdP real em producao;
- rate limiting e em memoria;
- ZAP e baseline nao autenticado;
- IAST e apenas evidencia runtime/academic substitute;
- falta Flyway/Liquibase;
- falta SIEM/WORM/secret manager externo.

## 22. Conclusao

A Phase 2 Sprint 2 fecha o GhostReport como uma entrega final robusta para
avaliacao: a aplicacao implementa os fluxos principais, os riscos da Phase 1
foram tratados com mitigacoes concretas, os endpoints estao documentados, a
pipeline gera evidencia e os testes validam o comportamento esperado. O relatorio
e os anexos permitem ao professor seguir a historia completa do projecto sem
depender de documentos soltos ou claims sem suporte.
