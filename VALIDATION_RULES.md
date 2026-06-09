# API Validation Rules

Scope: ASVS input validation, allowlists, API contracts, content-type handling, and safe error responses for GhostReport backend endpoints.

All JSON request endpoints require `Content-Type: application/json` and return JSON error envelopes with:

| Field | Description |
| --- | --- |
| `timestamp` | Server-side error timestamp |
| `correlationId` | Opaque support identifier |
| `status` | HTTP status code |
| `error` | Generic safe error message |
| `fields` | Present only for validation failures; maps field names to `Invalid value` |

Validation errors must not expose stack traces, Java class names, filesystem paths, SQL errors, or exception details.

| Endpoint | Campo | Tipo | Obrigatório/Opcional | Limite | Allowlist/Formato | Erro esperado |
| --- | --- | --- | --- | --- | --- | --- |
| `POST /auth/login` | `username` | string | Obrigatório | max 120 | not blank | `400 Invalid request` |
| `POST /auth/login` | `password` | string | Obrigatório | max 128 | not blank | `400 Invalid request` |
| `POST /admin/users` | `username` | string | Obrigatório | 3-120 | `A-Z`, `a-z`, `0-9`, `.`, `_`, `-` | `400 Invalid request` |
| `POST /admin/users` | `email` | string | Obrigatório | max 160 | valid email | `400 Invalid request` |
| `POST /admin/users` | `password` | string | Obrigatório | 12-128 | uppercase, lowercase, number, symbol | `400 Invalid request` |
| `POST /admin/users` | `role` | enum string | Obrigatório | enum value | `ADMIN`, `ANALYST`, `AUDITOR` | `400 Invalid request` |
| `POST /reports` | `title` | string | Obrigatório | 3-200 | not blank | `400 Invalid request` |
| `POST /reports` | `description` | string | Obrigatório | 10-3000 | not blank | `400 Invalid request` |
| `POST /reports` | `category` | enum string | Obrigatório | max 40 | `Fraude`, `Fraud`, `Security`, `Privacy`, `Procurement`, `Ethics`, `Corruption`, `Harassment`, `Other` | `400 Invalid request` |
| `POST /reports/verify` | `trackingCode` | string | Obrigatório | max 67 total chars | `GR-` plus 20-64 URL-safe chars | `400 Invalid request` |
| `POST /reports/{id}/attachments` | `files` | multipart file[] | Obrigatório | configured file limits | multipart/form-data | `400 Invalid request` |
| `POST /reports/{id}/attachments` | `trackingCode` | request param string | Opcional at binding, required by service authorization | service-level tracking validation | `GR-` tracking code | `403 Access denied` or `400 Invalid request` |
| `POST /reports/{id}/attachments/list` | `trackingCode` | string | Obrigatório | max 67 total chars | `GR-` plus 20-64 URL-safe chars | `400 Invalid request` |
| `POST /reports/download` | `trackingCode` | string | Obrigatório | max 67 total chars | `GR-` plus 20-64 URL-safe chars | `400 Invalid request` |
| `POST /reports/download` | `attachmentId` | number | Obrigatório | positive long | `> 0` | `400 Invalid request` |
| `PATCH /analyst/reports/{id}/status` | `status` | enum string | Obrigatório | enum value | `SUBMITTED`, `UNDER_REVIEW`, `MORE_INFO_REQUIRED`, `RESOLVED`, `REJECTED` | `400 Invalid request` |
| `PATCH /analyst/reports/{id}/priority` | `priority` | enum string | Obrigatório | enum value | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` | `400 Invalid request` |
| `PATCH /analyst/reports/{id}/notes` | `notes` | string | Obrigatório | max 4000 | not blank | `400 Invalid request` |

Security impact:

| ASVS ID | Impacto |
| --- | --- |
| V2.1.1 | DTOs rejeitam campos obrigatórios vazios antes da camada de serviço |
| V2.1.2 | Campos textuais têm limites explícitos de tamanho |
| V2.1.3 | Formatos sensíveis, como tracking code, username, email e password, são validados |
| V2.2.1 | Campos enum usam allowlists ancoradas |
| V2.2.3 | Content-type JSON/multipart é declarado nos controllers |
| V4.1.1 | Erros de validação são uniformes e genéricos |
| V4.1.2 | Respostas não incluem stack traces nem detalhes internos |
| V4.1.3 | Erros de content-type e JSON malformado são tratados por handler central |
| V4.2.1 | Contrato de API por endpoint está documentado e coberto por testes MockMvc |
