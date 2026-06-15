# Evidencia ASVS

O tracker ASVS principal do Sprint 2 e o ficheiro Excel:

- [ASVS_5.0_Tracker_Phase_2_Sprint_2.xlsx](ASVS_5.0_Tracker_Phase_2_Sprint_2.xlsx)

O ficheiro foi criado como copia estrutural do tracker ASVS da Phase 2 Sprint
1, mantendo as mesmas folhas, formulas, capitulos e formato geral. O conteudo
foi actualizado para reflectir a evidencia factual do Sprint 2. Este Markdown
e apenas o resumo explicativo da evidencia; nao substitui o XLSX.

## Base factual usada

| Evidencia | Resultado confirmado |
| --- | --- |
| Testes Maven | `286` testes, `0` falhas, `0` erros, `0` skipped. |
| Runtime probes locais | `101` probes, `101` passed, `0` failed, `0` skipped. |
| Spring Security | `6.5.11` via Spring Boot `3.5.15`. |
| SCA CVEs remediados | CVE-2026-40988, CVE-2026-41694, CVE-2026-41003. |
| Suppressions SCA | CVE-2025-15104 e CVE-2025-7962 documentados como nao aplicaveis/falso positivo para os componentes usados. |

## Mapa de evidencia

| Area ASVS | Evidencia GhostReport | Estado |
| --- | --- | --- |
| Arquitectura e threat modelling | Relatorio Phase 1, DDD, trust boundaries e relatorio Sprint 2. | Implementado/documentado |
| Autenticacao | BCrypt, login interno, JWT, bloqueio de utilizadores inactivos e logout/revogacao. | Implementado |
| MFA | Desafio MFA antes de JWT para `ADMIN`, `ANALYST` e `AUDITOR`, uso unico, TTL e bloqueio do challenge apos tentativas invalidas. | Implementado; canal de producao e futuro |
| Autorizacao | Regras de rota em `SecurityConfig`, RBAC e ownership nos servicos. | Implementado |
| Denunciante anonimo/tracking | Sem conta de reporter; tracking code controla verificacao/listagem/download. | Implementado |
| Validacao | DTOs, Bean Validation, enums/allowlists, tracking code e contratos API. | Implementado |
| Ficheiros/uploads | Extensao/MIME/magic bytes, tamanho, quota por request/denuncia, nomes gerados, path checks e ZIP Slip em backups/packages. | Implementado |
| Erros e logging | Erros genericos, correlation id, audit logs, security alerts e sanitizacao. | Implementado |
| Backups/evidencia | ZIPs com hashes, HMAC, manifesto, verificacao e restore para staging com reautenticacao do admin. | Implementado |
| SCA/SAST/DAST/runtime | Dependency-Check, CycloneDX, CodeQL, SpotBugs, SonarCloud, Gitleaks, ZAP baseline e runtime evidence. | Implementado como evidencia |
| Configuracao | Secrets por ambiente, PostgreSQL fora de testes, validacao de configuracao e guia seguro. | Implementado/documentado |
| Headers/browser security | CSP restritiva com `report-uri`, endpoint `/security/csp-report`, HSTS preload, COOP/COEP/CORP, Fetch Metadata/Origin validation, fallback para browsers sem features esperadas e bloqueio de headers anormais. | Implementado |
| Criptografia | [CRYPTOGRAPHIC_INVENTORY.md](CRYPTOGRAPHIC_INVENTORY.md) mapeia BCrypt, SecureRandom, HMAC-SHA-256, SHA-256, JWT, backups e hashes de integridade; `CryptographicInventoryTest` verifica o inventario. | Implementado |
| Dangerous functionality | [DANGEROUS_FUNCTIONALITY.md](DANGEROUS_FUNCTIONALITY.md) mapeia restore, backups, uploads, packages, password reset, JWT/logging/crypto e e verificado por teste. | Implementado |
| Comunicacao segura | Perfil prod-like com modo TLS explicito, TLS 1.2/1.3, cifras modernas e PostgreSQL com `sslmode=verify-ca`/`verify-full`. | Implementado/configurado; certificado publico e operacional |
| L2 aplicavel | Rate limit de submissao de denuncias, malware scanner local, JWT audience, MFA one-time, no-store, resource limits e logging estruturado. | Reavaliado no XLSX |

## Melhorias de codigo apos revisao L1/L2

- `MfaChallengeService` passou a invalidar desafios MFA apos o limite
  configurado de codigos invalidos, com evento `MFA_VERIFY_LOCKED`.
- `PasswordPolicyService` passou a alinhar com ASVS V6.2.5: aceita qualquer
  composicao de caracteres e mantem comprimento, lista de passwords
  comprometidas, reutilizacao e palavras contextuais no servico.
