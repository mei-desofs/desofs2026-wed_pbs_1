# Pipeline DevSecOps e automacoes

## 1. Objectivo

A pipeline DevSecOps do GhostReport transforma a entrega num processo
reprodutivel: compila, testa, mede cobertura, procura secrets, analisa codigo,
verifica dependencias, gera SBOM, arranca a aplicacao, executa checks runtime,
corre ZAP baseline e publica artefactos.

## 2. Workflows existentes

| Workflow | Triggers | Objectivo |
| --- | --- | --- |
| `.github/workflows/dev.yml` | `push`, `pull_request`, `workflow_dispatch` | Pipeline principal de build, testes e seguranca. |
| `.github/workflows/pit.yml` | `workflow_dispatch`, PRs e alteracoes relevantes em `main` | Mutation testing completo com PIT. |

O workflow principal usa `concurrency` para cancelar execucoes anteriores da
mesma ref, evitando consumir runner em builds obsoletas.

## 3. Job `build-test`

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

## 4. Job `security-secrets`

Automacoes:

- corre Gitleaks em Docker;
- usa `.gitleaks.toml`;
- gera `target/gitleaks/gitleaks-report.json`;
- publica artefacto `secret-scan-gitleaks-json`;
- escreve sumario de evidencia.

Mitigacao STRIDE:

- reduz Information Disclosure por secrets commitados;
- ajuda a detectar tokens/passwords/keys antes de merge.

## 5. Job `sast`

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

## 6. Job `dependency-scanning`

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

## 7. Job `dast-scan`

Automacoes:

- corre testes runtime de seguranca seleccionados;
- empacota a aplicacao;
- arranca GhostReport em `localhost:8081`;
- espera readiness;
- exercita endpoints runtime;
- verifica logs para fuga de dados sensiveis;
- corre OWASP ZAP baseline;
- prepara sumario IAST-like/runtime;
- publica `iast-runtime-security-evidence`;
- publica `dast-zap-baseline-reports`;
- para o processo no fim.

O ZAP baseline e evidencia passiva/nao autenticada. A parte runtime cobre mais
do que o ZAP sozinho, porque combina testes, app real, HTTP probes e logs.

## 8. Workflow `pit-mutation-testing`

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

## 9. Artefactos esperados

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

## 10. Relacao com STRIDE

| STRIDE | Pipeline/automacao |
| --- | --- |
| Spoofing | Testes de auth/JWT/MFA no build; runtime probes em CI. |
| Tampering | Testes de backup/package integrity; SAST; dependency scanning. |
| Repudiation | Testes de audit logs; runtime logs arquivados. |
| Information Disclosure | Gitleaks, frontend XSS/data exposure tests, log leakage checks, ZAP baseline. |
| Denial of Service | Rate limiter tests, upload limits, ZAP baseline como sinal passivo. |
| Elevation of Privilege | RBAC/ownership tests no build; CodeQL/SpotBugs como apoio. |

## 11. Governacao e code review

As antigas notas separadas de branch protection, code review e coding standards
foram consolidadas aqui:

- branches curtas por tema;
- PR deve explicar impacto de seguranca;
- claims de relatorio precisam de evidencia;
- novas rotas exigem actualizacao da matriz;
- alteracoes em controlos exigem testes;
- artefactos gerados ficam em CI/`target`, nao no repo;
- secrets nunca sao commitados.

## 12. Limitacoes da pipeline

- Branch protection e configuracao do GitHub, nao totalmente provavel por ficheiros.
- SonarCloud depende de `SONAR_TOKEN`.
- ZAP baseline nao e DAST autenticado completo.
- IAST e runtime/academic substitute, nao agent-based.
- PIT pode ser demorado e por isso fica separado.
