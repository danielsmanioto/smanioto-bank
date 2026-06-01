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

## Próximas fases (backlog)

- [ ] T009 [US2] Iniciar `people-service` para cadastro de cliente PF
- [ ] T010 [US2] Iniciar `accounts-service` para abertura de conta
- [ ] T011 [US3] Implementar extrato
- [ ] T012 [US3] Implementar transferência interna com atomicidade
