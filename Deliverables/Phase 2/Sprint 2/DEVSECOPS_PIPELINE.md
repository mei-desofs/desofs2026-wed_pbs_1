# Pipeline DevSecOps e automacoes

## 1. Objectivo

A pipeline DevSecOps do GhostReport transforma a entrega num processo
reprodutivel: compila, testa, mede cobertura, procura secrets, analisa codigo,
verifica dependencias, gera SBOM, arranca a aplicacao, executa checks runtime,
corre ZAP baseline e publica artefactos.

## 2. Workflows existentes

| Workflow | Triggers | Objectivo |
| --- | --- | --- |
| `.github/workflows/dev.yml` | `push` para `main`/`develop`, `pull_request` para `main`/`develop`, `workflow_dispatch` | Pipeline principal de build, testes e seguranca. |
| `.github/workflows/pit.yml` | `workflow_dispatch`, PRs para `main` quando mudam `ghostreport/pom.xml`, `ghostreport/src/main/**`, `ghostreport/src/test/**` ou o proprio workflow, e `push` para `main` com os mesmos paths | Mutation testing completo com PIT. |

O workflow principal usa `concurrency` para cancelar execucoes anteriores da
mesma ref, evitando consumir runner em builds obsoletas.

`.github/dependabot.yml` tambem existe, mas nao e um workflow de CI. Ele abre
actualizacoes semanais para Maven e GitHub Actions; esses PRs disparam os
workflows acima de acordo com os seus triggers e paths.

## 3. Como o GitHub Actions executa os jobs

Os workflows reais do repositorio usam GitHub-hosted runners. Todos os jobs em
`.github/workflows/dev.yml` e `.github/workflows/pit.yml` declaram
`runs-on: ubuntu-latest`; nao ha `self-hosted` runner configurado. Cada job
corre numa VM Linux efemera criada pelo GitHub para aquela execucao e descartada
no fim do job.

Consequencias praticas:

- cada job arranca isolado, mesmo quando pertence ao mesmo workflow;
- os ficheiros criados em `ghostreport/target` existem apenas na copia
  temporaria do runner;
- jobs nao partilham ficheiros automaticamente entre si;
- ficheiros que precisam sobreviver ao job devem ser publicados como
  artefactos com `actions/upload-artifact`;
- dependencias podem ser aceleradas por cache, mas a cache nao e evidencia e
  nao deve guardar secrets.

O primeiro passo tecnico relevante de cada job e o checkout:
`actions/checkout@v6` copia o repositorio para o workspace temporario do runner.
A partir dai os comandos correm sobre essa copia. No job `sast`, o checkout usa
`fetch-depth: 0` para disponibilizar o historico necessario a analises como
SonarCloud; nos restantes jobs e usado o checkout padrao.

Os jobs Maven configuram Java com `actions/setup-java@v5`, distribuicao
`temurin`, `java-version: 17` e `cache: maven`. O Maven Wrapper do projecto
aponta para Maven `3.9.14` em
`ghostreport/.mvn/wrapper/maven-wrapper.properties`, por isso `./mvnw` garante
uma versao Maven consistente no runner. A cache Maven reduz tempo de download de
dependencias, mas nao deve alterar o resultado da build: o `pom.xml`, o wrapper
e os comandos executados continuam a definir o comportamento.

Servicos auxiliares tambem vivem apenas durante o job. `build-test` e
`dast-scan` criam um servico `postgres:16` associado ao job, exposto em
`localhost:5432`, com base de dados `ghostreport`, utilizador `postgres` e
password de teste `user`. Este PostgreSQL e usado por testes e runtime scan, e
os dados desaparecem quando o job termina. `security-secrets`, `sast`,
`dependency-scanning` e `pit` nao declaram servicos; quando testes correm sem
esse servico, o perfil de teste usa H2 em memoria
(`application-test.yaml`/`@ActiveProfiles("test")`). O runtime do `dast-scan`
usa perfil `dev`, que aponta para PostgreSQL atraves de `DB_URL`.

```mermaid
flowchart TD
    A["Push / Pull Request / workflow_dispatch"] --> B["GitHub Actions cria runner ubuntu-latest efemero"]
    B --> C["actions/checkout copia o repositorio"]
    C --> D["Setup Java 17 + cache Maven quando necessario"]

    D --> E["build-test"]
    C --> F["security-secrets"]
    D --> G["sast"]
    D --> H["dependency-scanning"]
    D --> I["dast-scan"]
    D --> J["pit.yml: pit quando acionado"]

    E --> E1["Maven verify + Surefire + JaCoCo"]
    F --> F1["Gitleaks"]
    G --> G1["CodeQL init/analyze + SpotBugs + SonarCloud"]
    H --> H1["Dependency-Check + CycloneDX SBOM"]
    I --> I1["App em localhost + runtime probes + ZAP"]
    J --> J1["PIT mutation testing"]

    E1 --> K["Upload de artefactos"]
    F1 --> K
    G1 --> K
    H1 --> K
    I1 --> K
    J1 --> K

    K --> L["Runner descartado"]
    K --> M["Equipa reve evidencias e findings"]
    M --> N{"Findings criticos confirmados?"}
    N -->|Sim| O["Corrigir antes de merge"]
    N -->|Nao| P["Code review + merge quando aceitavel"]
```

