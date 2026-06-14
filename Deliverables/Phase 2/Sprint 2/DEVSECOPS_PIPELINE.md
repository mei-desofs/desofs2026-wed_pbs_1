# Pipeline DevSecOps

## Visão geral

GhostReport usa GitHub Actions para build, testes, scanning de dependências,
SAST, DAST, evidência runtime, SBOM e publicação de artefactos.

## Workflows

| Workflow | Objectivo |
| --- | --- |
| `.github/workflows/dev.yml` | Workflow principal de build, testes e evidência de segurança. |
| `.github/workflows/pit.yml` | Workflow dedicado a PIT mutation testing. |

## Jobs principais

| Job | Evidência |
| --- | --- |
| `build-test` | Maven `verify`, Surefire e JaCoCo. |
| `security-secrets` | Gitleaks e artefacto JSON. |
| `sast` | CodeQL, SpotBugs e SonarCloud. |
| `dependency-scanning` | OWASP Dependency-Check, SARIF e CycloneDX SBOM. |
| `dast-scan` | Runtime checks e OWASP ZAP baseline. |

PIT corre separado porque é mais lento e serve como evidência de qualidade, não
como gate rápido da pipeline principal.

## Revisão de código e governação

Notas antigas de branch protection, code review e coding standards foram
consolidadas aqui.

### Expectativas de pull request

- Branches com âmbito claro e commits focados.
- Explicação do impacto de segurança.
- Comandos de teste e artefactos de evidência.
- Actualização de ASVS quando um controlo muda.
- Evitar claims não suportados por código, testes ou pipeline.

### Checklist de revisão

| Área | Pergunta |
| --- | --- |
| Autenticação | JWT, logout e MFA admin continuam correctos? |
| Autorização | Roles estão alinhadas com `SecurityConfig` e serviços? |
| Validação | DTOs e Bean Validation são usados? |
| Filesystem | Caminhos são canónicos e nomes de ficheiro são gerados? |
| Evidência | Auditoria, alertas e testes foram actualizados? |
| Documentação | O relatório Sprint 2 corresponde à implementação? |

### Standards de código

- Preferir serviços/controladores pequenos e com responsabilidade clara.
- Centralizar decisões de segurança quando possível.
- Usar DTOs para input externo.
- Manter mensagens de erro genéricas em autenticação e fluxos públicos.
- Não commitar secrets, relatórios gerados, backups locais ou `target/`.

## Política de artefactos

Relatórios gerados devem ficar como artefactos de pipeline ou em `target/`
local. Só devem ser commitados se a entrega exigir um ficheiro estático.

## Limitações

- Branch protection é configuração do repositório e não é totalmente provada por
  ficheiros.
- SonarCloud exige secrets configurados.
- ZAP baseline não é DAST autenticado completo.
- PIT pode demorar mais do que a pipeline principal.
