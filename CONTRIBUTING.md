# Contribuição no GhostReport

Este guia mantém código, documentação, evidência ASVS e relatórios de entrega
alinhados enquanto várias pessoas trabalham em paralelo.

## Branches

Usar branches curtas e com âmbito claro:

```text
feature/admin-security-hardening
feature/devsecops-report-hardening
fix/upload-validation
docs/finalize-phase2-sprint2-documentation
```

Evitar misturar alterações de backend de segurança com documentação ou pipeline,
excepto quando as alterações estiverem directamente relacionadas.

## Commits

Usar commits focados:

```text
ci: clarify DevSecOps workflows
docs: update Sprint 2 security report
test: cover admin authorization
security: add login rate limiting
```

Cada commit deve compilar ou, no mínimo, deixar a área alterada compreensível.
Não commitar backups locais gerados, texto extraído de PDFs, ficheiros de base
de dados local, metadados de IDE ou resultados de ferramentas em `target/`.

## Pull requests

Cada pull request deve indicar:

- o que mudou;
- por que mudou;
- como foi testado;
- artefactos ou screenshots de evidência, quando aplicável;
- controlos ASVS afectados, se aplicável.

Para branches de DevSecOps ou documentação, incluir também:

- workflows alterados;
- artefactos esperados;
- se o workflow é gate de merge ou recolha de evidência;
- documentação actualizada para corresponder ao comportamento da pipeline.

Antes de pedir revisão, correr o comando relevante:

```powershell
cd ghostreport
.\mvnw.cmd test
```

Para alterações de pipeline, verificar indentação YAML e caminhos dos
artefactos.

## Checklist de revisão

- A implementação corresponde ao texto do relatório?
- As roles dos endpoints estão alinhadas com `SecurityConfig`?
- Os erros são suficientemente genéricos para não revelar detalhes internos?
- Novas afirmações de segurança têm suporte em testes, workflows ou artefactos?
- As limitações estão documentadas sem exagerar o que está implementado?
- Os links do `README.md` e do relatório Sprint 2 continuam válidos?

## Regras de documentação

Quando um controlo de segurança é criado ou alterado, actualizar o ficheiro
relevante da entrega em `Deliverables/Phase 2/Sprint 2/`, sobretudo:

- `PHASE2_SPRINT2_REPORT.md`
- `AUTHORIZATION_MATRIX.md`
- `SECURITY_TESTING.md`
- `DEVSECOPS_PIPELINE.md`
- `SCA_TRIAGE.md`
- `SPOTBUGS_TRIAGE.md`
- `SECURITY_ASSESSMENT.md`
- `SECURITY_CONFIGURATION_ASSESSMENT.md`
- `SECURE_INSTALLATION.md`
- `ASVS_EVIDENCE.md`

O relatório deve descrever apenas o que está implementado, validado ou suportado
por evidência do projecto.
