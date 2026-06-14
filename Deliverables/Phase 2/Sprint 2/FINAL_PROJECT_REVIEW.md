# Revisão final do projecto

## Pontos fortes

- O domínio é claro: denúncia anónima com análise interna e evidência de
  auditoria.
- O modelo de roles é pequeno e revível: `ADMIN`, `ANALYST`, `AUDITOR` e
  denunciantes anónimos.
- Fluxos públicos e APIs internas protegidas estão separados.
- Uploads, path traversal e pacotes de evidência respondem a ameaças da Phase 1.
- Auditoria, alertas e integridade de backups dão evidência concreta além de
  CRUD simples.
- A pipeline inclui build/testes, SCA, SAST, DAST baseline, SBOM e secrets scan.

## Claims correctamente delimitados

| Claim | Redacção correcta |
| --- | --- |
| MFA | Implementado para `ADMIN` apenas. |
| IAST | Evidência runtime/IAST-like; sem agente IAST completo. |
| DAST | OWASP ZAP baseline, não teste de penetração autenticado completo. |
| Produção | Há orientação prod-like, mas faltam controlos operacionais externos. |
| ASVS | Mapa Markdown actual; spreadsheet Sprint 1 é histórica. |

## Limitações principais

- MFA deve ser alargado ou delegado a um IdP para todas as roles internas antes
  de produção.
- Migrações formais devem substituir evolução ad hoc de schema.
- Secrets devem ser guardados e rodados por serviço gerido.
- Auditoria e alertas devem ser exportados para armazenamento centralizado e
  imutável.
- Rate limiting deve usar mecanismo externo/distribuído.
- DAST autenticado e IAST completo devem ser adicionados para maior garantia.

## Prontidão final

Para a entrega DESOFS, o projecto está pronto para avaliação como protótipo de
engenharia de software segura, com evidência documentada e limitações
explícitas. Não é apresentado como sistema de produção sem os controlos
operacionais listados acima.
