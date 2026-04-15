# Ghost Report

## Descrição da Plataforma

O **GhostReport** é uma plataforma web para **submissão e acompanhamento de denúncias anónimas** dentro de uma organização.

A aplicação permite que utilizadores reportem situações como fraude, corrupção, assédio ou vulnerabilidades de segurança, garantindo que a sua identidade permanece protegida durante todo o processo.

O sistema foi desenvolvido com um forte foco em **segurança**, assegurando:

- anonimato do denunciante
- integridade dos dados submetidos
- controlo de acessos
- rastreabilidade das ações

## Objetivos da Aplicação

- Garantir o anonimato dos utilizadores
- Proteger a integridade das denúncias 
- Permitir auditoria e rastreabilidade
- Facilitar deteção de incidentes de segurança

> Este projeto foi desenvolvido no âmbito da unidade curricular de Desenvolvimento de Software Seguro (DESOFS).

# Descrição do Sistema

## Tipo de Utilizadores

### Denunciante Anónimo
- Submete denúncias sem autenticação
- Consulta estado através de código

### Analista
- Analisa e gere denúncias
- Atualiza estados e adiciona notas

### Administrador
- Gere utilizadores e permissões
- Controla o sistema

---

## Funcionalidades Principais
- Submissão de denúncias anónimas
- Upload de ficheiros (evidências)
- Consulta de denúncia por código
- Gestão de denúncias (analista)
- Gestão de utilizadores (administrador)
- Armazenamento seguro de ficheiros

---

## Arquitetura 
O sistema é composto por:
- Denunciante
- Cliente Interno
- API Backend
- Base de dados relacional
- Sistema de ficheiros 
- Sistema de logs/auditoria

---

## Segurança
O GhostReport foi desenvolvido seguindo os princípios de **Secure Software Development Life Cycle (SSDLC)**.

### Principais medidas:
- Autenticação Segura
- RBAC
- Comunicação segura (HTTPS/TLS)
- Anonimização de dados
- Proteção contra path traversal
- Validação de uploads
- Minimização de dados
- Logging e Auditoria
- Backups e recuperação

---

## Testes de Segurança
O sistema inclui testes para:
- autenticação e autorização
- validação de inputs
- upload de ficheiros
- brute force e enumeração
- logging
- recuperação de falhas

## Autores

- Alexandre Vieira
- Bárbara Silva
- Sofia Marques
