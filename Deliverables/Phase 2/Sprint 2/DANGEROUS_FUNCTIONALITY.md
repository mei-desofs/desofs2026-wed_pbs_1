# Dangerous Functionality Inventory

Este anexo identifica pontos do GhostReport onde o codigo executa operacoes
sensíveis ou potencialmente perigosas. O objetivo e tornar explicitas as
pre-condicoes, mitigacoes e testes que suportam a revisao ASVS Sprint 2,
especialmente os controlos de secure coding e architecture.

## Inventario

| Area | Codigo | Risco principal | Mitigacoes atuais | Evidencia |
| --- | --- | --- | --- | --- |
| Backup restore | `AdminBackupController.restoreBackup`, `BackupService.restoreBackup` | Repor ou extrair ZIP manipulado, ZIP Slip, operacao administrativa sensivel. | Apenas `ADMIN`, CSRF, filename canonical, manifest/hash/HMAC antes de restore, rejeicao de entries nao assinadas, restore para staging, reautenticacao com `X-Reauth-Password`. | `AdminBackupControllerSecurityTest`, `BackupServiceIntegrationTest` |
| Backup create/download/verify | `BackupService.createBackup`, `getBackupResource`, `verifyBackup` | Exposicao ou tampering de dados exportados. | Diretorio dedicado, nomes allowlisted, hash sidecar, manifest HMAC, validacao de paths, `Content-Disposition` para download e alertas de traversal. | `BackupServiceIntegrationTest`, `AdminBackupControllerSecurityTest` |
| Uploads anonimos | `ReportController`, `ReportService`, `FileStorageService`, `LocalMalwareScanner` | Path traversal, malware de teste, upload excessivo, MIME/extensao incoerente. | Tracking code requerido para anexos, limites de ficheiros/tamanho, MIME+extensao+magic bytes, nomes gerados por UUID, scanner local EICAR, quarantine para rejeitados, paths canonicalizados. | `ReportControllerAttachmentUploadTest`, `FileStorageServiceTest`, `LocalMalwareScannerTest` |
| Download de anexos | `ReportService.downloadAttachment`, analyst/auditor endpoints | Exposicao de ficheiros errados ou paths internos. | Autorizacao por tracking code/role/ownership, `Content-Disposition: attachment`, `nosniff`, no-store, ids controlados e paths resolvidos dentro de storage. | `ReportControllerAttachmentUploadTest`, `RbacAuthorizationMatrixTest` |
| Evidence packages | `CasePackageService.generateCasePackage`, `verifyEvidencePackage` | Pacotes incompletos, tampering, path traversal em artefactos. | Apenas casos fechados/autorizados, manifesto JSON, SHA-256, safe resolve, verificacao auditor/admin. | `CasePackageServiceIntegrationTest`, `AuditorAuthorizationTest` |
| Password reset/admin reset | `PasswordResetService`, `AdminController.initiatePasswordReset` | Enumeracao de contas, reset indevido, exposicao de token. | Resposta generica, token hashed, TTL, token exposto apenas em dev/test configurado, admin nao define password nova diretamente. | `PasswordPolicyAndResetSecurityTest`, `AdminUserManagementSecurityTest` |
| JWT e revogacao | `JwtService`, `JwtAuthenticationFilter`, `RevokedToken` | Token tampering, algoritmo fraco, reuse depois de logout. | HMAC-SHA-256 allowlisted, `iss`/`aud`/`jti`, segredo configurado, denylist persistente ate expiracao. | `JwtServiceSecurityTest`, `JwtRevocationPersistenceIntegrationTest` |
| Audit/security logs | `AuditLogService`, `SecurityMonitoringService`, `SecurityLogSanitizer` | Log injection, segredos em logs, tampering local sem deteccao externa. | Sanitizacao, redacao de bearer/JWT/tracking/secrets, correlation id, integrity hash por registo. | `SecurityLogSanitizerTest`, `RuntimeSecurityEventLoggingTest`, `AuditLogSecurityTest` |
| Criptografia/hashing | `JwtService`, `BackupService`, `PasswordResetService`, `TrackingCode` | Uso acidental de algoritmo obsoleto ou segredo errado. | Inventario criptografico, BCrypt, SecureRandom, SHA-256/HMAC-SHA-256, validacao de tamanho/segregacao de segredos. | `CRYPTOGRAPHIC_INVENTORY.md`, `CryptographicInventoryTest` |

## Criterios de revisao

- Qualquer alteracao nestas areas deve ter code review e teste automatizado.
- Operacoes destrutivas ou de alto impacto devem manter pre-condicoes
  explicitas no endpoint ou no service.
- Erros devem continuar genericos e sem stack traces/paths internos.
- Novos usos de filesystem, ZIP, crypto, passwords ou tokens devem atualizar
  este anexo e o tracker ASVS.

## Limitacoes

- O restore continua a ser uma operacao administrativa sensivel; para producao,
  recomenda-se aprovacao multi-utilizador e execucao fora do processo web quando
  existir workflow operacional para isso.
- O projeto nao usa HSM/Vault externo; segredos continuam configurados por
  variaveis/propriedades de ambiente conforme `SECURE_INSTALLATION.md`.
- O scanner de malware actual e local e baseado em assinatura EICAR/teste; nao
  substitui antivirus, sandbox ou servico externo de analise em producao.
- `integrityHash` em audit logs/security alerts ajuda a detectar alteracao
  local, mas nao equivale a SIEM/WORM append-only externo.
- Backups usam HMAC/hashes para integridade, mas nao encriptacao aplicacional
  nem retencao externa automatizada.
