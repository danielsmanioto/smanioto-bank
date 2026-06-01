# Tasks: MVP Banco Digital PF

**Input**: `/tmp/workspace/danielsmanioto/smanioto-bank/specs/001-mvp-banco-digital-pf/spec.md`

## Phase 1: Setup

- [x] T001 Criar estrutura inicial do serviço em `/tmp/workspace/danielsmanioto/smanioto-bank/services/auth-service`
- [x] T002 Configurar dependências base de API, segurança e testes no `pom.xml`

## Phase 2: Fundacional (Auth primeiro)

- [x] T003 [US1] Implementar cadastro de credencial em `AuthController` e `UserCredentialsService`
- [x] T004 [US1] Implementar login com emissão de JWT em `AuthController` e `JwtService`
- [x] T005 [US1] Proteger rotas com filtro JWT em `SecurityConfig` e `JwtAuthenticationFilter`
- [x] T006 [US1] Implementar endpoint de validação de sessão em `AuthController`

## Phase 3: Testes de Auth

- [x] T007 [US1] Criar testes de cadastro/login/rota protegida em `src/test/java/com/smanioto/bank/auth/controller/AuthControllerTest.java`
- [x] T008 [US1] Persistir credenciais em banco de dados (próximo passo)

## Phase 4: Cadastro de cliente e conta (US2)

- [x] T009 [US2] Criar estrutura base do `people-service` em `services/people-service`
- [x] T010 [US2] Implementar entidade e persistência de cliente PF no `people-service`
- [x] T011 [US2] Implementar endpoint de cadastro de cliente PF com validações obrigatórias
- [x] T012 [US2] Criar estrutura base do `accounts-service` em `services/accounts-service`
- [x] T013 [US2] Implementar entidade e persistência de conta (banco, agência, número, saldo)
- [x] T014 [US2] Implementar regra de abertura de conta apenas para cliente PF já cadastrado
- [x] T015 [US2] Implementar endpoint de abertura de conta vinculada ao cliente
- [x] T016 [US2] Criar testes de cadastro de cliente e abertura de conta

## Phase 5: Extrato e transferência interna (US3)

- [x] T017 [US3] Implementar entidade de movimentação e persistência de lançamentos
- [x] T018 [US3] Implementar endpoint de consulta de extrato em ordem cronológica
- [x] T019 [US3] Implementar endpoint de transferência interna entre contas
- [x] T020 [US3] Garantir processamento atômico de débito/crédito da transferência
- [x] T021 [US3] Implementar validações de transferência (saldo insuficiente, valor inválido, conta destino inexistente)
- [x] T022 [US3] Criar testes de extrato e transferência (sucesso e rejeições)
