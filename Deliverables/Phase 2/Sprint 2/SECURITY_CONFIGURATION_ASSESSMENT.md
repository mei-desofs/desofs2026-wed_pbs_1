# Avaliação de configuração de segurança

## Sumário

A configuração actual é adequada para validação académica local e baseline
prod-like, desde que secrets e PostgreSQL sejam fornecidos externamente. Alguns
controlos continuam a exigir hardening operacional antes de produção real.

## Revisão de configuração

| Área | Configuração actual | Avaliação |
| --- | --- | --- |
| Spring Security | Regras centralizadas, filtro JWT e headers de segurança. | Implementado. |
| Autenticação | BCrypt, JWT e MFA para `ADMIN`, `ANALYST` e `AUDITOR`. | Implementado. |
| Secrets | Esperados por ambiente/deployment secrets. | Implementado; secret manager é futuro. |
| Base de dados | PostgreSQL em runtime, H2 em testes. | Implementado; migrações formais são futuro. |
| Uploads | Tamanho/tipo/assinatura e nomes gerados. | Implementado. |
| Backups | Manifesto HMAC e verificação. | Implementado; encriptação/retenção externa são futuro. |
| Rate limiting | Em memória na aplicação. | Adequado ao âmbito; externo/distribuído é futuro. |
| Logs/auditoria | Auditoria e alertas com metadados de integridade. | Implementado; SIEM/WORM é futuro. |
| Dependências | Dependency-Check e CycloneDX. | Implementado. |

## Acções obrigatórias em produção

- Definir `JWT_SECRET` e secrets HMAC fortes.
- Manter `GHOSTREPORT_MFA_EXPOSE_CODE` desligado.
- Executar atrás de HTTPS.
- Usar PostgreSQL com credenciais restritas.
- Proteger directórios de uploads, evidência e backups ao nível do sistema
  operativo.
- Rever suppressions do Dependency-Check antes de release.
- Arquivar artefactos da pipeline mais recente.

## Riscos residuais

- Sem secret manager externo.
- Sem rotação automática de chaves.
- Sem framework formal de migrações.
- Sem armazenamento imutável de auditoria/SIEM.
- Rate limiting não distribuído.
