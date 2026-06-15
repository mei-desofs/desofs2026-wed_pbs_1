# Inventario criptografico

Este ficheiro complementa o tracker ASVS Sprint 2 com uma lista curta dos
usos de criptografia existentes no GhostReport. O objectivo e manter
rastreabilidade entre codigo, finalidade, algoritmo e material de chave.

## Inventario actual

| Uso | Localizacao | Mecanismo | Material de chave/segredo | Estado |
| --- | --- | --- | --- | --- |
| Passwords internas | `SecurityConfig`, `UserService`, `PasswordPolicyService`, `PasswordResetService` | `BCryptPasswordEncoder` | Password fornecida pelo utilizador; hash BCrypt persistido. | Implementado |
| Tracking code anonimo | `TrackingCode`, `ReportService` | `SecureRandom` + Base64 URL-safe; hash BCrypt para verificacao. | Codigo gerado pela aplicacao; hash persistido. | Implementado |
| MFA dev/test | `MfaChallengeService` | `SecureRandom` para codigo de 6 digitos; BCrypt para hash do challenge. | Codigo temporario; hash em memoria; TTL e uso unico. | Implementado; canal de producao e futuro |
| Password reset | `PasswordResetService` | `SecureRandom` + Base64 URL-safe; SHA-256 do token antes de persistir. | Token temporario; hash persistido; TTL e uso unico. | Implementado |
| JWT | `JwtService` | HMAC-SHA-256 (`HS256`) com `kid`, `iss`, `aud`, `jti`; comparacao constante da assinatura. | `JWT_SECRET`, `ghostreport.jwt.active-key-id`, `ghostreport.jwt.previous-secrets`. | Implementado |
| Backups | `BackupService` | HMAC-SHA-256 do manifesto; SHA-256 de ficheiros/entradas; comparacao constante do HMAC. | `BACKUP_HMAC_SECRET`, `BACKUP_HMAC_KEY_ID`. | Implementado |
| Audit logs e security alerts | `AuditLogService`, `SecurityMonitoringService` | SHA-256 para hash de integridade de registos. | Sem segredo; integridade local, nao assinatura externa. | Implementado |
| Uploads e pacotes de evidencia | `FileStorageService`, `CasePackageService` | SHA-256 para hashes de ficheiros/pacotes. | Sem segredo; deteccao de alteracao. | Implementado |

## Politica de alteracao

- Novas operacoes criptograficas devem usar APIs JCA/JCE ou Spring Security
  existentes, evitando algoritmos obsoletos como MD5, SHA-1, DES, RC4, ECB e
  RSA PKCS#1 v1.5.
- Segredos simetricos devem ter pelo menos 32 bytes quando configurados por
  propriedade, como ja acontece para JWT e HMAC de backups.
- Novos usos de `Mac`, `MessageDigest`, `SecureRandom`, `Cipher`,
  `Signature`, `BCryptPasswordEncoder` ou `PasswordEncoder` devem actualizar
  este inventario.
- Algoritmos com chaves devem ter plano de rotacao documentado no respectivo
  guia de configuracao. No Sprint 2, JWT ja suporta `kid` activo e segredos
  anteriores; backups usam `BACKUP_HMAC_KEY_ID`.

## Limitacoes

- Nao existe HSM, vault ou KMS externo no ambiente academico actual.
- Nao existe inventario automatico de runtime em producao; a verificacao e
  feita por teste estatico no codigo-fonte.
- Migração para algoritmos pos-quanticos fica como trabalho futuro, porque o
  sistema actual usa sobretudo HMAC, BCrypt e hashes para integridade local.
