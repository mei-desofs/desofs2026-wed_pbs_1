# Inventário criptográfico

Este ficheiro complementa o tracker ASVS Sprint 2 com uma lista curta dos usos de criptografia existentes no GhostReport. O objetivo é manter rastreabilidade entre código, finalidade, algoritmo e material de chave.

## Inventário atual

| Uso                            | Localização                                                                      | Mecanismo                                                                                  | Material de chave/segredo                                                          | Estado                                   |
| ------------------------------ | -------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------- | ---------------------------------------- |
| Passwords internas             | `SecurityConfig`, `UserService`, `PasswordPolicyService`, `PasswordResetService` | `BCryptPasswordEncoder`                                                                    | Password fornecida pelo utilizador; hash BCrypt persistido.                        | Implementado                             |
| Tracking code anónimo          | `TrackingCode`, `ReportService`                                                  | `SecureRandom` + Base64 URL-safe; hash BCrypt para verificação.                            | Código gerado pela aplicação; hash persistido.                                     | Implementado                             |
| MFA dev/test                   | `MfaChallengeService`                                                            | `SecureRandom` para código de 6 dígitos; BCrypt para hash do challenge.                    | Código temporário; hash em memória; TTL e uso único.                               | Implementado; canal de produção é futuro |
| Password reset                 | `PasswordResetService`                                                           | `SecureRandom` + Base64 URL-safe; SHA-256 do token antes de persistir.                     | Token temporário; hash persistido; TTL e uso único.                                | Implementado                             |
| JWT                            | `JwtService`                                                                     | HMAC-SHA-256 (`HS256`) com `kid`, `iss`, `aud`, `jti`; comparação constante da assinatura. | `JWT_SECRET`, `ghostreport.jwt.active-key-id`, `ghostreport.jwt.previous-secrets`. | Implementado                             |
| Backups                        | `BackupService`                                                                  | HMAC-SHA-256 do manifesto; SHA-256 de ficheiros/entradas; comparação constante do HMAC.    | `BACKUP_HMAC_SECRET`, `BACKUP_HMAC_KEY_ID`.                                        | Implementado                             |
| Audit logs e security alerts   | `AuditLogService`, `SecurityMonitoringService`                                   | SHA-256 para hash de integridade de registos.                                              | Sem segredo; integridade local, não assinatura externa.                            | Implementado                             |
| Uploads e pacotes de evidência | `FileStorageService`, `CasePackageService`                                       | SHA-256 para hashes de ficheiros/pacotes.                                                  | Sem segredo; deteção de alteração.                                                 | Implementado                             |

## Política de alteração

* Novas operações criptográficas devem usar APIs JCA/JCE ou Spring Security existentes, evitando algoritmos obsoletos como MD5, SHA-1, DES, RC4, ECB e RSA PKCS#1 v1.5.
* Segredos simétricos devem ter pelo menos 32 bytes quando configurados por propriedade, como já acontece para JWT e HMAC de backups.
* Novos usos de `Mac`, `MessageDigest`, `SecureRandom`, `Cipher`, `Signature`, `BCryptPasswordEncoder` ou `PasswordEncoder` devem atualizar este inventário.
* Algoritmos com chaves devem ter plano de rotação documentado no respetivo guia de configuração. No Sprint 2, JWT já suporta `kid` ativo e segredos anteriores; os backups usam `BACKUP_HMAC_KEY_ID`.

## Limitações

* Não existe `Cipher`/encriptação aplicacional com IV/nonce ou combinação encryption+MAC; a aplicação usa BCrypt, SecureRandom, HMAC-SHA-256, SHA-256 e TLS da plataforma/deployment.
* Não existe HSM, vault ou KMS externo no ambiente académico atual.
* Não existe inventário automático de runtime em produção; a verificação é feita por teste estático no código-fonte.
* Migração para algoritmos pós-quânticos fica como trabalho futuro, porque o sistema atual usa sobretudo HMAC, BCrypt e hashes para integridade local.