## 4. Fluxo completo developer -> merge

O fluxo esperado da equipa e:

1. Developer cria uma branch curta a partir de `main` ou `develop`.
2. Implementa a alteracao e actualiza testes/documentacao quando o claim de
   seguranca muda.
3. Corre localmente os testes relevantes; para backend, o minimo e
   `cd ghostreport; .\mvnw.cmd test`.
4. Abre pull request para `main` ou `develop` com resumo, risco e evidencia
   relevante. A branch `main` actual nao contem `.github/pull_request_template.md`.
5. O workflow `dev.yml` arranca automaticamente para o PR.
6. `build-test` executa `./mvnw verify`, testes e JaCoCo.
7. `security-secrets` executa Gitleaks contra o repositorio.
8. `sast` compila, corre SpotBugs, CodeQL e SonarCloud quando `SONAR_TOKEN`
   esta configurado.
9. `dependency-scanning` executa OWASP Dependency-Check e gera SBOM CycloneDX.
10. `dast-scan` corre testes runtime, arranca a app, executa probes HTTP,
    verifica logs e corre ZAP baseline.
11. Artefactos e GitHub Step Summary ficam disponiveis para revisao.
12. A equipa avalia findings: confirmar, corrigir, justificar falso positivo ou
    documentar limitacao.
13. Findings criticos confirmados bloqueiam merge ate correcao.
14. Findings informativos ou fora do ambito podem ser aceites com justificacao
    em triagem/documentacao.
15. So depois de revisao humana e checks aceitaveis o PR deve ser merged.

Visao rapida do mesmo fluxo:

```mermaid
flowchart LR
    A["Developer cria branch"] --> B["Implementa alteracao"]
    B --> C["Testes locais"]
    C --> D["Pull Request"]
    D --> E["dev.yml"]
    E --> F["build-test: Maven verify + JaCoCo"]
    E --> G["security-secrets: Gitleaks"]
    F --> H["sast: SpotBugs + CodeQL + SonarCloud"]
    G --> H
    F --> I["dependency-scanning: Dependency-Check + CycloneDX SBOM"]
    F --> J["dast-scan: runtime evidence + ZAP baseline"]
    H --> K["Artefactos e Step Summary"]
    I --> K
    J --> K
    K --> L["Triagem de findings"]
    L --> M["Code review"]
    M --> N["Merge quando aceitavel"]
```

## 5. Quando corre e em que branches

| Evento | Branches/paths | Resultado esperado |
| --- | --- | --- |
| Pull request para `main` ou `develop` | Qualquer alteracao coberta pelo PR | Executa pipeline principal antes de merge. |
| Push para `main` ou `develop` | Commits directos ou merges | Revalida a branch integrada. |
| `workflow_dispatch` | Manual | Permite repetir evidencia ou validar entrega. |
| PIT PR/push | Apenas paths Java/Maven/workflow em `main` | Mutation testing quando altera codigo/testes relevantes. |

Branches de documentacao continuam a disparar `dev.yml` quando abrem PR para as
branches alvo, mas PIT so corre se os paths configurados forem alterados.

## 6. Checks bloqueantes e findings aceitaveis

| Area | Modo no repositorio | Decisao de merge |
| --- | --- | --- |
| Maven build/testes/JaCoCo | `./mvnw verify` no job `build-test` | Bloqueante: falha deve ser corrigida. |
| Gitleaks | Job termina com exit code da ferramenta | Bloqueante para leaks confirmados. Falso positivo deve ser justificado/remediado via configuracao. |
| SonarCloud | Falha se `SONAR_TOKEN` ausente ou analise falhar | Bloqueante no estado actual do workflow; se o token nao estiver configurado, a run deve ser tratada como limitacao operacional, nao como evidencia SonarCloud concluida. |
| CodeQL | `init` antes de SonarCloud e `analyze` depois de SonarCloud | Findings confirmados de alta severidade devem ser corrigidos antes de merge. Se SonarCloud falhar antes, o `analyze` nao chega a executar nessa run. |
| SpotBugs | Gera evidencia SAST no job `sast` | Falha tecnica do comando bloqueia o job; findings confirmados devem ser corrigidos ou documentados em triagem. |
| Dependency-Check | Evidence mode com `failBuildOnCVSS=11` e `continue-on-error` | Nao bloqueia automaticamente por CVSS; vulnerabilidades reais devem ser corrigidas ou justificadas em [SCA_TRIAGE.md](SCA_TRIAGE.md). |
| CycloneDX SBOM | Gera `bom.json`/`bom.xml` | Bloqueia apenas se a geracao tecnica falhar e a evidencia for necessaria. |
| Runtime tests no `dast-scan` | Maven tests seleccionados | Bloqueante para controlos runtime. |
| ZAP baseline | `continue-on-error` com `-I` | Evidencia/review; findings sao triados, nao bloqueiam automaticamente. |
| PIT | Workflow separado sem `continue-on-error` | Bloqueia a propria run quando falha, se o workflow for acionado; nao substitui o gate rapido de PR do `dev.yml`. |

