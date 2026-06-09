# Security Testing & Pipeline Hardening

Este documento descreve os testes de seguranca, scanners DevSecOps, criterios de qualidade e artifacts esperados para o projeto GhostReport.

## Objetivo

Garantir que o backend Spring Boot e a pipeline executam controlos minimos de seguranca antes de aceitar alteracoes:

- testes JUnit e MockMvc;
- cobertura JaCoCo com thresholds minimos;
- SAST com SpotBugs;
- SCA com OWASP Dependency-Check;
- secret scanning com Gitleaks;
- DAST baseline com OWASP ZAP;
- publicacao de artifacts dos relatórios.

## Testes Automatizados

### JUnit e MockMvc

Comando local:

```bash
cd ghostreport
./mvnw test
```

Estado esperado: `./mvnw test` deve passar antes de uma alteracao ser aceite.
O workflow publica os relatorios Surefire como evidence artifact.

Os testes cobrem fluxos de:

- autenticacao e JWT;
- rate limiting;
- RBAC;
- ownership de casos por analyst;
- auditor authorization;
- security headers;
- backup security;
- validacao de ficheiros e tracking codes;
- tratamento de erros seguro.

## Cobertura JaCoCo

Comando local com gate:

```bash
cd ghostreport
./mvnw verify
```

Relatorios gerados:

- `ghostreport/target/site/jacoco/index.html`
- `ghostreport/target/site/jacoco/jacoco.xml`
- `ghostreport/target/site/jacoco/jacoco.csv`
- `ghostreport/target/jacoco.exec`

Thresholds minimos configurados:

| Metrica | Minimo |
| ------- | ------ |
| Instruction coverage | 70% |
| Line coverage | 70% |
| Branch coverage | 50% |

Cobertura observada localmente antes da configuracao do gate:

| Metrica | Valor |
| ------- | ----- |
| Instruction coverage | 76.95% |
| Line coverage | 75.24% |
| Branch coverage | 58.95% |

Impacto de seguranca: o gate reduz o risco de regressões em fluxos de autenticacao, autorizacao e validacao, obrigando a manter testes minimos para codigo novo.

## SpotBugs SAST

Workflow/job:

- `.github/workflows/dev.yml`, job `sast / SonarCloud SAST Scan`

Comando local:

```bash
cd ghostreport
./mvnw -DskipTests compile com.github.spotbugs:spotbugs-maven-plugin:4.8.6.6:spotbugs -Dspotbugs.xmlOutput=true
```

Artifact esperado:

- `ghostreport/target/spotbugsXml.xml`
- `ghostreport/target/*spotbugs*.xml`
- `ghostreport/target/site/spotbugs*`

Politica: SpotBugs gera evidencia para triagem manual no job SAST; findings
confirmados devem ser corrigidos ou documentados antes de serem usados como
evidencia ASVS.

## OWASP Dependency-Check SCA

Workflow/job:

- `.github/workflows/dev.yml`, job `dependency-scanning / Dependency Vulnerability Scanning`

Comando local:

```bash
cd ghostreport
./mvnw org.owasp:dependency-check-maven:12.1.0:check -Dformat=ALL -DossindexAnalyzerEnabled=false -DfailOnError=false -DfailBuildOnCVSS=11
```

Artifacts esperados:

- `ghostreport/target/dependency-check-report/dependency-check-report.html`
- `ghostreport/target/dependency-check-report/dependency-check-report.xml`
- `ghostreport/target/dependency-check-report/dependency-check-report.json`
- `ghostreport/target/dependency-check-report/dependency-check-report.sarif`

Politica:

- Dependency-Check corre em modo de evidencia/triagem, sem baixar o gate para ocultar findings;
- CVSS baixo, medio, alto ou critico deve ser triado;
- falso positivo deve ser documentado com justificacao;
- dependencias vulneraveis devem ser atualizadas quando houver versao segura compativel.

## Gitleaks Secret Scanning

Workflow/job:

- `.github/workflows/dev.yml`, job `security-secrets / secrets`

Configuracao:

- `.gitleaks.toml`

Artifact esperado:

- `ghostreport/target/gitleaks/gitleaks-report.json`

Politica:

- qualquer secret real identificado deve ser removido;
- secrets expostos devem ser rodados;
- tokens e chaves nunca devem ser colocados em `application.yaml`, workflows ou documentacao.

## OWASP ZAP DAST

Workflow/job:

- `.github/workflows/dev.yml`, job `dast-scan / dast-scan`

Target:

- `http://localhost:8081`

Artifacts esperados:

- `ghostreport/target/zap-reports/zap-baseline.html`
- `ghostreport/target/zap-reports/zap-baseline.xml`
- `ghostreport/target/zap-reports/zap-baseline.json`
- `ghostreport/target/ghostreport-dast-app.log`

Politica:

- ZAP baseline corre em modo de evidencia/triagem;
- alertas medios/altos devem ser triados;
- alertas baixos devem ser corrigidos quando simples;
- findings aceites devem ter justificacao.

## Security Headers

Headers verificados por teste automatizado:

- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy: no-referrer`
- `Content-Security-Policy` com `default-src 'self'`
- `Cache-Control` seguro em respostas publicas.

Impacto de seguranca:

- reduz XSS;
- reduz clickjacking;
- reduz MIME sniffing;
- limita leakage de URL/referrer.

## Artifacts da Pipeline

Os workflows publicam:

| Job | Artifacts |
| -------- | --------- |
| `build-test / build-and-test` | Surefire reports, JaCoCo report, PIT report/summary |
| `security-secrets / secrets` | Gitleaks JSON report |
| `sast / SonarCloud SAST Scan` | SpotBugs XML, SAST summary, CodeQL/SonarCloud evidence |
| `dependency-scanning / Dependency Vulnerability Scanning` | Dependency-Check HTML/XML/JSON/SARIF, CycloneDX SBOM |
| `dast-scan / dast-scan` | Runtime security/IAST readiness notes, ZAP HTML/XML/JSON, application log |

Os artifacts devem ser descarregados do GitHub Actions e guardados como evidencia da sprint quando solicitado.

## Processo de Triagem

Para cada finding:

1. Identificar ferramenta, severidade e ficheiro/endpoint afetado.
2. Classificar como `Corrigido`, `Falso positivo`, `Aceite temporariamente` ou `Pendente`.
3. Corrigir primeiro findings que causem exposure de secrets, bypass de autenticacao/autorizacao, XSS, SQL injection ou dependencia critica.
4. Reexecutar o scanner.
5. Guardar relatorio e screenshot/output da pipeline.

## Proximos Passos Recomendados

1. Reexecutar todas as pipelines na branch de trabalho.
2. Guardar screenshots dos runs verdes.
3. Descarregar artifacts HTML/XML/JSON/SARIF.
4. Triar findings restantes dos scanners.
5. Aumentar gradualmente branch coverage acima de 60%.
6. Subir thresholds JaCoCo quando a cobertura estiver estabilizada.
