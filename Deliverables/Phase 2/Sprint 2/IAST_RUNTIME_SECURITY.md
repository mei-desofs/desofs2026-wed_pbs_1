# Segurança em runtime e evidência IAST-like

GhostReport não integra uma ferramenta IAST comercial nem um agente de
instrumentação. A evidência do Sprint 2 é, por isso, descrita como segurança em
runtime e IAST-like: a aplicação arranca, fluxos sensíveis são exercitados,
logs/respostas são verificados e o OWASP ZAP baseline corre contra o serviço em
execução.

## O que está implementado

| Evidência | Descrição |
| --- | --- |
| Testes runtime | O job `dast-scan` executa testes de segurança antes de empacotar a aplicação, incluindo report flow, uploads, RBAC, MFA, auditoria e backups. |
| Aplicação em execução | A CI inicia o GhostReport em `localhost:8081` e exercita endpoints reais. |
| Probes live | A CI cria denúncia, valida tracking code, tenta inputs inválidos/perigosos, testa uploads, completa MFA dev e valida endpoints protegidos com tokens reais por role. |
| Revisão de logs | O workflow verifica logs para evitar fuga de passwords, bearer tokens, secrets e stack traces. |
| ZAP baseline | OWASP ZAP gera artefactos HTML/XML/JSON em modo baseline/passivo. |
| Sumário | O workflow publica `iast-runtime-security-evidence` e evidência DAST. |

## Artefactos documentados

- [iast-runtime-evidence.md](iast-runtime-evidence.md)
- [runtime-endpoints.md](runtime-endpoints.md)
- [runtime-log-sanitization.md](runtime-log-sanitization.md)

## Porque não é IAST completo

IAST completo exige normalmente instrumentação da aplicação para observar fluxo
de dados dentro do runtime durante os testes. Isso não existe neste repositório.
O projecto evita, por isso, afirmar cobertura IAST completa.

## Riscos cobertos

- endpoints protegidos exigem roles esperadas;
- fluxos anónimos continuam públicos;
- MFA é exigido antes da emissão de JWT para roles internas configuradas;
- upload e validação de caminhos são exercitados;
- headers e respostas genéricas são avaliados por requests runtime e ZAP;
- logs são revistos para reduzir fuga de evidência sensível.

## Cobertura reforçada no `dast-scan`

O job cobre, por testes e probes:

- `POST /reports` válido, inválido, com caracteres perigosos e tentativa de mass assignment;
- `POST /reports/verify` com tracking code válido, inválido e tentativas repetidas;
- uploads permitidos, extensão proibida, conteúdo suspeito e filename com traversal;
- endpoint admin sem token e com JWT inválido;
- login real com MFA para admin, analyst e auditor usando o código dev exposto no log da CI;
- RBAC live com tokens reais para `/admin/**`, `/analyst/**` e `/audit/**`;
- JWT expirado, backups, ZIP Slip e tamanho máximo através da suite runtime-focused.

## Trabalho futuro

- Integrar agente IAST compatível com Java/Spring Boot.
- Adicionar contextos ZAP autenticados para admin, analyst e auditor.
- Arquivar relatório IAST formal como artefacto.
- Correlacionar findings runtime com controlos ASVS.
