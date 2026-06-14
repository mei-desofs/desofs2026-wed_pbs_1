# Segurança em runtime e evidência IAST-like

GhostReport não integra uma ferramenta IAST comercial nem um agente de
instrumentação. A evidência do Sprint 2 é, por isso, descrita como segurança em
runtime e IAST-like: a aplicação arranca, fluxos sensíveis são exercitados,
logs/respostas são verificados e o OWASP ZAP baseline corre contra o serviço em
execução.

## O que está implementado

| Evidência | Descrição |
| --- | --- |
| Testes runtime | O job `dast-scan` executa testes de segurança antes de empacotar a aplicação. |
| Aplicação em execução | A CI inicia o GhostReport em `localhost:8081` e exercita endpoints. |
| Revisão de logs | O workflow verifica logs para evitar fuga de dados sensíveis. |
| ZAP baseline | OWASP ZAP gera artefactos HTML/XML/JSON. |
| Sumário | O workflow publica `iast-runtime-security-evidence` e evidência DAST. |

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

## Trabalho futuro

- Integrar agente IAST compatível com Java/Spring Boot.
- Adicionar contextos ZAP autenticados para admin, analyst e auditor.
- Arquivar relatório IAST formal como artefacto.
- Correlacionar findings runtime com controlos ASVS.
