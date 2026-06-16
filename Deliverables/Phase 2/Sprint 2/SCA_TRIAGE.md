# Triagem SCA e dependências

## Estado atual

O GhostReport usa o parent/BOM do Spring Boot para gerir versões do Spring Framework e do Spring Security:

```xml
<artifactId>spring-boot-starter-parent</artifactId>
<version>3.5.15</version>
```

Os módulos Spring Security resolvem atualmente para `6.5.11`.

Validação local:

```powershell
cd ghostreport
.\mvnw.cmd dependency:tree "-Dincludes=org.springframework.security"
```

Resultado observado:

| Módulo                   | Versão resolvida |
| ------------------------ | ---------------- |
| `spring-security-config` | `6.5.11`         |
| `spring-security-web`    | `6.5.11`         |
| `spring-security-core`   | `6.5.11`         |
| `spring-security-crypto` | `6.5.11`         |
| `spring-security-test`   | `6.5.11`         |

## Alertas GitHub remediados

O Code Scanning reportava alertas do Dependency-Check para `org.springframework.security` `6.5.10`.

| Componente                                                                 | Versão vulnerável       | CVE            | Severidade                                           | Origem                                                       | Impacto                                                                                                                                                                       | Estado    | Decisão                                                                                  |
| -------------------------------------------------------------------------- | ----------------------- | -------------- | ---------------------------------------------------- | ------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------- | ---------------------------------------------------------------------------------------- |
| `org.springframework.security:spring-security-web` / stack Spring Security | `6.5.10`                | CVE-2026-40988 | High                                                 | Dependency-Check / GitHub Code Scanning; advisory Spring/NVD | DoS em SAML2 Service Provider com Redirect Binding devido à inflação DEFLATE sem limite. O GhostReport não usa SAML2, mas a versão estava na árvore.                          | Corrigido | Atualizado através do Spring Boot BOM `3.5.15`; Spring Security resolvido para `6.5.11`. |
| `org.springframework.security:spring-security-web` / stack Spring Security | `6.5.10`                | CVE-2026-41694 | Low/Medium, conforme a fonte                         | Dependency-Check / GitHub Code Scanning; advisory Spring/NVD | Possível decryption oracle em payloads SAML sem assinatura válida. Não aplicável diretamente porque o GhostReport não configura SAML.                                         | Corrigido | Atualizado para `6.5.11`; sem suppression.                                               |
| `org.springframework.security:spring-security-web` / stack Spring Security | `6.5.10`                | CVE-2026-41003 | High                                                 | Dependency-Check / GitHub Code Scanning; advisory Spring/NVD | XSS em HTML gerado por filtros do Spring Security quando `RelyingPartyRegistration` é influenciável. O GhostReport não usa SAML/OIDC Relying Party, mas a versão era afetada. | Corrigido | Atualizado para `6.5.11`; o frontend continua coberto por testes XSS.                    |
| `org.hibernate.validator:hibernate-validator`                              | `8.0.3.Final` reportado | CVE-2025-15104 | Reportado por SCA; não aplicável ao componente usado | Dependency-Check local/configurado                           | O CVE refere-se ao The Nu Html Checker, não ao Hibernate Validator.                                                                                                           | Suprimido | Suppression específica por Package URL e CVE até 2026-09-30; rever antes da produção.    |
| `org.eclipse.angus:angus-activation`                                       | `2.0.3` reportado       | CVE-2025-7962  | Reportado por SCA; não aplicável ao componente usado | Dependency-Check local/configurado                           | O CVE refere SMTP injection em Jakarta Mail/Angus SMTP; o GhostReport não usa SMTP nem `org.eclipse.angus:smtp`.                                                              | Suprimido | Suppression específica por Package URL e CVE até 2026-09-30; manter monitorização.       |

A remediação consistiu em atualizar o BOM do Spring Boot para `3.5.15`, em vez de fixar manualmente módulos individuais do Spring Security. Isto mantém `spring-security-core`, `spring-security-web`, `spring-security-config`, `spring-security-crypto` e `spring-security-test` alinhados na mesma versão (`6.5.11`).

## Porque corrigir mesmo sem SAML no projeto

Mesmo que a superfície SAML não esteja ativa no GhostReport, a triagem optou por corrigir a versão porque:

