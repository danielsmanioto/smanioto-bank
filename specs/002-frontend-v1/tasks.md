# Tasks: Front-end v1 — Interface Web do Banco Digital

**Input**: `specs/002-frontend-v1/spec.md`

## Phase 1: Setup

- [x] T001 Criar estrutura de diretórios do front-end em `services/frontend/` com subpastas `css/` e `js/`
- [x] T002 Criar `services/frontend/css/style.css` com estilos globais (reset, tipografia, formulários, botões, tabela de extrato, mensagens de erro/sucesso)
- [x] T003 Criar `services/frontend/server.sh` com comando `python3 -m http.server 3000` para servir o front-end localmente

## Phase 2: Fundacional — CORS e portas dos serviços backend

- [x] T004 Criar `services/auth-service/src/main/resources/application.properties` com `server.port=8080` e adicionar `@CrossOrigin(origins = "http://localhost:3000")` em `services/auth-service/src/main/java/com/smanioto/bank/auth/controller/AuthController.java`
- [x] T005 Criar `services/people-service/src/main/resources/application.properties` com `server.port=8081` e adicionar `@CrossOrigin(origins = "http://localhost:3000")` em `services/people-service/src/main/java/com/smanioto/bank/people/controller/CustomerController.java`
- [x] T006 Criar `services/accounts-service/src/main/resources/application.properties` com `server.port=8082` e adicionar `@CrossOrigin(origins = "http://localhost:3000")` em `services/accounts-service/src/main/java/com/smanioto/bank/accounts/controller/AccountController.java`
- [x] T007 Criar `services/frontend/js/api.js` — módulo central com funções `login(user, pass)`, `getAccount(accountId, token)`, `getStatement(accountId, token)`, `transfer(fromId, toId, amount, token)` usando `fetch` e as URLs base de cada serviço
- [x] T008 Criar `services/frontend/js/auth.js` — guard que lê token do `sessionStorage`, chama `GET /auth/validate` e redireciona para `login.html` se inválido ou ausente

## Phase 3: Tela de Login (US1)

- [x] T009 [US1] Criar `services/frontend/login.html` com formulário de login (campos usuário e senha, botão Entrar, área de mensagem de erro)
- [x] T010 [US1] Criar `services/frontend/js/login.js` — captura submit do formulário, chama `api.login()`, salva token no `sessionStorage` e redireciona para `account.html` com `?accountId=...` na URL; exibe erro em falha

## Phase 4: Tela de Visão de Conta (US2)

- [x] T011 [US2] Criar `services/frontend/account.html` com layout para exibir banco, agência, número da conta, saldo atual e botões de navegação para extrato e transferência, além de botão Sair
- [x] T012 [US2] Criar `services/frontend/js/account.js` — importa `auth.js` (guard), lê `accountId` da URL, chama `api.getAccount()`, preenche os dados na tela; botão Sair limpa `sessionStorage` e redireciona para `login.html`

## Phase 5: Tela de Extrato (US3)

- [x] T013 [US3] Criar `services/frontend/statement.html` com tabela de movimentações (colunas: data, tipo, valor, descrição) e área para mensagem de extrato vazio
- [x] T014 [US3] Criar `services/frontend/js/statement.js` — importa `auth.js` (guard), lê `accountId` da URL, chama `api.getStatement()`, renderiza movimentações na tabela em ordem cronológica; exibe mensagem de extrato vazio se lista vazia

## Phase 6: Tela de Transferência (US4)

- [x] T015 [US4] Criar `services/frontend/transfer.html` com formulário de transferência (campos ID conta destino e valor, botão Transferir, área de mensagem de sucesso/erro)
- [x] T016 [US4] Criar `services/frontend/js/transfer.js` — importa `auth.js` (guard), lê `accountId` de origem da URL, valida campos (destino não vazio, valor > 0) antes do submit, chama `api.transfer()`, exibe mensagem de sucesso ou erro conforme resposta do backend

## Phase 7: Página inicial e navegação

- [x] T017 Criar `services/frontend/index.html` que redireciona automaticamente para `login.html` (meta refresh ou JS redirect)
- [x] T018 Verificar e ajustar links de navegação entre todas as páginas (`account.html` ↔ `statement.html` ↔ `transfer.html`) garantindo que `accountId` seja sempre passado via query string

## Dependencies

```
T001 → T002, T003
T004, T005, T006 (paralelos — serviços independentes)
T007 → T008 → T010, T012, T014, T016
T009 → T010
T011 → T012
T013 → T014
T015 → T016
T017, T018 (último — após todas as páginas prontas)
```

## MVP Scope

US1 (T009–T010) + T001–T008 são o MVP mínimo executável — com login funcionando, as demais telas podem ser incrementadas em seguida.

## Parallel Execution

- T004, T005, T006 podem ser implementadas em paralelo (um serviço cada).
- T009+T011+T013+T015 (HTML de cada tela) podem ser criados em paralelo.
- T010+T012+T014+T016 (JS de cada tela) dependem de T007+T008 mas são independentes entre si.