O repositorio nao contem configuracao de branch protection. Portanto, a partir
dos ficheiros so e possivel afirmar quais jobs falham tecnicamente; se esses
checks bloqueiam merge automaticamente depende da configuracao do GitHub no
repositorio remoto.

Interpretacao pratica:

- falha de compilacao, testes, coverage ou secrets confirmados impede merge;
- CVE exploravel em dependencia usada deve ser corrigido antes de aceitar o PR;
- CVE nao aplicavel, falso positivo SAST ou alerta ZAP informativo pode ser
  aceite com justificacao e link para evidencia;
- ausencia de secret operacional, como `SONAR_TOKEN`, deve ser tratada como
  limitacao de ambiente, nao como claim de seguranca implementado.

## 7. Job `build-test`

Ambiente tecnico:

- workflow: `.github/workflows/dev.yml`;
- runner: `ubuntu-latest`;
- timeout: 35 minutos;
- working directory dos comandos: `./ghostreport`;
- servico auxiliar: container `postgres:16` em `localhost:5432`;
- Java: Temurin 17 com cache Maven;
- Maven: `./mvnw`, usando o Maven Wrapper do projecto.

Execucao interna:

- checkout;
- configuracao Java 17 com cache Maven;
- `./mvnw verify` com `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` apontados para o
  PostgreSQL efemero do job;
- upload de Surefire reports;
- upload de JaCoCo;
- publicacao de sumario no GitHub Step Summary.

Valor de seguranca:

- impede regressao funcional;
- garante que testes de seguranca entram no gate normal;
- gera artefactos revistos pelo professor/equipa;
- JaCoCo evita que novas areas fiquem sem cobertura minima.

Gate: bloqueante para compilacao, testes e regras JaCoCo, porque nao usa
`continue-on-error`.

## 8. Job `security-secrets`

Ambiente tecnico:

- workflow: `.github/workflows/dev.yml`;
- runner: `ubuntu-latest`;
- timeout: 10 minutos;
- working directory dos comandos: `./ghostreport`;
- nao configura Java porque a ferramenta corre em Docker;
- nao usa servico de base de dados.

Execucao interna:

- corre Gitleaks em Docker;
- usa `.gitleaks.toml`;
- gera `target/gitleaks/gitleaks-report.json`;
- publica artefacto `secret-scan-gitleaks-json`;
- escreve sumario de evidencia.

Mitigacao STRIDE:

- reduz Information Disclosure por secrets commitados;
- ajuda a detectar tokens/passwords/keys antes de merge.

Gate: bloqueante para leaks confirmados, porque o script termina com o exit code
do Gitleaks. O relatorio e redigido com `--redact`.

## 9. Job `sast`

Ambiente tecnico:

- workflow: `.github/workflows/dev.yml`;
- runner: `ubuntu-latest`;
- timeout: 30 minutos;
- depende de `build-test` e `security-secrets`;
- working directory dos comandos: `./ghostreport`;
- Java: Temurin 17 com cache Maven;
- cache adicional: `~/.sonar/cache` com `actions/cache@v5`;
- permissions incluem `security-events: write` para CodeQL/Code Scanning.

Execucao interna:

- inicializa CodeQL para Java;
- compila o projecto para analise;
- corre SpotBugs;
- corre SonarCloud usando `SONAR_TOKEN` e variaveis de projecto/organizacao
  quando configuradas;
- executa CodeQL analyze;
- publica `sast-reports`.

Notas importantes:

- CodeQL envia resultados para GitHub Code Scanning;
- SpotBugs gera XML/site como evidencia;
- SonarCloud depende do secret `SONAR_TOKEN`;
- `SONAR_PROJECT_KEY` e `SONAR_ORGANIZATION` sao lidos de GitHub Actions vars,
  com valores default no script;
