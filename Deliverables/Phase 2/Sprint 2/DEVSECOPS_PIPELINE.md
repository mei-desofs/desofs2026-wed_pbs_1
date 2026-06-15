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
| `.github/workflows/pit.yml` | `workflow_dispatch`, PRs para `main` quando mudam `ghostreport/pom.xml`, `ghostreport/src/**` ou o proprio workflow, e `push` para `main` com os mesmos paths | Mutation testing completo com PIT. |

O workflow principal usa `concurrency` para cancelar execucoes anteriores da
mesma ref, evitando consumir runner em builds obsoletas.

## 3. Fluxo completo developer -> merge

O fluxo esperado da equipa e:

1. Developer cria uma branch curta a partir de `main` ou `develop`.
2. Implementa a alteracao e actualiza testes/documentacao quando o claim de
   seguranca muda.
3. Corre localmente os testes relevantes; para backend, o minimo e
   `cd ghostreport; .\mvnw.cmd test`.
4. Abre pull request para `main` ou `develop` usando o template do repositorio.
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

## 4. Quando corre e em que branches

| Evento | Branches/paths | Resultado esperado |
| --- | --- | --- |
| Pull request para `main` ou `develop` | Qualquer alteracao coberta pelo PR | Executa pipeline principal antes de merge. |
| Push para `main` ou `develop` | Commits directos ou merges | Revalida a branch integrada. |
| `workflow_dispatch` | Manual | Permite repetir evidencia ou validar entrega. |
| PIT PR/push | Apenas paths Java/Maven/workflow em `main` | Mutation testing quando altera codigo/testes relevantes. |

Branches de documentacao continuam a disparar `dev.yml` quando abrem PR para as
branches alvo, mas PIT so corre se os paths configurados forem alterados.

## 5. Checks bloqueantes e findings aceitaveis

| Area | Modo no repositorio | Decisao de merge |
| --- | --- | --- |
| Maven build/testes/JaCoCo | `./mvnw verify` no job `build-test` | Bloqueante: falha deve ser corrigida. |
| Gitleaks | Job termina com exit code da ferramenta | Bloqueante para leaks confirmados. Falso positivo deve ser justificado/remediado via configuracao. |
| SonarCloud | Falha se `SONAR_TOKEN` ausente ou analise falhar | Bloqueante quando se espera SonarCloud na entrega; se token nao estiver configurado, documentar limitacao operacional. |
| CodeQL | Upload para GitHub Code Scanning | Findings confirmados de alta severidade devem ser corrigidos antes de merge. |
| SpotBugs | Evidencia SAST em artefacto | Corrigir bugs confirmados; falso positivo documentado em triagem. |
| Dependency-Check | Evidence mode com `failBuildOnCVSS=11` e `continue-on-error` | Nao bloqueia automaticamente por CVSS; vulnerabilidades reais devem ser corrigidas ou justificadas em [SCA_TRIAGE.md](SCA_TRIAGE.md). |
| CycloneDX SBOM | Gera `bom.json`/`bom.xml` | Bloqueia apenas se a geracao tecnica falhar e a evidencia for necessaria. |
| Runtime tests no `dast-scan` | Maven tests seleccionados | Bloqueante para controlos runtime. |
| ZAP baseline | `continue-on-error` com `-I` | Evidencia/review; findings sao triados, nao bloqueiam automaticamente. |
| PIT | Workflow separado | Indicador de qualidade de testes; falha tecnica deve ser revista, mas nao substitui o gate rapido de PR. |

Interpretacao pratica:

- falha de compilacao, testes, coverage ou secrets confirmados impede merge;
- CVE exploravel em dependencia usada deve ser corrigido antes de aceitar o PR;
- CVE nao aplicavel, falso positivo SAST ou alerta ZAP informativo pode ser
  aceite com justificacao e link para evidencia;
- ausencia de secret operacional, como `SONAR_TOKEN`, deve ser tratada como
  limitacao de ambiente, nao como claim de seguranca implementado.

## 6. Job `build-test`

Automacoes:

- checkout;
- configuracao Java 17 com cache Maven;
- `./mvnw verify`;
- upload de Surefire reports;
- upload de JaCoCo;
- publicacao de sumario no GitHub Step Summary.

Valor de seguranca:

- impede regressao funcional;
- garante que testes de seguranca entram no gate normal;
- gera artefactos revistos pelo professor/equipa;
- JaCoCo evita que novas areas fiquem sem cobertura minima.

## 7. Job `security-secrets`

Automacoes:

