# Implementation Plan: Front-end v1 — Interface Web do Banco Digital

**Branch**: `002-frontend-v1` | **Date**: 2026-06-04 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-frontend-v1/spec.md`

## Summary

Construir a interface web v1 do banco digital smanioto-bank em HTML + CSS + JavaScript puro (sem framework), com 4 telas funcionais (login, conta, extrato, transferência) que consomem diretamente os 3 microserviços Java já implementados. Inclui configuração de CORS nos serviços backend e um servidor HTTP simples para servir os arquivos estáticos localmente.

## Technical Context

**Language/Version**: HTML5, CSS3, JavaScript ES2022 (sem transpilação)

**Primary Dependencies**: Nenhuma — vanilla JS puro; servidor estático com Python 3 http.server

**Storage**: localStorage (token JWT) — sem persistência de dados no front-end

**Testing**: Manual via navegador (testes automatizados de UI fora do escopo do v1)

**Target Platform**: Navegador desktop moderno (Chrome/Firefox/Safari) rodando em localhost

**Project Type**: Aplicação web estática servida localmente

**Performance Goals**: Tempo de carregamento < 1s em localhost; resposta de API < 500ms localmente

**Constraints**: Sem dependências externas (CDN, npm); deve funcionar com `python3 -m http.server`

**Scale/Scope**: 4 páginas HTML, 1 módulo JS de API, 1 CSS global

## APIs dos Serviços Backend

### auth-service (porta 8080)
| Método | Endpoint       | Descrição                        |
|--------|---------------|----------------------------------|
| POST   | /auth/login    | Login — retorna `{ token, type }` |
| GET    | /auth/validate | Valida token JWT (Bearer)        |

### people-service (porta 8081)
| Método | Endpoint      | Descrição                      |
|--------|--------------|--------------------------------|
| POST   | /people       | Cadastrar cliente PF           |
| GET    | /people/{id}/exists | Verificar existência de cliente |

### accounts-service (porta 8082)
| Método | Endpoint                       | Descrição                        |
|--------|-------------------------------|----------------------------------|
| POST   | /accounts                      | Abrir conta                      |
| GET    | /accounts/{accountId}/statement | Consultar extrato                |
| POST   | /accounts/transfer             | Realizar transferência           |

> **Nota**: Os serviços precisam de `application.properties` com `server.port` e configuração de CORS para `http://localhost:3000`.

## Constitution Check

*GATE: simplicity first — sem framework, sem build tooling, sem dependências externas.*

- [x] Solução mais simples possível para o problema
- [x] Sem dependências desnecessárias
- [x] Cada arquivo tem propósito único e claro
- [x] JWT no localStorage é adequado para MVP local (sem risco de produção)

## Project Structure

### Documentation (this feature)

```text
specs/002-frontend-v1/
├── plan.md              # Este arquivo
├── research.md          # Decisões técnicas
├── data-model.md        # Entidades do front-end
├── contracts/           # Contratos das APIs consumidas
└── tasks.md             # Gerado pelo speckit.tasks
```

### Source Code

```text
services/frontend/
├── index.html            # Redireciona para login.html
├── login.html            # Tela de login
├── account.html          # Visão de conta (saldo + navegação)
├── statement.html        # Extrato de movimentações
├── transfer.html         # Formulário de transferência
├── css/
│   └── style.css         # Estilos globais
├── js/
│   ├── api.js            # Módulo de chamadas HTTP para os 3 serviços
│   ├── auth.js           # Guard de autenticação (redirect se não logado)
│   ├── login.js          # Lógica da tela de login
│   ├── account.js        # Lógica da tela de conta
│   ├── statement.js      # Lógica da tela de extrato
│   └── transfer.js       # Lógica da tela de transferência
└── server.sh             # Script para iniciar python3 http.server na porta 3000
```

### CORS nos Serviços Backend

Cada serviço precisa de um arquivo `application.properties` com:

```properties
# auth-service
server.port=8080
spring.web.cors.allowed-origins=http://localhost:3000

# people-service
server.port=8081
spring.web.cors.allowed-origins=http://localhost:3000

# accounts-service
server.port=8082
spring.web.cors.allowed-origins=http://localhost:3000
```

E um `@CrossOrigin(origins = "http://localhost:3000")` em cada Controller (ou configuração global via `WebMvcConfigurer`).

## Complexity Tracking

Nenhuma violação de princípio de simplicidade identificada.