- se `SONAR_TOKEN` estiver ausente, o script escreve
  `target/sast-evidence/sonarcloud-summary.txt` e falha o job antes do passo
  `Run CodeQL analysis`;
- SAST e evidencia complementar, nao prova ausencia de vulnerabilidades.

Gate: bloqueante para falhas tecnicas do job SAST. Findings SAST devem ser
triados; findings confirmados de severidade alta/critica devem ser corrigidos
antes de merge.

## 10. Job `dependency-scanning`

Ambiente tecnico:

- workflow: `.github/workflows/dev.yml`;
- runner: `ubuntu-latest`;
- timeout: 25 minutos;
- depende de `build-test` e `security-secrets`;
- working directory dos comandos: `./ghostreport`;
- Java: Temurin 17 com cache Maven;
- sem servico de base de dados;
- permissions incluem `security-events: write` para upload SARIF.

Execucao interna:

- OWASP Dependency-Check `12.1.0`;
- formatos HTML/XML/JSON/SARIF;
- upload SARIF para GitHub Code Scanning quando gerado;
- CycloneDX `makeAggregateBom`;
- artefactos `dependency-check-sca-reports` e `sbom-cyclonedx`;
- sumario no Step Summary.

Evidencia local recente:

- a arvore Maven actual ja nao resolve Spring Security `6.5.10`;
- Spring Boot BOM `3.5.15` resolve Spring Security `6.5.11`;
- SBOM permite listar componentes e suportar triagem futura.

O estado remoto exacto de GitHub Code Scanning/Dependabot Alerts deve ser
confirmado na interface GitHub. O clone local confirma a arvore Maven e os
ficheiros de workflow, mas nao substitui a metadata de alertas remotos.

Gate: o passo Dependency-Check usa `continue-on-error: true` e
`failBuildOnCVSS=11`, por isso vulnerabilidades detectadas funcionam como
evidencia de triagem e nao como gate automatico por CVSS. Falhas tecnicas
posteriores, como impossibilidade de gerar SBOM, podem falhar o job.

## 11. Job `dast-scan`

Ambiente tecnico:

- workflow: `.github/workflows/dev.yml`;
- runner: `ubuntu-latest`;
- timeout: 35 minutos;
- depende de `build-test` e `security-secrets`;
- working directory dos comandos: `./ghostreport`;
- servico auxiliar: container `postgres:16` em `localhost:5432`;
- Java: Temurin 17 com cache Maven;
- runtime: JAR Spring Boot iniciado em `http://localhost:8081` com perfil
  `dev`;
- OWASP ZAP corre em container Docker com `--network host`.

Execucao interna:

- corre testes runtime de seguranca seleccionados;
- empacota a aplicacao;
- arranca GhostReport em `localhost:8081` usando o PostgreSQL efemero do job;
- espera readiness;
- exercita paginas publicas, reports, tracking code, uploads, download/listagem
  de anexos, login/MFA/logout/password reset, endpoints admin, analyst e auditor,
  JWT invalido, Authorization malformado e outros casos negativos;
- verifica logs para fuga de dados sensiveis;
- corre OWASP ZAP baseline;
- prepara sumario IAST-like/runtime;
- publica `iast-runtime-security-evidence`;
- publica `dast-zap-baseline-reports`;
- para o processo no fim.

O ZAP baseline e evidencia passiva/nao autenticada. A parte runtime cobre mais
do que o ZAP sozinho, porque combina testes, app real, HTTP probes e logs.

Validacao local expandida do probe em 2026-06-15:

| Metrica | Valor |
| --- | --- |
| Total probes | 101 |
| Passed | 101 |
| Failed | 0 |
| Skipped | 0 |
| Public endpoint probes | 23 |
| Admin endpoint probes | 22 |
| Analyst endpoint probes | 17 |
| Auditor endpoint probes | 13 |
| Negative-case probes | 6 |

Nao houve probes skipped na validacao local. `GET /login.html` e tratado como
controlo de exposicao quando responde `401/404`, e o restore destrutivo de
backup continua fora do probe runtime porque exige reautenticacao e e coberto
por testes automatizados. O workflow publica o JSON
`runtime-probe-summary.json` para confirmar estes numeros por run.

Artefactos documentados na entrega:

- [iast-runtime-evidence.md](iast-runtime-evidence.md)
- [runtime-endpoints.md](runtime-endpoints.md)
- [runtime-log-sanitization.md](runtime-log-sanitization.md)

Gate: os testes runtime, build do JAR, arranque da app e readiness sao
bloqueantes. O passo ZAP usa `continue-on-error: true` e `-I`, por isso funciona
como evidencia DAST baseline/passiva; findings ZAP precisam de triagem humana.

## 12. Workflow `pit-mutation-testing`

Ambiente tecnico:

- workflow: `.github/workflows/pit.yml`;
- job: `pit`;
- runner: `ubuntu-latest`;
- timeout: 90 minutos;
- triggers: `workflow_dispatch`, PRs para `main` e pushes para `main` quando
  mudam `ghostreport/pom.xml`, `ghostreport/src/main/**`,
  `ghostreport/src/test/**` ou `.github/workflows/pit.yml`;
- working directory dos comandos: `./ghostreport`;
- Java: Temurin 17 com cache Maven;
- sem servico de base de dados.

Execucao interna:

- prepara Java 17 e cache Maven;
- compila testes;
- inventaria classes alvo;
- executa PIT com configuracao do `pom.xml`;
- valida existencia de `target/pit-reports/index.html`;
- gera sumario Markdown;
- publica `pit-mutation-testing-report`.

PIT fica separado porque e mais lento. Serve para avaliar qualidade dos testes e
nao para bloquear todos os commits rapidamente.

Gate: sem `continue-on-error`; falha se nao encontrar classes alvo, se o PIT
falhar ou se `target/pit-reports/index.html` nao for gerado. O artefacto usa
`if-no-files-found: error`.

## 13. Artefactos esperados

Artefactos de GitHub Actions sao ficheiros copiados do runner para o run do
workflow, para revisao posterior. Eles sao diferentes dos ficheiros temporarios
do workspace: sem upload por `actions/upload-artifact`, relatorios gerados no
runner desaparecem quando a VM efemera e descartada.

| Artefacto | Origem | Uso |
| --- | --- | --- |
| `ci-surefire-test-reports` | `build-test` | Evidencia de testes. |
| `ci-jacoco-coverage-report` | `build-test` | Cobertura. |
| `secret-scan-gitleaks-json` | `security-secrets` | Secrets scan. |
| `sast-reports` | `sast` | SpotBugs/SonarCloud notes. |
| GitHub Code Scanning alerts | CodeQL e Dependency-Check SARIF | Findings centralizados. |
| `dependency-check-sca-reports` | `dependency-scanning` | SCA HTML/XML/JSON/SARIF. |
| `sbom-cyclonedx` | `dependency-scanning` | SBOM JSON/XML. |
| `iast-runtime-security-evidence` | `dast-scan` | Runtime/IAST-like. |
| `dast-zap-baseline-reports` | `dast-scan` | ZAP HTML/XML/JSON e logs. |
| `pit-mutation-testing-report` | `pit.yml` | Mutation testing. |

Os workflows nao definem `retention-days`, por isso a retencao concreta segue a
configuracao default do GitHub/repo/organizacao. Nao se deve assumir que os
artefactos ficam guardados para sempre.

Artefactos temporarios gerados em `target/` nao devem ser commitados no
repositorio; a documentacao da entrega referencia o tipo de evidencia e, quando
util, inclui resumos Markdown.

## 14. Cache, artefactos, secrets e variaveis

Cache e artefactos nao resolvem o mesmo problema:

- cache Maven (`actions/setup-java` com `cache: maven`) acelera builds ao
  reaproveitar dependencias descarregadas;
- cache Sonar (`actions/cache` em `~/.sonar/cache`) acelera o job `sast`;
- artefactos guardam evidencias como Surefire, JaCoCo, Dependency-Check, SBOM,
  SpotBugs, ZAP, logs runtime e PIT;
- nenhum deles deve ser usado para guardar secrets.

Secrets e variaveis vistos nos workflows:

| Nome | Tipo | Onde aparece | Uso |
| --- | --- | --- | --- |
| `SONAR_TOKEN` | GitHub Actions secret | `sast` | Autenticar SonarCloud. |
| `SONAR_PROJECT_KEY` | GitHub Actions variable | `sast` | Project key SonarCloud, com default no script. |
| `SONAR_ORGANIZATION` | GitHub Actions variable | `sast` | Organizacao SonarCloud, com default no script. |
| `NVD_API_KEY` | GitHub Actions secret | `dependency-scanning` | Passado ao Dependency-Check quando configurado. |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | variaveis de ambiente do job | `build-test`, `dast-scan` | Credenciais de teste para PostgreSQL efemero do job. |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | variaveis do servico | `build-test`, `dast-scan` | Configuracao do container `postgres:16`. |

`POSTGRES_PASSWORD: user` e `DB_PASSWORD: user` sao credenciais de teste dentro
do runner efemero, nao GitHub Actions secrets. Secrets reais sao injectados no
runner apenas durante o job que os declara e nao devem ser impressos nos logs,
incluidos no SBOM nem guardados em artefactos. O Gitleaks procura secrets
commitados no repositorio; isso e diferente de usar secrets de runtime
injectados pelo GitHub Actions.

