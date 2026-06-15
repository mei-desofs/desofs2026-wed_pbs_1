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

Resultado observado:

| Modulo | Versao resolvida |
| --- | --- |
| `spring-security-config` | `6.5.11` |
| `spring-security-web` | `6.5.11` |
| `spring-security-core` | `6.5.11` |
| `spring-security-crypto` | `6.5.11` |
| `spring-security-test` | `6.5.11` |

## Alertas GitHub remediados

O code scanning reportava alertas Dependency-Check para
`org.springframework.security` `6.5.10`.

| CVE | Descricao resumida | Componentes/funcionalidade afectada | Aplicabilidade ao GhostReport | Decisao |
| --- | --- | --- | --- | --- |
| CVE-2026-40988 | Possivel denial of service por inflacao sem limite de payload SAML comprimido no service provider SAML2 com Redirect binding. | Spring Security SAML2 service provider em versoes incluindo `6.5.0` a `6.5.10`. | GhostReport nao usa SAML2, mas dependia de Spring Security `6.5.10` via BOM. | Remediado por actualizacao para Spring Security `6.5.11`. |
| CVE-2026-41694 | Payloads SAML podiam ser descriptografados sem assinatura valida, criando risco de decryption oracle. | Spring Security SAML. | GhostReport nao implementa SAML nem IdP/SP SAML. | Remediado por actualizacao para `6.5.11`; risco directo baixo no desenho actual. |
| CVE-2026-41003 | Saida HTML nao codificada em forms gerados por filtros Spring Security podia permitir XSS quando valores de `RelyingPartyRegistration` fossem influenciaveis. | Spring Security SAML/FormPost related flows. | GhostReport nao configura `RelyingPartyRegistration` nem SAML login; frontend e estatico. | Remediado por actualizacao para `6.5.11`; manter monitorizacao SCA. |

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
| Spring Security `6.5.10` | Remediado. |
| Spring Security `6.5.11` | Versao resolvida actualmente. |
| CVEs Spring Security listados acima | Sem risco residual conhecido no codigo actual apos upgrade. |
| Suppressions activas | Mantidas apenas para falso positivo/componente nao usado, com expiracao. |
| Accao futura | Reexecutar Dependency-Check antes da submissao final e apos qualquer alteracao em `pom.xml`. |