- corre Gitleaks em Docker;
- usa `.gitleaks.toml`;
- gera `target/gitleaks/gitleaks-report.json`;
- publica artefacto `secret-scan-gitleaks-json`;
- escreve sumario de evidencia.

Mitigacao STRIDE:

- reduz Information Disclosure por secrets commitados;
- ajuda a detectar tokens/passwords/keys antes de merge.

## 8. Job `sast`

Automacoes:

- inicializa CodeQL para Java;
- compila o projecto para analise;
- corre SpotBugs;
- corre SonarCloud quando `SONAR_TOKEN` existe;
- executa CodeQL analyze;
- publica `sast-reports`.

Notas importantes:

- CodeQL envia resultados para GitHub Code Scanning;
- SpotBugs gera XML/site como evidencia;
- SonarCloud depende de secret configurado;
- SAST e evidencia complementar, nao prova ausencia de vulnerabilidades.

## 9. Job `dependency-scanning`

Automacoes:

- OWASP Dependency-Check `12.1.0`;
- formatos HTML/XML/JSON/SARIF;
- upload SARIF para GitHub Code Scanning quando gerado;
- CycloneDX `makeAggregateBom`;
- artefactos `dependency-check-sca-reports` e `sbom-cyclonedx`;
- sumario no Step Summary.

Evidencia recente:

- alertas Spring Security `6.5.10` foram corrigidos;
- Spring Boot BOM `3.5.15` resolve Spring Security `6.5.11`;
- SBOM permite listar componentes e suportar triagem futura.

## 10. Job `dast-scan`

Automacoes:

- corre testes runtime de seguranca seleccionados;
- empacota a aplicacao;
- arranca GhostReport em `localhost:8081`;
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

## 11. Workflow `pit-mutation-testing`

Automacoes:

- prepara Java 17 e cache Maven;
- compila testes;
- inventaria classes alvo;
- executa PIT com configuracao do `pom.xml`;
- valida existencia de `target/pit-reports/index.html`;
- gera sumario Markdown;
- publica `pit-mutation-testing-report`.

PIT fica separado porque e mais lento. Serve para avaliar qualidade dos testes e
nao para bloquear todos os commits rapidamente.

## 12. Artefactos esperados

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

Os artefactos ficam associados ao run de GitHub Actions. Artefactos temporarios
gerados em `target/` nao devem ser commitados no repositorio; a documentacao da
entrega referencia o tipo de evidencia e, quando util, inclui resumos Markdown.

## 13. Relacao com STRIDE

| STRIDE | Pipeline/automacao |
| --- | --- |
| Spoofing | Testes de auth/JWT/MFA no build; runtime probes em CI. |
| Tampering | Testes de backup/package integrity; SAST; dependency scanning. |
| Repudiation | Testes de audit logs; runtime logs arquivados. |
| Information Disclosure | Gitleaks, frontend XSS/data exposure tests, log leakage checks, ZAP baseline. |
| Denial of Service | Rate limiter tests, upload limits, ZAP baseline como sinal passivo. |
| Elevation of Privilege | RBAC/ownership tests no build; CodeQL/SpotBugs como apoio. |

## 14. Governacao e code review

As antigas notas separadas de branch protection, code review e coding standards
foram consolidadas aqui:

- branches curtas por tema;
- PR deve explicar impacto de seguranca;
- pelo menos uma revisao humana deve confirmar que a alteracao nao quebra RBAC,
  validacao, logging seguro ou claims ASVS;
- claims de relatorio precisam de evidencia;
- novas rotas exigem actualizacao da matriz;
- alteracoes em controlos exigem testes;
- artefactos gerados ficam em CI/`target`, nao no repo;
- secrets nunca sao commitados.

Critérios de triagem:

| Resultado | Tratamento |
| --- | --- |
| Vulnerabilidade confirmada em codigo proprio | Corrigir codigo e adicionar/actualizar teste. |
| CVE em dependencia directa/transitiva usada | Actualizar versao/BOM ou justificar mitigacao temporaria. |
| Falso positivo SCA/SAST | Documentar componente, regra/CVE, motivo e data de revisao. |
| ZAP baseline informativo | Avaliar impacto; corrigir se expuser controlos reais ou documentar como hardening futuro. |
| Evidencia incompleta por ambiente | Repetir workflow ou documentar limitação operacional sem transformar em claim. |

## 15. Limitacoes da pipeline

- Branch protection e configuracao do GitHub, nao totalmente provavel por ficheiros.
- SonarCloud depende de `SONAR_TOKEN`.
- ZAP baseline nao e DAST autenticado completo.
- IAST e runtime/academic substitute, nao agent-based.
- PIT pode ser demorado e por isso fica separado.