O job `dast-scan` tambem procura nos logs padroes sensiveis, incluindo nomes
como `JWT_SECRET` e `BACKUP_HMAC_SECRET`. Essa verificacao indica apenas que o
workflow procura fugas desses padroes em logs; nao significa que esses secrets
estejam configurados no GitHub Actions.

## 15. Relacao com STRIDE

| STRIDE | Pipeline/automacao |
| --- | --- |
| Spoofing | Testes de auth/JWT/MFA no build; runtime probes em CI. |
| Tampering | Testes de backup/package integrity; SAST; dependency scanning. |
| Repudiation | Testes de audit logs; runtime logs arquivados. |
| Information Disclosure | Gitleaks, frontend XSS/data exposure tests, log leakage checks, ZAP baseline. |
| Denial of Service | Rate limiter tests, upload limits, ZAP baseline como sinal passivo. |
| Elevation of Privilege | RBAC/ownership tests no build; CodeQL/SpotBugs como apoio. |

## Code review process

O fluxo de revisao pretendido no GhostReport e branch-based: as alteracoes sao
isoladas em branches, revistas por pull request e validadas por workflows antes
de serem integradas. A evidencia local do repositorio confirma merges de PRs,
branches `feature/*`, `fix/*`, `docs/*`, `security/*`/`ci-*`, Dependabot e os
workflows `dev.yml` e `pit.yml`.

GitHub pull request approval metadata must be checked in the GitHub interface because it is not fully available from the local repository clone.

O code review nao e apenas leitura manual. Ele combina revisao humana, testes,
analise estatica, analise de dependencias, runtime evidence, ZAP baseline,
artefactos e revisao de documentacao.

Fluxo de revisao:

1. Criar branch curta com ambito claro.
2. Implementar a alteracao mantendo a arquitectura existente.
3. Correr testes locais relevantes; para backend, o minimo recomendado e
   `cd ghostreport; .\mvnw.cmd test`.
4. Abrir pull request com resumo, motivo e impacto de seguranca.
5. Validar a run de `dev.yml`: build/testes/JaCoCo, Gitleaks, SAST,
   Dependency-Check, SBOM, runtime evidence e ZAP baseline.
6. Executar ou rever `pit.yml` quando a alteracao toca codigo/testes Java e o
   custo temporal se justifica.
7. Triar findings: corrigir, aceitar com justificacao, suprimir com prazo de
   revisao ou documentar limitacao.
8. Actualizar testes, matriz de autorizacao, ASVS ou anexos quando uma claim de
   seguranca muda.
9. Integrar apenas quando os checks e riscos confirmados forem aceitaveis.

### Code review responsibilities

| Area revista | O que e verificado | Evidencia |
| --- | --- | --- |
| Funcionalidade | Alteracao cumpre o objectivo e nao quebra fluxos existentes. | Testes Maven, runtime probes e revisao manual. |
| Seguranca | Autenticacao, autorizacao, validacao, logging, uploads, backups, errors e secrets. | Security tests, SAST, DAST/runtime, Gitleaks e [AUTHORIZATION_MATRIX.md](AUTHORIZATION_MATRIX.md). |
| Dependencias | CVEs, versoes vulneraveis, suppressions e SBOM. | Dependency-Check, Dependabot, CycloneDX e [SCA_TRIAGE.md](SCA_TRIAGE.md). |
| Qualidade | Code smells, duplicacao, complexidade, cobertura e mutacoes quando aplicavel. | SonarCloud, SpotBugs, JaCoCo e PIT. |
| Documentacao | Claims tecnicos alinhados com codigo, outputs e artefactos. | README, relatorio principal, ASVS XLSX e anexos. |

### Security review checklist

- A alteracao muda autenticacao, autorizacao, roles ou ownership?
- A alteracao cria ou altera endpoint?
- O endpoint foi adicionado a [AUTHORIZATION_MATRIX.md](AUTHORIZATION_MATRIX.md)?
- Existem testes positivos e negativos para roles relevantes?
- Ha validacao de input por DTO, Bean Validation, enum ou allowlist?
- Ha risco de mass assignment por binding indevido?
- Ha risco de path traversal, ZIP Slip ou acesso indevido ao filesystem?
- Ha risco de expor passwords, JWTs, tracking codes, MFA codes, secrets,
  stack traces ou paths internos?
- A alteracao afecta uploads, backups, evidence packages ou logs?
- A alteracao adiciona ou actualiza dependencias?
- O impacto em SCA/SBOM foi revisto?
- A documentacao e a evidencia ASVS foram actualizadas quando a claim mudou?

### Coding standards and naming conventions

O projecto segue convencoes leves alinhadas com a estrutura real do codigo:

| Area | Regra | Validacao |
| --- | --- | --- |
| Controllers | Recebem HTTP, aplicam validacao inicial/DTOs e devolvem respostas; nao devem concentrar regras de negocio. | Code review, MockMvc e testes de controller/security. |
| Services | Centralizam regras de negocio, ownership, workflow, filesystem seguro e decisoes sensiveis. | Testes unitarios/integracao em `service` e `security`. |
| Repositories | Limitados a persistencia Spring Data/JPA. | Revisao de codigo e testes de integracao. |
| DTOs | Requests/responses evitam expor entidades JPA directamente e reduzem mass assignment. | API tests, review e checklist de seguranca desta seccao. |
| Validacao | Usar Bean Validation, enums, allowlists e domain primitives quando fizer sentido. | `ApiValidationContractTest`, testes de dominio e security tests. |
| Frontend | Nao guardar tokens em storage, nao colocar logica sensivel no browser e evitar sinks XSS. | `FrontendXssDataExposureTest` e runtime probes. |
| Logs/erros | Nao escrever passwords, JWTs, tracking codes, MFA codes ou secrets; nao devolver stack traces/paths internos. | Runtime log sanitization, `ErrorHandlingSecurityTest` e audit/logging tests. |
| Endpoints | Novas rotas devem seguir agrupamento por contexto e ter RBAC positivo/negativo. | [AUTHORIZATION_MATRIX.md](AUTHORIZATION_MATRIX.md) e `RbacAuthorizationMatrixTest`. |
| Dependencias | CVEs devem ser triados e SBOM actualizado. | [SCA_TRIAGE.md](SCA_TRIAGE.md), Dependency-Check e CycloneDX. |
| Documentacao | Numeros e claims devem vir de outputs reais ou artefactos verificaveis. | README, relatorio, ASVS XLSX e anexos. |

Convencoes observadas:

| Tipo | Convencao usada |
| --- | --- |
| Controllers | `*Controller.java`, por exemplo `AdminController` e `AdminBackupController`. |
| Services | `*Service.java`, por exemplo `JwtService`, `MfaChallengeService`, `BackupService`. |
| Repositories | `*Repository.java`. |
| DTO/request/response | `*Request.java`, `*Response.java` ou `*Dto.java`. |
| Security/config | Nome associado ao controlo, como `SecurityConfig`, `JwtAuthenticationFilter`, `SecurityConfigurationValidator`. |
| Testes | `*Test.java`, `*IntegrationTest.java` ou nomes descritivos de security tests. |
| Documentacao principal | Anexos principais em `UPPER_SNAKE_CASE.md`. |
| Evidencia gerada/espelhada | Artefactos runtime em `kebab-case.md`, como `runtime-endpoints.md`. |

Rotas novas devem respeitar o agrupamento actual:

- publico: `/reports`, `/reports/verify`, `/reports/download`;
- autenticacao: `/auth/**`;
- administracao: `/admin/**`;
- analista: `/analyst/**`;
- auditoria: `/audit/**`.

Endpoints publicos nao devem expor dados internos, paths, hashes, stack traces,
tokens ou tracking codes em URL. Operacoes sensiveis devem exigir role adequada
e, quando aplicavel, validacao adicional como tracking code, ownership, CSRF ou
reautenticacao.

### Branch and commit naming

O historico local mostra branches `feature/*`, `fix/*`, `docs/*`, `security/*`
e `ci`/pipeline-oriented, mas os commits antigos nao sao totalmente uniformes.
Por isso, as convencoes abaixo devem ser tratadas como pratica recomendada e
evolucao do processo, nao como regra historica absoluta:

| Prefixo | Uso recomendado |
| --- | --- |
| `feature/...` ou `feat/...` | Novas funcionalidades. |
| `fix/...` | Correcao funcional, seguranca ou regressao. |
| `docs/...` | Documentacao e evidencia. |
| `test/...` | Testes, probes e evidencia automatizada. |
| `security/...` | Alteracoes directamente relacionadas com controlos de seguranca. |
| `ci/...` | Workflows, automacoes e artefactos. |

Commits devem preferir Conventional Commits simples: `feat:`, `fix:`, `docs:`,
`test:`, `refactor:`, `security:` e `ci:`.

Critérios de triagem:

| Resultado | Tratamento |
| --- | --- |
| Vulnerabilidade confirmada em codigo proprio | Corrigir codigo e adicionar/actualizar teste. |
| CVE em dependencia directa/transitiva usada | Actualizar versao/BOM ou justificar mitigacao temporaria. |
| Falso positivo SCA/SAST | Documentar componente, regra/CVE, motivo e data de revisao. |
| ZAP baseline informativo | Avaliar impacto; corrigir se expuser controlos reais ou documentar como hardening futuro. |
| Evidencia incompleta por ambiente | Repetir workflow ou documentar limitação operacional sem transformar em claim. |

