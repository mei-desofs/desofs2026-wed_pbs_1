# Seguranca em runtime e evidencia IAST-like

## Objectivo

Este ficheiro e o documento principal de IAST/runtime da Sprint 2. Os ficheiros
`iast-runtime-evidence.md`, `runtime-endpoints.md` e
`runtime-log-sanitization.md` ficam como anexos de evidencia especifica, porque
espelham artefactos gerados pelo workflow `dast-scan`.

GhostReport nao integra uma ferramenta IAST comercial, JVM agent, taint tracking
ou telemetria source-to-sink. A formulacao correcta e:

```text
runtime security testing / evidencia academica IAST-like
```

## O que foi feito

O job `dast-scan` em `.github/workflows/dev.yml` executa uma validacao runtime
com varias camadas:

1. Corre testes Maven focados em seguranca runtime.
2. Empacota a aplicacao Spring Boot.
3. Arranca GhostReport em `http://localhost:8081` com perfil `dev` e PostgreSQL
   de CI.
4. Executa probes HTTP reais com `.github/scripts/runtime_security_probe.py`.
5. Completa login e MFA dev para `ADMIN`, `ANALYST` e `AUDITOR`.
6. Testa endpoints publicos, endpoints protegidos e fronteiras RBAC.
7. Analisa logs da aplicacao para padroes sensiveis.
8. Corre OWASP ZAP baseline/passivo contra a app em execucao.
9. Publica artefactos `iast-runtime-security-evidence` e
   `dast-zap-baseline-reports`.

## Testes runtime executados

O workflow selecciona testes que validam controlos observaveis em runtime:

- `RuntimeSecurityEventLoggingTest`
- `ErrorHandlingSecurityTest`
- `SecurityHeadersTest`
- `JwtServiceSecurityTest`
- `LoginRateLimitSecurityTest`
- `CsrfSecurityTest`
- `PublicReportFlowIntegrationTest`
- `ReportControllerAttachmentUploadTest`
- `RbacAuthorizationMatrixTest`
- `AdminMfaAuthenticationTest`
- `AuditorAuthorizationTest`
- `AdminBackupControllerSecurityTest`
- `BackupServiceIntegrationTest`
- `ApiValidationContractTest`

Estes testes cobrem autenticacao, MFA, JWT invalido/expirado, rate limiting,
headers, CSRF, validacao de reports, uploads, RBAC, auditoria, backups,
integridade e tratamento de erros.

## Endpoints analisados por probes live

A evidencia live inclui:

| Area | Exemplos |
| --- | --- |
| Frontend publico | `GET /index.html` e verificacao de disponibilidade. |
| Auth | `POST /auth/login`, `POST /auth/mfa/verify`, falhas de login e MFA invalido. |
| Denuncia anonima | `POST /reports` valido, invalido, com caracteres perigosos e tentativa de mass assignment. |
| Tracking | `POST /reports/verify` valido, invalido e repetido. |
| Uploads | upload permitido, extensao proibida, assinatura/conteudo suspeito e filename com traversal. |
| Admin | `/admin/users` sem token, com JWT invalido, com admin JWT e com roles erradas. |
| Analyst | `/analyst/panel` com analyst/admin e negado a auditor. |
| Auditor | `/audit/logs` com auditor/admin e negado a analyst. |
| Erros | endpoint inexistente para confirmar resposta sem stack trace. |

Detalhe complementar: [runtime-endpoints.md](runtime-endpoints.md).

## Sanitizacao de logs

O workflow escreve logs em `target/ghostreport-dast-app.log` e procura padroes
como:

- passwords;
- headers `Authorization`;
- bearer tokens;
- nomes de secrets (`JWT_SECRET`, `BACKUP_HMAC_SECRET`);
- stack traces e detalhes Java.

Quando ha match, a amostra e redigida em
`target/iast-evidence/runtime-log-sensitive-findings.txt` para revisao manual.
Os testes tambem validam que audit logs e respostas evitam passwords, tokens,
tracking codes e detalhes internos.

Detalhe complementar: [runtime-log-sanitization.md](runtime-log-sanitization.md).

## Artefactos de evidencia

| Artefacto | Origem | Uso |
| --- | --- | --- |
| `target/iast-evidence/iast-runtime-evidence.md` | Workflow `dast-scan` | Sumario runtime gerado pela CI. |
| `target/iast-evidence/runtime-endpoints.md` | Probe Python | Tabela de endpoints exercitados. |
| `target/iast-evidence/runtime-log-sanitization.md` | Check de logs | Resultado da procura por padroes sensiveis. |
| `target/iast-evidence/runtime-probe-summary.json` | Probe Python | Sumario JSON redigido. |
| `target/ghostreport-dast-app.log` | Aplicacao em CI | Log bruto para revisao. |
| `target/zap-reports/zap-baseline.*` | OWASP ZAP | HTML/XML/JSON de DAST baseline. |
| `target/surefire-reports` | Maven | Relatorios dos testes runtime-focused. |

O anexo [iast-runtime-evidence.md](iast-runtime-evidence.md) documenta a
estrutura esperada destes artefactos.

## Relacao com DAST

O ZAP baseline e executado contra `http://localhost:8081` em modo passivo. Isto
ajuda a detectar configuracoes e respostas inseguras, mas nao substitui:

- DAST autenticado com contexto por role;
- teste de penetracao manual;
- IAST com agente;
- revisao de codigo e testes negativos.

Por isso, os resultados ZAP sao tratados como evidencia de triagem. Findings
criticos confirmados devem ser corrigidos; avisos informativos podem ser
documentados como hardening futuro.

## Limitacoes

- Nao existe agente IAST externo.
- Nao existe taint tracking nem correlacao automatica source-to-sink.
- ZAP baseline e nao autenticado.
- A probe de MFA usa codigo exposto em logs apenas no perfil `dev`; em producao,
  `ghostreport.mfa.expose-code` deve ficar `false`.
- Pattern scanning de logs nao equivale a DLP formal.
- A evidencia deve ser interpretada em conjunto com SAST, SCA, SBOM, testes,
  code review e ASVS.

## Necessario para producao

Antes de tratar esta evidencia como controlo production-grade, seria necessario:

- integrar uma ferramenta IAST/agent compativel com Java/Spring Boot;
- configurar DAST autenticado para `ADMIN`, `ANALYST` e `AUDITOR`;
- enviar logs para SIEM com retencao e alerting;
- garantir canal MFA real, sem exposicao de codigos em logs;
- definir gates formais para findings runtime por severidade;
- arquivar artefactos de CI com politica de retencao adequada.