- `SecurityConfig` passou a aplicar `Cache-Control: no-store, no-cache`,
  `Pragma: no-cache` e `Expires` a respostas sensiveis de auth, reports,
  admin, analyst e audit.
- `SecurityConfig` passou a aplicar CSP mais restritiva, HSTS com preload,
  COOP/COEP/CORP, validacao Fetch Metadata/Origin para pedidos unsafe e
  rejeicao antecipada de `TRACE`, headers com caracteres de controlo e
  `Authorization` excessivamente grande.
- `SecurityConfigurationValidator` passou a validar configuracao prod-like de
  TLS, reverse proxy, PostgreSQL com validacao de certificado e limites
  positivos de pool/conexoes/threads.
- `RateLimiterService` passou a ter limite especifico para `POST /reports`,
  reduzindo abuso automatizado da submissao publica anonima.
- `ReportService` passou a aplicar quota acumulada de anexos por denuncia,
  alem do limite de ficheiros por pedido.
- `SecurityConfig` passou a rejeitar parametros escalares duplicados fora de
  multipart, mitigando HTTP parameter pollution antes dos controllers.
- `SecurityConfig` passou a rejeitar pedidos HTTP/2 ou HTTP/3 com headers
  connection-specific (`Transfer-Encoding`, `Connection`, `Upgrade`,
  `Keep-Alive` ou `Proxy-Connection`) antes dos controllers.
- As paginas estaticas passaram a carregar `/js/security-support.js`, que
  detecta browsers sem features de seguranca/runtime esperadas, mostra aviso e
  desactiva controlos interactivos.
- `TrackingCodeTest` passou a validar 2.000 tracking codes gerados por
  `SecureRandom` sem colisoes em carga academica moderada.
- `SecurityConfig` passou a bloquear explicitamente paths `/.git` e `/.svn`
  com resposta controlada, reforcando a proteccao contra exposicao de metadados
  de controlo de versao.
- `CRYPTOGRAPHIC_INVENTORY.md` e `CryptographicInventoryTest` passaram a
  manter evidencia de inventario criptografico e deteccao estatica de usos
  esperados de criptografia no codigo.
- `AdminController` passou a disponibilizar
  `POST /admin/users/{id}/password-reset`, permitindo ao admin iniciar reset de
  password sem escolher nem conhecer a nova password.
- `FrontendXssDataExposureTest` passou a cobrir padroes de DOM clobbering,
  garantindo ausencia de `name` em controlos e de acesso `document.<id>` aos
  elementos da pagina.
- `AdminBackupController` passou a exigir reautenticacao por
  `X-Reauth-Password` antes de executar restore de backup para staging.
- `BackupRestoreResponse` e `CasePackageResponse` deixaram de expor paths
  internos/listas de ficheiros gerados, com cobertura em
  `ResponseDataMinimizationTest`.
- `DANGEROUS_FUNCTIONALITY.md` e `DangerousFunctionalityInventoryTest` passaram
  a manter rastreabilidade de operacoes sensiveis.

## Melhorias L2 adicionais

O tracker XLSX foi revisto para todos os capitulos L2. Foram actualizados como
`Compliant` apenas controlos com evidencia verificavel no codigo, testes ou
documentacao: business limits, malware scanner local/EICAR, password policy,
MFA one-time/hashed challenge, JWT `iss`/`aud`/`jti`, crypto key rotation hooks,
no-store em dados sensiveis, limites de recursos, configuracao prod-like,
logging estruturado e tratamento de erros inesperados.

## Melhorias L3 adicionais

O tracker XLSX tambem foi revisto para controlos L3 que ja tinham evidencia
real ou que puderam ser reforcados sem alterar radicalmente a aplicacao:

- `V3.4.7` passou a `Compliant` com CSP `report-uri /security/csp-report`,
  endpoint publico para relatorios CSP, alerta `CSP_VIOLATION` e sanitizacao de
  JWT/tracking codes antes de persistir o alerta.
- `V3.1.1` e `V3.7.4` foram actualizados com base nos headers ja testados:
  CSP, HSTS `includeSubDomains`/`preload`, COOP, CORP, COEP,
  Permissions-Policy e no-store para respostas sensiveis.
- `V8.3.2` foi actualizado porque a validacao JWT consulta o estado actual do
  utilizador e rejeita imediatamente users desactivados.
- `V11.2.4` foi actualizado porque `JwtService` valida HMAC SHA-256 com
  comparacao constante via `MessageDigest.isEqual`.
