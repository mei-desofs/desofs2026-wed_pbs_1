# Avaliacao de configuracao de seguranca

## Sumario

A configuracao actual e adequada para validacao academica local e baseline
prod-like, desde que secrets, PostgreSQL e TLS/proxy sejam fornecidos
externamente. Alguns controlos continuam a exigir hardening operacional antes de
producao real.

## Revisao de configuracao

| Area | Configuracao actual | Avaliacao |
| --- | --- | --- |
| Spring Security | Regras centralizadas, filtro JWT, CSP/HSTS/COOP/COEP/CORP, `report-uri /security/csp-report`, Fetch Metadata/Origin validation e request-boundary filter. | Implementado. |
| Autenticacao | BCrypt, JWT e MFA para `ADMIN`, `ANALYST` e `AUDITOR`. | Implementado. |
| Secrets | Esperados por ambiente/deployment secrets; dev/test secrets sao rejeitados em prod-like. | Implementado; secret manager e futuro. |
| Base de dados | PostgreSQL em runtime, H2 em testes; prod-like exige `sslmode=verify-ca` ou `sslmode=verify-full`. | Implementado; migracoes formais sao futuro. |
| Transporte TLS | `application-prod.yaml` define modo TLS, TLS 1.2/1.3, cifras modernas e reverse proxy trusted mode. | Implementado/configurado; certificado publico e operacional. |
| Limites de recursos | Hikari pool, timeouts, Tomcat connections/threads/backlog e header size configurados. | Implementado e validado em prod-like. |
| Uploads | Tamanho/tipo/assinatura e nomes gerados. | Implementado. |
| Backups | Manifesto HMAC e verificacao. | Implementado; encriptacao/retencao externa sao futuro. |
| Rate limiting | Em memoria na aplicacao para login, tracking, report submission, upload e download. | Adequado ao ambito; externo/distribuido e futuro. |
| Logs/auditoria | Auditoria e alertas com metadados de integridade. | Implementado; SIEM/WORM e futuro. |
| CSP reporting | Endpoint publico `/security/csp-report` guarda alerta sanitizado para violacoes CSP do browser. | Implementado. |
| Dependencias | Dependency-Check e CycloneDX. | Implementado. |

## Accoes obrigatorias em producao

- Definir `JWT_SECRET` e secrets HMAC fortes.
- Manter `GHOSTREPORT_MFA_EXPOSE_CODE` desligado.
- Executar atras de HTTPS por reverse proxy ou activar TLS directo com keystore.
- Definir `GHOSTREPORT_TRANSPORT_TLS_MODE`.
- Em reverse proxy, confirmar `GHOSTREPORT_TRUSTED_PROXY_ENABLED=true` e
  `SERVER_FORWARD_HEADERS_STRATEGY=framework` ou `native`.
- Usar PostgreSQL com credenciais restritas.
- Usar `DB_URL` PostgreSQL com `sslmode=verify-ca` ou `sslmode=verify-full`.
- Manter limites Hikari/Tomcat positivos e ajustados ao ambiente.
- Proteger directorios de uploads, evidencia e backups ao nivel do sistema
  operativo.
- Rever suppressions do Dependency-Check antes de release.
- Arquivar artefactos da pipeline mais recente.

## Riscos residuais

- Sem secret manager externo.
- Sem rotacao automatica de chaves.
- Sem framework formal de migracoes.
- Sem armazenamento imutavel de auditoria/SIEM.
- Rate limiting nao distribuido.
- Certificado publico, OCSP/ECH e rotacao automatica de certificados dependem
  da infraestrutura de deployment.
