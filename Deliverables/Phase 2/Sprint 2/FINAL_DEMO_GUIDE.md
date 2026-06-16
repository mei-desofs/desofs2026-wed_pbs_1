# Guião de demonstração final

## Preparação

A partir do módulo Spring Boot:

```powershell
cd ghostreport
.\mvnw.cmd test
.\mvnw.cmd "-Dspring-boot.run.profiles=dev" spring-boot:run
```

Abrir `http://localhost:8081`.

Numa base de dados dev nova, usar as contas internas criadas pelo perfil `dev`.
O login de `ADMIN`, `ANALYST` e `AUDITOR` exige MFA; quando a exposição dev está
activa, o código é escrito no log da aplicação apenas para demonstração local.
Com `ghostreport.seed-users.enabled=true`, o perfil `dev/test` mantém estas
contas demo alinhadas:

| Role | Username | Password |
|---|---|---|
| ADMIN | `admin` | `AdminPassword123!` |
| ANALYST | `analyst` | `AnalystPassword123!` |
| AUDITOR | `auditor` | `AuditorPassword123!` |

## Fluxo de demonstração

1. Abrir a página pública.
2. Submeter uma denúncia anónima.
3. Guardar o tracking code gerado.
4. Usar a página de tracking para verificar a denúncia; a página pública mostra
   estado, categoria e contagem de anexos, mas não nomes, IDs, caminhos,
   previews ou links de download.
5. Fazer upload de um ficheiro permitido.
6. Tentar um upload proibido ou nome com padrão de traversal e mostrar rejeição.
7. Fazer login como admin e completar MFA.
8. Mostrar gestão de utilizadores e visibilidade de auditoria/segurança.
9. Fazer login como analyst e mostrar lista de casos ou actualização permitida.
10. Fazer login como auditor e mostrar evidência de auditoria/segurança.
11. Mostrar verificação de backup ou pacote de evidência quando disponível.
12. Abrir o índice do Sprint 2 e ligar a demo à evidência documental.

## Fluxos internos finais

- `ANALYST`: assumir uma denúncia elegível; o caso passa para `UNDER_REVIEW`.
  Depois actualizar o estado para `RESOLVED` e confirmar que o estado persiste
  após actualizar a página. Uma transição directa inválida continua a ser
  rejeitada.
- `AUDITOR`: abrir Backups, listar backups existentes, verificar integridade e
  consultar manifesto. O auditor não deve ter botão nem permissão para criar ou
  restaurar backups.
- `ADMIN`: abrir Backups, criar backup, validar, descarregar e, se for
  demonstrado restore, preencher a password admin no campo dedicado. O restore
  é validado e extraído apenas para staging controlado; não sobrescreve a base
  de dados ou uploads live.

## Evidência a abrir

- [PHASE2_SPRINT2_REPORT.md](PHASE2_SPRINT2_REPORT.md)
- [AUTHORIZATION_MATRIX.md](AUTHORIZATION_MATRIX.md)
- [SECURITY_TESTING.md](SECURITY_TESTING.md)
- [DEVSECOPS_PIPELINE.md](DEVSECOPS_PIPELINE.md)
- [SCA_TRIAGE.md](SCA_TRIAGE.md)
- [IAST_RUNTIME_SECURITY.md](IAST_RUNTIME_SECURITY.md)
- [ASVS_EVIDENCE.md](ASVS_EVIDENCE.md)

## Pontos de apresentação

- Denúncia anónima continua pública; APIs internas exigem roles.
- MFA é exigido antes da emissão de JWT para admin, analyst e auditor.
- Analyst e auditor têm responsabilidades e acessos distintos.
- Upload controls mitigam ficheiros maliciosos e path traversal.
- Auditoria, alertas e verificação de backups apoiam accountability.
- A pipeline dá evidência de segurança, mas não elimina revisão nem hardening
  operacional.

## Não exagerar

- Não descrever evidência runtime como IAST completo.
- Não afirmar prontidão de produção sem TLS externo, secret management,
  migrações, logs centralizados e monitorização operacional.
- Não apresentar o MFA como produção-ready sem canal/IdP real de entrega de códigos.