* a ferramenta SCA sinalizava a dependência vulnerável na árvore;
* manter uma versão afetada obrigaria a justificar risco residual em cada entrega;
* a atualização através do BOM é de baixo risco e preserva a compatibilidade entre módulos Spring;
* a aplicação usa Spring Security em funcionalidades críticas como JWT, filtros, CSRF, headers e autorização;
* a remoção do alerta melhora a postura de supply chain sem depender de suppressions.

## Comandos de validação

```powershell
cd ghostreport
.\mvnw.cmd dependency:tree
.\mvnw.cmd org.owasp:dependency-check-maven:12.1.0:check -Dformat=ALL -DossindexAnalyzerEnabled=false -DfailOnError=false -DfailBuildOnCVSS=11
.\mvnw.cmd -DskipTests org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom
```

A validação local mais recente confirmou que `spring-security-core`, `spring-security-web`, `spring-security-config`, `spring-security-crypto` e `spring-security-test` resolvem para `6.5.11` e que a versão `6.5.10` já não surge na árvore de dependências.

O Dependency-Check local executado em 2026-06-15 terminou com **build success** e gerou relatórios HTML/XML/JSON/SARIF em `target/dependency-check-report`. O JSON confirmou **0 vulnerabilidades não suprimidas** e **2 vulnerabilidades suprimidas**, documentadas abaixo.

O comando `.\mvnw.cmd dependency:tree` também confirmou:

* `org.hibernate.validator:hibernate-validator:8.0.3.Final` como dependência de `spring-boot-starter-validation`;
* `org.eclipse.angus:angus-activation:2.0.3` como dependência transitiva via `org.glassfish.jaxb:jaxb-core`;
* `org.postgresql:postgresql:42.7.11`;
* `org.apache.tomcat.embed:tomcat-embed-core:10.1.55`;
* `org.springframework:spring-webmvc:6.2.19`.

## Processo de triagem SCA

1. Confirmar o componente e a versão exata na árvore Maven.
2. Ler a descrição do CVE e o intervalo de versões afetadas.
3. Verificar se a funcionalidade vulnerável está ativa no GhostReport.
4. Preferir a atualização da versão/BOM quando existir uma versão corrigida compatível.
5. Usar suppression apenas para falso positivo ou componente não aplicável, sempre com data de expiração.
6. Registar risco residual quando o CVE não puder ser corrigido antes da entrega.
7. Gerar SBOM CycloneDX para facilitar auditorias futuras.

## Suppressions

O ficheiro de suppressions está limitado a pares componente/CVE específicos:

| Componente                                                | CVE            | Justificação                                                                                     |
| --------------------------------------------------------- | -------------- | ------------------------------------------------------------------------------------------------ |
| `org.hibernate.validator:hibernate-validator@8.0.3.Final` | CVE-2025-15104 | O CVE refere-se ao The Nu Html Checker, não ao Hibernate Validator.                              |
| `org.eclipse.angus:angus-activation@2.0.3`                | CVE-2025-7962  | O CVE refere SMTP injection em Jakarta Mail/Angus SMTP; o GhostReport não usa o componente SMTP. |

As suppressions não devem esconder vulnerabilidades reais. Têm data de expiração e devem ser revistas antes da submissão final ou da produção.

## Evidência de pipeline

O job `dependency-scanning` executa o OWASP Dependency-Check, carrega o SARIF para o GitHub Code Scanning quando o ficheiro é gerado e produz a SBOM CycloneDX.

Artefactos esperados:

* `dependency-check-sca-reports`: relatórios HTML/XML/JSON/SARIF;
* GitHub Code Scanning: SARIF quando gerado;
* `sbom-cyclonedx`: `bom.json` e `bom.xml`.

## Estado final da triagem

| Item                                | Estado                                                                                         |
| ----------------------------------- | ---------------------------------------------------------------------------------------------- |
| Spring Security `6.5.10`            | Remediado.                                                                                     |
| Spring Security `6.5.11`            | Versão resolvida atualmente.                                                                   |
| CVEs Spring Security listados acima | Sem risco residual conhecido no código atual após o upgrade.                                   |
| Dependency-Check 2026-06-15         | 0 vulnerabilidades não suprimidas; 2 suppressions documentadas.                                |
| Suppressions ativas                 | Mantidas apenas para falso positivo/componente não usado, com expiração.                       |
| Ação futura                         | Reexecutar o Dependency-Check antes da submissão final e após qualquer alteração ao `pom.xml`. |
