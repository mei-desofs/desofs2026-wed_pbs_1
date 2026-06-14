# GhostReport

GhostReport é uma aplicação web Spring Boot para denúncia anónima, análise
interna de casos e gestão de evidência de auditoria. O projecto foi
desenvolvido no âmbito da unidade curricular de desenvolvimento seguro de
software.

## Capacidades principais

- Submissão anónima de denúncias e verificação pública por código de tracking.
- Upload de evidência com validação de ficheiros, nomes gerados e armazenamento
  seguro.
- Autenticação JWT para utilizadores internos.
- MFA baseado em código antes da emissão de JWT para `ADMIN`, `ANALYST` e `AUDITOR`.
- Controlo de acesso por roles `ADMIN`, `ANALYST` e `AUDITOR`.
- Controlo de propriedade de casos para analistas.
- Logs de auditoria, alertas de segurança e hashes de integridade para registos
  críticos.
- Geração de pacotes de evidência para casos fechados.
- Geração, reposição e verificação de integridade de backups.
- Evidência DevSecOps através de GitHub Actions, SCA, SAST, DAST e SBOM.

## Modelo de roles actual

| Role | Capacidades implementadas |
| --- | --- |
| Denunciante anónimo | Submeter denúncias, verificar códigos de tracking, enviar evidência e descarregar uma cópia da denúncia por código. |
| Analyst | Consultar casos elegíveis, assumir/actualizar casos atribuídos e gerar pacotes de evidência para casos fechados. |
| Auditor | Consultar evidência de auditoria/segurança e verificar pacotes de evidência e backups. |
| Admin | Criar, listar, editar, activar e desactivar utilizadores, consultar informação de auditoria/segurança e gerir backups. |

As roles internas activas são `ADMIN`, `ANALYST` e `AUDITOR`. Linhas legadas
`USER` são tratadas como dados históricos e não fazem parte do modelo de acesso
actual.

## Execução local

A partir do módulo Spring Boot:

```powershell
cd ghostreport
$env:SPRING_PROFILES_ACTIVE="dev"
$env:DB_PASSWORD="user"
$env:JWT_SECRET="dev-local-secret-with-at-least-32-chars"
.\mvnw.cmd spring-boot:run
```

A aplicação usa a porta `8081` por omissão. Em PowerShell, o comando equivalente
com perfil explícito é:

```powershell
.\mvnw.cmd "-Dspring-boot.run.profiles=dev" spring-boot:run
```

O perfil `dev` cria contas `admin`, `analyst` e `auditor` numa base de dados
nova. O login destas roles internas exige MFA; com `GHOSTREPORT_MFA_EXPOSE_CODE=true`,
o código MFA de desenvolvimento é escrito no log da aplicação apenas para testes
locais.

Para execução local com Docker e PostgreSQL:

```powershell
$env:DB_PASSWORD="<password-local-da-base-de-dados>"
$env:JWT_SECRET="<segredo-aleatorio-com-pelo-menos-32-caracteres>"
docker compose up --build
```

## Comandos de teste e evidência

```powershell
cd ghostreport
.\mvnw.cmd test
.\mvnw.cmd test jacoco:report
.\mvnw.cmd -DskipTests compile com.github.spotbugs:spotbugs-maven-plugin:4.8.6.6:spotbugs "-Dspotbugs.xmlOutput=true"
.\mvnw.cmd org.owasp:dependency-check-maven:12.1.0:check -Dformat=ALL -DossindexAnalyzerEnabled=false -DfailOnError=false -DfailBuildOnCVSS=11
.\mvnw.cmd -DskipTests org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom
```

A aplicação usa actualmente Spring Boot `3.5.15`; os módulos Spring Security
são geridos pelo BOM do Spring Boot e resolvem para `6.5.11`.

## Documentação de entrega

A entrega final da Phase 2 Sprint 2 está indexada em:

- [Índice da documentação Sprint 2](Deliverables/Phase%202/Sprint%202/README.md)
- [Relatório principal Sprint 2](Deliverables/Phase%202/Sprint%202/PHASE2_SPRINT2_REPORT.md)

Os relatórios históricos permanecem em `Deliverables/Phase 1/` e
`Deliverables/Phase 2/Sprint 1/`. A documentação específica do Sprint 2 fica
consolidada dentro da pasta da entrega.

## Autores

- Alexandre Vieira
- Barbara Silva
- Sofia Marques
