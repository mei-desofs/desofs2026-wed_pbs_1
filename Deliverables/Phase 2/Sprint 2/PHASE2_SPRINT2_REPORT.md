# GhostReport - Relatório Phase 2 Sprint 2

## 1. Âmbito e objectivo

GhostReport é uma plataforma segura de denúncia anónima com análise interna de
casos, evidência de auditoria e controlos administrativos. A Phase 2 Sprint 2
focou-se na qualidade da entrega final: evidência DevSecOps, testes de
segurança, configuração segura, triagem de dependências, validação em runtime e
documentação final.

O projecto mantém um âmbito académico. Este relatório separa controlos
implementados, controlos parciais e hardening futuro necessário para produção.

## 2. Relação com trabalho anterior

A Phase 1 definiu o problema de segurança: submissões anónimas, boundaries de
confiança, agregados DDD, modelação STRIDE, abuse cases e attack trees para
divulgação de identidade, abuso de códigos de tracking, uploads maliciosos,
path traversal e acesso não autorizado.

A Phase 2 Sprint 1 implementou os controlos base: autenticação JWT, RBAC,
submissão pública, validação, restrições de upload, eventos de auditoria,
alertas de segurança, integridade de backups e pacotes de evidência.

A Phase 2 Sprint 2 consolidou e validou a entrega final através de pipeline,
SCA/SAST/DAST, evidência runtime/IAST-like, instalação segura, avaliação de
configuração, rastreabilidade ASVS e revisão crítica.

## 3. Estado actual do sistema

GhostReport suporta:

- submissão anónima e verificação pública por código de tracking;
- upload de evidência com extensões permitidas, validação MIME/assinatura e
  protecção contra path traversal;
- autenticação JWT para utilizadores internos;
- MFA baseado em código para `ADMIN` antes da emissão de JWT;
- RBAC para `ADMIN`, `ANALYST` e `AUDITOR`;
- verificações de propriedade para actualizações de casos por analistas;
- logs de auditoria e alertas de segurança;
- geração de pacotes de evidência para casos fechados;
- criação, reposição e verificação de backups.

Não existe uma role geral `USER` activa no modelo actual. Linhas legadas `USER`
são tratadas como dados de migração, não como uma role com permissões.

## 4. Requisitos e modelo de domínio

| Requisito | Evidência no GhostReport |
| --- | --- |
| Backend web API | Controladores Spring Boot em `ghostreport/src/main/java/com/ghostreport/controller`. |
| Base de dados relacional | PostgreSQL em execução local/dev/prod-like; H2 apenas para testes. |
| Pelo menos três agregados DDD | `Report`, `CaseReview` e `User`; entidades de auditoria/segurança dão suporte adicional. |
| Pelo menos três roles | `ADMIN`, `ANALYST` e `AUDITOR`. |
| Funcionalidade de sistema operativo no backend | Armazenamento de uploads, criação de ZIPs de evidência e ficheiros de backup. |
| Evidência de entrega segura | GitHub Actions, SCA, SAST, DAST, SBOM e testes documentados nesta pasta. |

## 5. Arquitectura e stack

| Camada | Tecnologia |
| --- | --- |
| Backend | Java 17, Spring Boot `3.5.15`, Spring Security, Spring Data JPA |
| Segurança | JWT, BCrypt, Spring Security filter chain, Bean Validation |
| Dados | PostgreSQL em runtime, H2 em testes |
| Frontend | HTML, CSS e JavaScript estáticos servidos pelo Spring Boot |
| Build/testes | Maven wrapper, JUnit 5, MockMvc, JaCoCo |
| DevSecOps | GitHub Actions, CodeQL, SonarCloud, SpotBugs, OWASP Dependency-Check, CycloneDX, Gitleaks, OWASP ZAP |

As versões Spring Security são geridas pelo BOM do Spring Boot. A versão
resolvida actual é `6.5.11`, substituindo a linha vulnerável `6.5.10` reportada
anteriormente pelo dependency scanning.

## 6. Autenticação, MFA e sessões

A autenticação interna usa credenciais username/password, BCrypt e JWT
stateless. Utilizadores inactivos são bloqueados no login.

O MFA de admin é uma segunda etapa antes da emissão do JWT. Após password válida
de admin, é criado um desafio MFA curto e de utilização única; o JWT final só é
emitido depois da verificação do código. Este controlo está implementado apenas
para `ADMIN`. Estender MFA a todas as roles internas ou integrar TOTP/IdP fica
como hardening futuro.

O logout revoga tokens JWT no servidor através do fluxo de revogação
implementado.

## 7. Autorização

A autorização é centralizada no Spring Security e complementada por verificações
de propriedade nos serviços. Rotas públicas incluem submissão anónima,
verificação por tracking code, upload associado a denúncia válida e download por
código. Rotas internas são restringidas por role.

A matriz completa está em [AUTHORIZATION_MATRIX.md](AUTHORIZATION_MATRIX.md).

## 8. Validação e segurança de input

GhostReport usa DTOs e Bean Validation para evitar binding directo de entidades
e mass assignment. As áreas principais são:

- login, MFA e password reset;
- título, descrição, categoria e formato de tracking code;
- tipo, tamanho, extensão, MIME e assinatura de ficheiros;
- nomes de armazenamento gerados, não controlados pelo utilizador;
- path traversal e ZIP Slip em downloads, pacotes de evidência e backups;
- validação de estado, prioridade e notas de analistas.

A evidência está consolidada em [SECURITY_TESTING.md](SECURITY_TESTING.md).

## 9. Auditoria, alertas e integridade

O sistema regista eventos críticos: falhas de login, conclusão de MFA admin,
alterações a utilizadores, acesso a denúncias, actualizações de casos, geração
de pacotes de evidência e operações de backup. Registos de auditoria e alertas
incluem dados de correlação e integridade para apoiar verificação posterior.

## 10. Backups, reposição e pacotes de evidência

GhostReport gera backups ZIP com manifestos e verificações de integridade.
Backups podem ser listados, descarregados, verificados e repostos por rotas de
admin; auditores podem consultar evidência de integridade. Casos fechados podem
gerar pacotes de evidência.

A instalação segura está documentada em [SECURE_INSTALLATION.md](SECURE_INSTALLATION.md).

## 11. Pipeline DevSecOps

A pipeline está descrita em [DEVSECOPS_PIPELINE.md](DEVSECOPS_PIPELINE.md) e
cobre:

- build e testes com enforcement JaCoCo;
- Gitleaks para secrets;
- CodeQL, SonarCloud e SpotBugs;
- OWASP Dependency-Check e CycloneDX SBOM;
- testes runtime e OWASP ZAP baseline;
- publicação de artefactos.

PIT mutation testing corre num workflow separado por ser mais lento e usado como
evidência de qualidade, não como gate rápido.

## 12. SAST, DAST, IAST e SCA

| Área | Estado actual |
| --- | --- |
| SCA | Dependency-Check e CycloneDX configurados. Alertas Spring Security `6.5.10` remediados ao actualizar o BOM para resolver `6.5.11`. |
| SAST | CodeQL, SonarCloud e SpotBugs configurados na pipeline. |
| DAST | OWASP ZAP baseline contra a aplicação em execução. |
| IAST | Não existe agente/ferramenta IAST completa; há evidência runtime/IAST-like através de testes, requests runtime, logs e ZAP. |

Detalhes: [SCA_TRIAGE.md](SCA_TRIAGE.md),
[SPOTBUGS_TRIAGE.md](SPOTBUGS_TRIAGE.md) e
[IAST_RUNTIME_SECURITY.md](IAST_RUNTIME_SECURITY.md).

## 13. Testes automatizados

A validação local mais recente da linha actual correu `.\mvnw.cmd test` com
180 testes a passar e 0 falhas. As áreas cobertas incluem autenticação, MFA
admin, RBAC, propriedade de casos, fluxos anónimos, tracking code, uploads,
auditoria, backups, alertas de segurança e comportamento seleccionado de
frontend.

## 14. Instalação e configuração segura

Execução prod-like deve fornecer secrets por variáveis de ambiente ou secrets
da plataforma:

- `JWT_SECRET`;
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`;
- `BACKUP_HMAC_SECRET` e `BACKUP_HMAC_KEY_ID`;
- directórios de uploads, backups e evidência.

O perfil prod-like usa PostgreSQL e validação de schema. Ainda não há cadeia
Flyway/Liquibase, pelo que mudanças de schema devem seguir
[SECURE_INSTALLATION.md](SECURE_INSTALLATION.md) e
[SECURITY_CONFIGURATION_ASSESSMENT.md](SECURITY_CONFIGURATION_ASSESSMENT.md).

## 15. Rastreabilidade ASVS

A evidência ASVS está em [ASVS_EVIDENCE.md](ASVS_EVIDENCE.md). A spreadsheet de
Sprint 1 permanece evidência histórica; nesta branch não existe uma spreadsheet
Sprint 2 separada.

## 16. Avaliação final

A avaliação final está em [SECURITY_ASSESSMENT.md](SECURITY_ASSESSMENT.md).
Globalmente, o GhostReport implementa os controlos principais esperados:
autenticação interna, fluxos anónimos públicos, separação de roles, hardening de
uploads, auditabilidade, scanning de dependências e validação runtime.

## 17. Limitações e trabalho futuro

- MFA está implementado apenas para `ADMIN`.
- A evidência runtime é IAST-like; não há agente IAST completo.
- ZAP baseline não substitui um teste de penetração autenticado.
- Rate limiting é em memória e deve ser externalizado em produção multi-nó.
- TLS, WAF, SIEM, retenção imutável e secret manager são controlos operacionais.
- Migrações formais de base de dados devem ser adicionadas antes de produção.

## 18. Conclusão

A Phase 2 Sprint 2 transforma o GhostReport num protótipo académico de entrega
segura mais coerente, documentado e avaliável. A documentação final separa o que
está implementado, a evidência que o suporta e as limitações que permanecem.
