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
- [ ] T008 [US1] Persistir credenciais em banco de dados (próximo passo)

## Phase 4: Cadastro de cliente e conta (US2)

- [ ] T009 [US2] Criar estrutura base do `people-service` em `/tmp/workspace/danielsmanioto/smanioto-bank/services/people-service`
- [ ] T010 [US2] Implementar entidade e persistência de cliente PF no `people-service`
- [ ] T011 [US2] Implementar endpoint de cadastro de cliente PF com validações obrigatórias
- [ ] T012 [US2] Criar estrutura base do `accounts-service` em `/tmp/workspace/danielsmanioto/smanioto-bank/services/accounts-service`
- [ ] T013 [US2] Implementar entidade e persistência de conta (banco, agência, número, saldo)
- [ ] T014 [US2] Implementar regra de abertura de conta apenas para cliente PF já cadastrado
- [ ] T015 [US2] Implementar endpoint de abertura de conta vinculada ao cliente
- [ ] T016 [US2] Criar testes de cadastro de cliente e abertura de conta

## Phase 5: Extrato e transferência interna (US3)

- [ ] T017 [US3] Implementar entidade de movimentação e persistência de lançamentos
- [ ] T018 [US3] Implementar endpoint de consulta de extrato em ordem cronológica
- [ ] T019 [US3] Implementar endpoint de transferência interna entre contas
- [ ] T020 [US3] Garantir processamento atômico de débito/crédito da transferência
- [ ] T021 [US3] Implementar validações de transferência (saldo insuficiente, valor inválido, conta destino inexistente)
- [ ] T022 [US3] Criar testes de extrato e transferência (sucesso e rejeições)
