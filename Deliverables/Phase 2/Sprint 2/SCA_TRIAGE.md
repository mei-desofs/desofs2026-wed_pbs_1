# Triagem SCA e dependências

## Estado actual

GhostReport usa o parent/BOM do Spring Boot para gerir versões Spring Framework
e Spring Security:

```xml
<artifactId>spring-boot-starter-parent</artifactId>
<version>3.5.15</version>
```

Os módulos Spring Security resolvem actualmente para `6.5.11`.

## Alertas GitHub remediados

O code scanning reportava alertas dependency-check para
`org.springframework.security` `6.5.10`:

| CVE | Módulos afectados | Severidade |
| --- | --- | --- |
| CVE-2026-40988 | `spring-security-core`, `spring-security-web` | High |
| CVE-2026-41694 | `spring-security-core`, `spring-security-web` | Medium |
| CVE-2026-41003 | `spring-security-core`, `spring-security-web` | Medium |

A remediação foi actualizar o BOM do Spring Boot, em vez de fixar manualmente
módulos Spring Security individuais. Isto mantém o conjunto de módulos alinhado.

## Comandos de validação

```powershell
cd ghostreport
.\mvnw.cmd dependency:tree
.\mvnw.cmd org.owasp:dependency-check-maven:12.1.0:check -Dformat=ALL -DossindexAnalyzerEnabled=false -DfailOnError=false -DfailBuildOnCVSS=11
.\mvnw.cmd -DskipTests org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom
```

A validação local mais recente confirmou que `spring-security-core`,
`spring-security-web` e `spring-security-config` resolvem para `6.5.11` e que
`6.5.10` já não surge na árvore de dependências.

## Suppressions

O ficheiro de suppressions está limitado a pares componente/CVE específicos:

| Componente | CVE | Justificação |
| --- | --- | --- |
| `org.hibernate.validator:hibernate-validator@8.0.3.Final` | CVE-2025-15104 | O CVE refere The Nu Html Checker, não Hibernate Validator. |
| `org.eclipse.angus:angus-activation@2.0.3` | CVE-2025-7962 | O CVE refere SMTP injection em Jakarta Mail/Angus SMTP; GhostReport não usa o componente SMTP. |

Suppressions não devem esconder vulnerabilidades reais. Têm data de expiração e
devem ser revistas antes de submissão final ou produção.

## Evidência de pipeline

O job `dependency-scanning` corre OWASP Dependency-Check, carrega SARIF para code
scanning e gera SBOM CycloneDX.