Tabela de decisao operacional:

| Tipo de finding | Decisao esperada |
| --- | --- |
| Build, testes ou JaCoCo falham | Corrigir antes de merge. |
| Secret confirmado | Remover, rodar secret quando aplicavel e bloquear merge ate remediacao. |
| CVE aplicavel/exploravel | Actualizar dependencia/BOM ou documentar mitigacao temporaria. |
| CVE nao aplicavel/falso positivo | Documentar suppression especifica, motivo e prazo de revisao. |
| CodeQL/Sonar/SpotBugs critico confirmado | Corrigir antes de merge ou documentar risco se nao for exploravel. |
| ZAP baseline informativo | Triar e aceitar com justificacao se nao representar risco real. |
| Runtime probe falha | Corrigir endpoint, teste ou evidencia antes de aceitar a entrega. |
| PIT fraco em area critica | Adicionar testes quando fizer sentido para o risco. |
| Evidencia incompleta por ambiente | Repetir workflow ou documentar limitacao sem transformar em claim. |

### Code review evidence

| Evidencia local | Objectivo | Checks relevantes | Limite da evidencia |
| --- | --- | --- | --- |
| Merge PR `#54` de `docs/evaluate-documentation-visual-improvements` | Remocao de template PR nao usado e melhorias de documentacao. | Docs e navegacao de evidencia. | Aprovacoes/reviewers devem ser vistos no GitHub. |
| Merge PR `#53` de `fix/asvs-final-l1-l2-l3-hardening` | Hardening ASVS e SecurityConfig. | Build/testes, docs ASVS e triagem ZAP. | Aprovacoes/reviewers devem ser vistos no GitHub. |
| Merge PR `#52` de `docs/complete-sprint2-documentation` | Consolidacao de documentacao Sprint 2 e evidencia runtime/SCA/ASVS. | Docs, runtime probes, SCA e ASVS. | Metadata detalhada de review nao esta no clone local. |
| Merge PR `#51` de `docs/finalize-phase2-sprint2-documentation` | Finalizacao Sprint 2, MFA, PIT/runtime e coverage. | Maven, PIT, DAST/runtime e docs. | Resultado exacto dos checks deve ser consultado no run GitHub. |
| Merge PR `#50` de `fix/spring-security-dependency-alerts` | Remediacao CVEs Spring Security via BOM. | SCA, dependency tree e testes. | Aprovacoes formais nao sao visiveis localmente. |
| Merge PR `#48` de `feat/project-final-review` | Revisao final, MFA/admin e documentacao. | Testes, docs e security review. | Metadata de reviewer nao fica preservada no log local. |
| `.github/dependabot.yml` | PRs semanais para Maven e GitHub Actions. | Dependency review, SCA e workflows. | Dependabot nao substitui triagem humana. |

Local repository evidence confirms branch-based development, automated security
workflows and documented triage gates. The table above is repository evidence,
not a substitute for PR approval metadata in GitHub.

### Relationship with ASVS

O code review suporta evidencias ASVS em secure coding, autenticacao,
autorizacao, validacao, logging, configuracao, dependency management, error
handling e runtime testing. Quando uma alteracao muda uma claim ASVS, o tracker
Excel continua a ser a fonte principal e [ASVS_EVIDENCE.md](ASVS_EVIDENCE.md)
funciona como resumo explicativo.

### Triagem ZAP actual

| Finding ZAP | Estado | Decisao tecnica |
| --- | --- | --- |
| `CSP: Notices` | Corrigido | `SecurityConfig` usa `report-to csp-endpoint` e header `Report-To` em vez de depender de `report-uri`. |
| `Cookie No HttpOnly Flag` (`XSRF-TOKEN`) | Aceite | O frontend precisa ler o cookie para enviar `X-XSRF-TOKEN`; nao e cookie de sessao/JWT e fica com `SameSite=Lax`. |
| `Non-Storable Content` | Aceite informacional | `no-store` e mantido para endpoints/paginas sensiveis. |
| `Session Management Response Identified` (`XSRF-TOKEN`) | Aceite informacional | O cookie identifica proteccao CSRF, nao uma sessao autenticada. |

## 17. Limitacoes da pipeline

- Branch protection e configuracao do GitHub nao sao totalmente comprovaveis por ficheiros.
- SonarCloud depende de `SONAR_TOKEN`.
- ZAP baseline nao e DAST autenticado completo.
- IAST-like e runtime security testing academico; nao e full IAST,
  agent-based IAST, JVM-agent telemetry, taint tracking ou source-to-sink
  telemetry.
- PIT pode ser demorado e por isso fica separado.
