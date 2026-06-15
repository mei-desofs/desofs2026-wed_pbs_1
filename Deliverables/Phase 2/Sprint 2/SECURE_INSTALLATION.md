# Instalacao segura

## Perfis

| Perfil | Objectivo | Base de dados | Notas |
| --- | --- | --- | --- |
| `dev` | Desenvolvimento local | PostgreSQL por omissao | Cria utilizadores demo em BD nova e pode expor codigos MFA em logs se explicitamente activado. |
| `test` | Testes automatizados | H2 | Usado apenas por Maven tests. |
| default/prod-like | Baseline de deployment | PostgreSQL | Exige secrets externos, TLS/proxy configurado, PostgreSQL com TLS validado e validacao de schema. |

## Variaveis necessarias

| Variavel | Objectivo | Requisito |
| --- | --- | --- |
| `DB_URL` | JDBC URL | Usar PostgreSQL fora de testes; em prod-like deve incluir `sslmode=verify-ca` ou `sslmode=verify-full`. |
| `DB_USERNAME` | Utilizador da BD | Privilegios minimos no schema. |
| `DB_PASSWORD` | Password da BD | Guardar em secrets/ambiente. |
| `JWT_SECRET` | Assinatura JWT | Pelo menos 32 caracteres; rodar se exposto. |
| `BACKUP_HMAC_SECRET` | Integridade de manifestos de backup | Guardar como secret. |
| `BACKUP_HMAC_KEY_ID` | Identificador da chave de backup | Usar em rotacao manual. |
| `GHOSTREPORT_MFA_EXPOSE_CODE` | Exposicao dev-only de MFA em logs | Desligado fora de desenvolvimento local. |
| `GHOSTREPORT_TRANSPORT_TLS_MODE` | Modo de TLS | `direct` ou `reverse-proxy` em prod-like. |
| `GHOSTREPORT_TRUSTED_PROXY_ENABLED` | Confirma reverse proxy confiavel | `true` quando `GHOSTREPORT_TRANSPORT_TLS_MODE=reverse-proxy`. |
| `SERVER_FORWARD_HEADERS_STRATEGY` | Tratamento de forwarded headers | `framework` ou `native` atras de reverse proxy confiavel. |
| `SERVER_SSL_ENABLED`/`SERVER_SSL_KEY_STORE` | TLS directo | Obrigatorio quando `GHOSTREPORT_TRANSPORT_TLS_MODE=direct`. |
| `SERVER_SSL_ENABLED_PROTOCOLS` | Protocolos TLS | Apenas TLS 1.2/1.3. |
| `SERVER_SSL_CIPHERS` | Cifras TLS | Cifras modernas; defaults definidos em `application-prod.yaml`. |
| `DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE` | Pool Hikari | Limites positivos obrigatorios em prod-like. |
| `SERVER_MAX_CONNECTIONS`, `SERVER_TOMCAT_THREADS_MAX` | Recursos Tomcat | Limites positivos obrigatorios em prod-like. |
| `RATE_LIMIT_REPORT_MAX_ATTEMPTS`, `RATE_LIMIT_REPORT_WINDOW_SECONDS` | Submissao publica anonima | Controla abuso de `POST /reports`. |

## Execucao local

```powershell
cd ghostreport
$env:SPRING_PROFILES_ACTIVE="dev"
$env:DB_PASSWORD="user"
$env:JWT_SECRET="dev-local-secret-with-at-least-32-chars"
.\mvnw.cmd spring-boot:run
```

## Docker Compose

Na raiz do repositorio:

```powershell
$env:DB_PASSWORD="<password-local-da-base-de-dados>"
$env:JWT_SECRET="<segredo-aleatorio-com-pelo-menos-32-caracteres>"
docker compose up --build
```

O `docker-compose.yml` define aplicacao e PostgreSQL locais. Secrets devem vir
do ambiente e nunca ser commitados.

## Base de dados e schema

A configuracao prod-like usa PostgreSQL e validacao de schema. O repositorio
ainda nao inclui Flyway/Liquibase. Antes de producao:

- rever e scriptar mudancas de schema;
- testar arranque contra PostgreSQL limpo;
- validar `ddl-auto=validate`;
- fazer backup da BD antes de alteracoes;
- adicionar migracoes versionadas como hardening futuro.

O perfil `dev` pode usar logica de update/reparacao para iteracao local; isto
nao deve ser usado como estrategia de migracao de producao.

## Transporte seguro

Em perfis prod-like, `SecurityConfigurationValidator` bloqueia arranque se o
modo TLS nao estiver configurado.

- `GHOSTREPORT_TRANSPORT_TLS_MODE=direct`: a aplicacao termina TLS directamente
  e exige `SERVER_SSL_ENABLED=true`, keystore configurado e protocolos limitados
  a TLS 1.2/1.3.
- `GHOSTREPORT_TRANSPORT_TLS_MODE=reverse-proxy`: TLS termina num reverse proxy
  confiavel e a aplicacao exige `SERVER_FORWARD_HEADERS_STRATEGY=framework` ou
  `native` e `GHOSTREPORT_TRUSTED_PROXY_ENABLED=true`.

Para PostgreSQL em producao, `DB_URL` deve validar o certificado do servidor:

```text
jdbc:postgresql://db.example:5432/ghostreport?sslmode=verify-full
```

`sslmode=require` cifra a ligacao, mas nao e aceite pelo validator porque nao
valida suficientemente a identidade do servidor.

## Limites de recursos

Os perfis configuram limites de pool e servidor para reduzir risco de exaustao:

- Hikari: `maximum-pool-size`, `minimum-idle`, `connection-timeout`,
  `validation-timeout`, `idle-timeout`, `max-lifetime`;
- Tomcat: `max-connections`, `accept-count`, `threads.max`,
  `threads.min-spare`, `connection-timeout`;
- HTTP request headers: `server.max-http-request-header-size`.
- Public report submission: `security.rate-limit.report`.

Em prod-like, o arranque falha se os limites obrigatorios forem ausentes ou nao
positivos.

## Armazenamento de ficheiros

Uploads, pacotes de evidencia e backups usam filesystem. Deployment deve:

- configurar caminhos fora do web root;
- restringir permissoes ao utilizador da aplicacao;
- monitorizar espaco em disco;
- fazer backup dos directorios de evidencia;
- proteger ZIPs de backup, porque encriptacao aplicacional de backup nao esta
  implementada.

## Checklist de producao

| Check | Estado |
| --- | --- |
| PostgreSQL em vez de H2 | Obrigatorio fora de testes. |
| `DB_URL` com `sslmode=verify-ca`/`verify-full` | Obrigatorio em PostgreSQL prod-like. |
| Secrets externalizados | Obrigatorio. |
| Exposicao de codigo MFA dev desligada | Obrigatorio. |
| HTTPS por reverse proxy ou TLS directo | Obrigatorio. |
| TLS 1.2/1.3 e cifras modernas | Configurado/validado para modo `direct`. |
| Limites Hikari/Tomcat positivos | Obrigatorio em prod-like. |
| Revisao de CORS/proxy headers | Obrigatoria por ambiente. |
| Logs centralizados/SIEM | Hardening futuro. |
| Flyway/Liquibase | Hardening futuro. |
| Rate limiting externo | Hardening futuro em multi-no. |

## Credenciais dev

Bases de dados dev novas criam utilizadores internos para demonstracao. Estas
credenciais sao apenas para testes academicos locais e devem ser alteradas em
qualquer ambiente partilhado.
