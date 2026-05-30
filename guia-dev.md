# Guia de desenvolvimento (SDD + Spec Kit)

Este repositório será seu laboratório para aprender **SDD (Spec-Driven Development)** com foco em um banco digital em Java.

## 1) O que já foi instalado

- **Spec Kit** inicializado no projeto.
- Estrutura criada em:
  - `.specify/` (templates, scripts e workflow SDD)
  - `.github/prompts/` (comandos `/speckit.*` para o Copilot)

Fluxo principal que você vai usar no dia a dia:

1. `/speckit.constitution`
2. `/speckit.specify`
3. `/speckit.plan`
4. `/speckit.tasks`
5. `/speckit.implement`

## 2) Visão do produto (seu banco digital)

Escopo inicial:

- Conta bancária para **PF**.
- Cada conta com:
  - banco
  - agência
  - número da conta
  - saldo
- Login com usuário/senha e autenticação com **JWT**.
- Extrato (histórico de lançamentos).
- Transferência entre usuários do próprio banco (movimentando saldo real entre contas internas).

Escopo futuro:

- Serviço para saída de dinheiro para outros bancos (base para **PIX** futuro).

## 3) Arquitetura alvo (microserviços)

Sugestão de serviços Java:

1. **auth-service**
   - cadastro de credenciais
   - login
   - emissão/validação de JWT

2. **people-service**
   - cadastro de clientes PF
   - dados pessoais

3. **accounts-service**
   - abertura e gestão de contas
   - saldo
   - transferências internas
   - extrato

Front-end v1 (simples):

- HTML + CSS + JavaScript
- Servidor web básico
- Telas:
  - login
  - visão de conta
  - extrato
  - transferência

## 4) Ordem recomendada para aprender e construir

### Fase 1 — Fundamentos

1. Definir princípios do projeto com `/speckit.constitution`.
2. Criar especificação do MVP com `/speckit.specify`.
3. Refinar com `/speckit.clarify` (opcional, mas recomendado).

### Fase 2 — Planejamento técnico

1. Executar `/speckit.plan` informando stack Java (ex.: Spring Boot, PostgreSQL, JWT).
2. Gerar backlog com `/speckit.tasks`.

### Fase 3 — Implementação incremental

1. Implementar primeiro o **auth-service**.
2. Depois **people-service**.
3. Depois **accounts-service** (saldo, extrato, transferência interna).
4. Criar front-end v1 para consumir as APIs.

### Fase 4 — Evolução

1. Hardening de segurança.
2. Testes integrados entre serviços.
3. Preparar contrato para saída externa (futuro PIX).

## 5) Regras de domínio mínimas (MVP)

- Não permitir transferência com saldo insuficiente.
- Registrar toda movimentação no extrato.
- Transferência interna deve ser atômica (débito/crédito).
- JWT obrigatório nas rotas protegidas.
- Cliente PF deve existir antes de abrir conta.

## 6) Como usar SDD neste repo (prática)

Sempre que for iniciar uma funcionalidade:

1. Escreva o objetivo com `/speckit.specify`.
2. Detalhe decisões técnicas com `/speckit.plan`.
3. Quebre em tarefas com `/speckit.tasks`.
4. Implemente com `/speckit.implement`.
5. Revise consistência com `/speckit.analyze`.

Isso cria um ciclo claro: **especificar → planejar → executar → validar**.
