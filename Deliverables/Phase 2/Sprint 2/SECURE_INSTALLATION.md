# Instalação segura

## Perfis

| Perfil | Objectivo | Base de dados | Notas |
| --- | --- | --- | --- |
| `dev` | Desenvolvimento local | PostgreSQL por omissão | Cria utilizadores demo em BD nova e pode expor códigos MFA em logs se explicitamente activado. |
| `test` | Testes automatizados | H2 | Usado apenas por Maven tests. |
| default/prod-like | Baseline de deployment | PostgreSQL | Exige secrets externos e validação de schema. |

## Variáveis necessárias

| Variável | Objectivo | Requisito |
| --- | --- | --- |
| `DB_URL` | JDBC URL | Usar PostgreSQL fora de testes. |
| `DB_USERNAME` | Utilizador da BD | Privilégios mínimos no schema. |
| `DB_PASSWORD` | Password da BD | Guardar em secrets/ambiente. |
| `JWT_SECRET` | Assinatura JWT | Pelo menos 32 caracteres; rodar se exposto. |
| `BACKUP_HMAC_SECRET` | Integridade de manifestos de backup | Guardar como secret. |
| `BACKUP_HMAC_KEY_ID` | Identificador da chave de backup | Usar em rotação manual. |
| `GHOSTREPORT_MFA_EXPOSE_CODE` | Exposição dev-only de MFA em logs | Desligado fora de desenvolvimento local. |

## Execução local

```powershell
cd ghostreport
$env:SPRING_PROFILES_ACTIVE="dev"
$env:DB_PASSWORD="user"
$env:JWT_SECRET="dev-local-secret-with-at-least-32-chars"
.\mvnw.cmd spring-boot:run
```

## Docker Compose

Na raiz do repositório:

```powershell
$env:DB_PASSWORD="<password-local-da-base-de-dados>"
$env:JWT_SECRET="<segredo-aleatorio-com-pelo-menos-32-caracteres>"
docker compose up --build
```

O `docker-compose.yml` define aplicação e PostgreSQL locais. Secrets devem vir
do ambiente e nunca ser commitados.

## Base de dados e schema

A configuração prod-like usa PostgreSQL e validação de schema. O repositório
ainda não inclui Flyway/Liquibase. Antes de produção:

- rever e scriptar mudanças de schema;
- testar arranque contra PostgreSQL limpo;
- validar `ddl-auto=validate`;
- fazer backup da BD antes de alterações;
- adicionar migrações versionadas como hardening futuro.

O perfil `dev` pode usar lógica de update/reparação para iteração local; isto
não deve ser usado como estratégia de migração de produção.

## Armazenamento de ficheiros

Uploads, pacotes de evidência e backups usam filesystem. Deployment deve:

- configurar caminhos fora do web root;
- restringir permissões ao utilizador da aplicação;
- monitorizar espaço em disco;
- fazer backup dos directórios de evidência;
- proteger ZIPs de backup, porque encriptação aplicacional de backup não está
  implementada.

## Checklist de produção

| Check | Estado |
| --- | --- |
| PostgreSQL em vez de H2 | Obrigatório fora de testes. |
| Secrets externalizados | Obrigatório. |
| Exposição de código MFA dev desligada | Obrigatório. |
| HTTPS por reverse proxy ou plataforma | Controlo operacional obrigatório. |
| Revisão de CORS/proxy headers | Obrigatória por ambiente. |
| Logs centralizados/SIEM | Hardening futuro. |
| Flyway/Liquibase | Hardening futuro. |
| Rate limiting externo | Hardening futuro em multi-nó. |

## Credenciais dev

Bases de dados dev novas criam utilizadores internos para demonstração. Estas
credenciais são apenas para testes académicos locais e devem ser alteradas em
qualquer ambiente partilhado.
