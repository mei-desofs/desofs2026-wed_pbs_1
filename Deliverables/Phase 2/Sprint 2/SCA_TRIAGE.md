# Triagem SCA e dependencias

## Estado actual

GhostReport usa o parent/BOM do Spring Boot para gerir versoes Spring Framework
e Spring Security:

```xml
<artifactId>spring-boot-starter-parent</artifactId>
<version>3.5.15</version>
```

Os modulos Spring Security resolvem actualmente para `6.5.11`.

Validacao local:

```powershell
cd ghostreport
.\mvnw.cmd dependency:tree "-Dincludes=org.springframework.security"
```

Resultado observado em 2026-06-16:

| Modulo | Versao resolvida |
| --- | --- |
| `spring-security-config` | `6.5.11` |
| `spring-security-web` | `6.5.11` |
| `spring-security-core` | `6.5.11` |
| `spring-security-crypto` | `6.5.11` |
| `spring-security-test` | `6.5.11` |

## Triagem dos alertas Spring Security

O code scanning reportava alertas Dependency-Check para
`org.springframework.security` `6.5.10`. A validacao local de 2026-06-16
confirma que a arvore Maven actual resolve Spring Security para `6.5.11` e que
`6.5.10` ja nao aparece no resultado filtrado de `dependency:tree`.

O estado remoto actual de GitHub Code Scanning/Dependabot Alerts nao e
comprovavel apenas pelo clone local. Deve ser confirmado na interface GitHub
antes de declarar que um alerta remoto esta fechado.

| Componente | Versao vulneravel | CVE | Severidade | Origem | Impacto | Estado | Decisao |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `org.springframework.security:spring-security-web` / stack Spring Security | `6.5.10` | CVE-2026-40988 | High | Dependency-Check / GitHub Code Scanning; advisory Spring/NVD | DoS em SAML2 service provider com Redirect binding por inflacao DEFLATE sem limite. GhostReport nao usa SAML2, mas a versao estava na arvore. | Remediado na arvore Maven local | Actualizado via Spring Boot BOM `3.5.15`; Spring Security resolvido para `6.5.11`. Confirmar fecho do alerta remoto no GitHub. |
| `org.springframework.security:spring-security-web` / stack Spring Security | `6.5.10` | CVE-2026-41694 | Low/Medium conforme fonte | Dependency-Check / GitHub Code Scanning; advisory Spring/NVD | Possivel decryption oracle em payloads SAML sem assinatura valida. Nao aplicavel directamente porque GhostReport nao configura SAML. | Remediado na arvore Maven local | Actualizado para `6.5.11`; sem suppression. Confirmar fecho do alerta remoto no GitHub. |
| `org.springframework.security:spring-security-web` / stack Spring Security | `6.5.10` | CVE-2026-41003 | High | Dependency-Check / GitHub Code Scanning; advisory Spring/NVD | XSS em HTML gerado por filtros Spring Security quando `RelyingPartyRegistration` e influenciavel. GhostReport nao usa SAML/OIDC relying party, mas a versao era afectada. | Remediado na arvore Maven local | Actualizado para `6.5.11`; frontend continua coberto por testes XSS. Confirmar fecho do alerta remoto no GitHub. |
| `org.hibernate.validator:hibernate-validator` | `8.0.3.Final` reportado | CVE-2025-15104 | Reportado por SCA; nao aplicavel ao componente usado | Dependency-Check local/configurado | O CVE refere The Nu Html Checker, nao Hibernate Validator. | Suprimido | Suppression especifica por package URL e CVE ate 2026-09-30; rever antes de producao. |
| `org.eclipse.angus:angus-activation` | `2.0.3` reportado | CVE-2025-7962 | Reportado por SCA; nao aplicavel ao componente usado | Dependency-Check local/configurado | O CVE refere SMTP injection em Jakarta Mail/Angus SMTP; GhostReport nao usa SMTP nem `org.eclipse.angus:smtp`. | Suprimido | Suppression especifica por package URL e CVE ate 2026-09-30; manter monitorizacao. |

A remediacao foi actualizar o BOM do Spring Boot para `3.5.15`, em vez de fixar
manualmente modulos Spring Security individuais. Isto mantem `spring-security-core`,
`spring-security-web`, `spring-security-config`, `spring-security-crypto` e
`spring-security-test` alinhados na mesma linha `6.5.11`.

## Porque corrigir mesmo sem SAML no projecto

Mesmo que a superficie SAML nao esteja activa no GhostReport, a triagem optou
por corrigir a versao porque:

- a ferramenta SCA sinalizava a dependencia vulneravel na arvore;
- manter uma versao afectada obrigaria a justificar risco residual em cada
  entrega;
- a actualizacao via BOM e de baixo risco e preserva compatibilidade entre
  modulos Spring;
- a aplicacao usa Spring Security em funcionalidades criticas como JWT, filtros,
  CSRF, headers e autorizacao;
- a remocao do alerta melhora a postura de supply chain sem depender de
  suppressions.

## Comandos de validacao

```powershell
cd ghostreport
.\mvnw.cmd dependency:tree
.\mvnw.cmd org.owasp:dependency-check-maven:12.1.0:check -Dformat=ALL -DossindexAnalyzerEnabled=false -DfailOnError=false -DfailBuildOnCVSS=11
.\mvnw.cmd -DskipTests org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom
```

A validacao local mais recente confirmou que `spring-security-core`,
`spring-security-web`, `spring-security-config`, `spring-security-crypto` e
`spring-security-test` resolvem para `6.5.11` e que `6.5.10` ja nao surge na
arvore de dependencias.

O Dependency-Check local executado em 2026-06-15 terminou com build success e
gerou HTML/XML/JSON/SARIF em `target/dependency-check-report`. O JSON confirmou
0 vulnerabilidades nao suprimidas e 2 vulnerabilidades suprimidas
documentadas abaixo. Esta revisao de 2026-06-16 nao reexecutou
Dependency-Check, porque o ambiente local nao tinha JDK/`javac`; a confirmacao
actual ficou limitada a `dependency:tree`.

O comando `.\mvnw.cmd dependency:tree` tambem confirmou:

- `org.hibernate.validator:hibernate-validator:8.0.3.Final` como dependencia de
  `spring-boot-starter-validation`;
- `org.eclipse.angus:angus-activation:2.0.3` como dependencia transitiva via
  `org.glassfish.jaxb:jaxb-core`;
- `org.postgresql:postgresql:42.7.11`;
- `org.apache.tomcat.embed:tomcat-embed-core:10.1.55`;
- `org.springframework:spring-webmvc:6.2.19`.

## Processo de triagem SCA

1. Confirmar o componente e versao exacta na arvore Maven.
2. Ler a descricao do CVE e o intervalo de versoes afectadas.
3. Verificar se a funcionalidade vulneravel esta activa no GhostReport.
4. Preferir actualizacao de versao/BOM quando existir versao corrigida
   compativel.
5. Usar suppression apenas para falso positivo ou componente nao aplicavel,
   sempre com data de expiracao.
6. Registar risco residual quando o CVE nao puder ser corrigido antes da
   entrega.
7. Gerar SBOM CycloneDX para facilitar auditoria futura.

## Suppressions

O ficheiro de suppressions esta limitado a pares componente/CVE especificos:

| Componente | CVE | Justificacao |
| --- | --- | --- |
| `org.hibernate.validator:hibernate-validator@8.0.3.Final` | CVE-2025-15104 | O CVE refere The Nu Html Checker, nao Hibernate Validator. |
| `org.eclipse.angus:angus-activation@2.0.3` | CVE-2025-7962 | O CVE refere SMTP injection em Jakarta Mail/Angus SMTP; GhostReport nao usa o componente SMTP. |

Suppressions nao devem esconder vulnerabilidades reais. Tem data de expiracao e
devem ser revistas antes de submissao final ou producao.

## Evidencia de pipeline

O job `dependency-scanning` corre OWASP Dependency-Check, carrega SARIF para Code
Scanning quando o ficheiro e gerado, e produz SBOM CycloneDX.

Artefactos esperados:

- `dependency-check-sca-reports`: relatorios HTML/XML/JSON/SARIF;
- GitHub Code Scanning: SARIF quando gerado;
- `sbom-cyclonedx`: `bom.json` e `bom.xml`.

## Estado final da triagem

| Item | Estado |
| --- | --- |
| Spring Security `6.5.10` | Ausente do `dependency:tree` actual; alerta remoto a confirmar no GitHub. |
| Spring Security `6.5.11` | Versao resolvida actualmente por `dependency:tree` em 2026-06-16. |
| CVEs Spring Security listados acima | Sem risco residual conhecido no codigo actual apos upgrade, sujeito a reconfirmacao dos alertas remotos no GitHub. |
| Dependency-Check 2026-06-15 | 0 vulnerabilidades nao suprimidas; 2 suppressions documentadas. |
| Suppressions activas | Mantidas apenas para falso positivo/componente nao usado, com expiracao. |
| Accao futura | Reexecutar Dependency-Check antes da submissao final e apos qualquer alteracao em `pom.xml`. |