- `V13.4.7` foi actualizado porque uploads ficam fora dos recursos estaticos e
  o web tier permite apenas paginas publicas explicitas, `/css/**` e `/js/**`.
- `V14.2.5` foi actualizado pela cobertura `no-store/no-cache` em respostas
  sensiveis de auth, reports, admin, analyst, audit e security.
- `V1.2.10` foi reclassificado como `Not Applicable`, porque nao existe export
  CSV/XLSX/ODS no GhostReport actual.
- `V5.2.4` passou a `Compliant` com limite de tamanho, limite por request e
  quota acumulada por denuncia.
- `V11.3.1`, `V11.3.2` e `V11.5.2` foram actualizados com evidencia de
  algoritmos aprovados, ausencia de ECB/PKCS#1 v1.5 e teste de `SecureRandom`
  sob carga moderada.
- `V15.3.7` passou a `Compliant` com rejeicao explicita de HTTP parameter
  pollution por parametros escalares duplicados.
- `V6.3.2` e `V7.4.2` foram corrigidos com base em seed users desactivados em
  prod-like e rejeicao de JWTs quando a conta interna fica inactiva.
- `V1.5.3` passou a `Compliant` com uso consistente de parsers/APIs
  estruturadas: Jackson/Bean Validation nos DTOs, helpers DOM por text node,
  normalizacao de paths/URIs e ausencia de parsing de tracking code via query
  string no browser.
- `V3.7.5` passou a `Compliant` com fallback documentado para browsers sem
  `fetch`, `Promise`, `crypto.getRandomValues`, `TextEncoder` ou APIs DOM
  esperadas.
- `V4.2.3` passou a `Compliant` com rejeicao de headers connection-specific em
  pedidos HTTP/2 e HTTP/3.
- `V6.2.5` passou a `Compliant` porque a aplicacao deixou de exigir classes
  especificas de caracteres nas passwords.
- `V13.4.1` passou a `Compliant` com bloqueio explicito de `/.git` e `/.svn`.
- `V11.1.1`, `V11.1.3` e `V11.1.4` passaram a `Compliant` com inventario
  criptografico documentado, politica de alteracao e teste estatico que garante
  que os mecanismos criptograficos principais continuam mapeados.
- `V6.4.6` passou a `Compliant` porque admins podem iniciar reset de password
  sem definir a password do utilizador.
- `V3.2.3` passou a `Compliant` com teste automatico contra padroes de DOM
  clobbering no frontend estatico.
- `V7.5.3` passou a `Compliant` porque operacoes sensiveis cobertas exigem
  verificacao adicional: password actual em password change e
  `X-Reauth-Password` em restore de backup admin.
- `V14.2.6` passou a `Compliant` porque respostas de backup/package deixam de
  expor paths internos ou listas de ficheiros gerados, com teste de contrato.
- `V15.1.5` passou a `Compliant` com inventario de dangerous functionality
  verificado por teste.
- `V11.3.4` e `V11.3.5` foram reclassificados como `Not Applicable` porque a
  aplicacao nao usa `Cipher`/encriptacao aplicacional com IV ou composicao
  encryption+MAC.

Capitulos fora do escopo implementado, como OAuth/OIDC e WebRTC, permanecem
`Not Applicable`; nao foram convertidos em `Compliant` para evitar claims
enganosos.

## Limitacoes ASVS registadas

- Nao existe agente IAST real; a evidencia e runtime security evidence /
  IAST-like academic evidence.
- ZAP e baseline/passivo e nao cobre contexto autenticado completo.
- MFA em dev/test pode expor codigo em log para demonstracao; producao precisa
  de canal real.
- Rate limiting e em memoria.
- Secret manager, SIEM/WORM, certificado publico/TLS operacional, KMS e Flyway/Liquibase ficam como
  controlos futuros/operacionais.

## Documentos relacionados

- [PHASE2_SPRINT2_REPORT.md](PHASE2_SPRINT2_REPORT.md)
- [AUTHORIZATION_MATRIX.md](AUTHORIZATION_MATRIX.md)
- [SECURITY_TESTING.md](SECURITY_TESTING.md)
- [DEVSECOPS_PIPELINE.md](DEVSECOPS_PIPELINE.md)
- [SCA_TRIAGE.md](SCA_TRIAGE.md)
- [CRYPTOGRAPHIC_INVENTORY.md](CRYPTOGRAPHIC_INVENTORY.md)
- [DANGEROUS_FUNCTIONALITY.md](DANGEROUS_FUNCTIONALITY.md)
- [IAST_RUNTIME_SECURITY.md](IAST_RUNTIME_SECURITY.md)
- [SECURITY_ASSESSMENT.md](SECURITY_ASSESSMENT.md)
