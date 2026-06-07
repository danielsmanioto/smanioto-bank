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

Escopo implementado:

- Conta bancária para **PF**.
- Cada conta com banco, agência, número e saldo.
- Login com usuário/senha e autenticação via **JWT**.
- Extrato (histórico de lançamentos por conta).
- Transferência interna atômica entre contas do mesmo banco.
- **Camada de dados**: pipeline PySpark que lê o banco operacional e gera Parquet diário particionado por conta/data, permitindo análises históricas sem impactar o serviço transacional.

Escopo futuro:

- Serviço para saída de dinheiro para outros bancos (base para **PIX** futuro).
- Migração do banco operacional de H2 para PostgreSQL.
- Controle incremental no job de dados (watermark por data de criação).

## 3) Arquitetura implementada

### Microsserviços Java

| Serviço | Porta | Responsabilidade |
|---|---|---|
| **auth-service** | 8080 | Cadastro de credenciais, login, emissão e validação de JWT |
| **people-service** | 8081 | Cadastro e consulta de clientes PF (CPF, nome, e-mail) |
| **accounts-service** | 8082 | Abertura de conta, saldo, transferências internas, extrato |

Cada serviço tem banco H2 in-memory próprio e independente. O `accounts-service` chama o `people-service` via HTTP para validar o cliente antes de abrir uma conta.

### Frontend

- HTML + CSS + JavaScript sem framework
- Servidor Node.js simples na porta 3000
- Telas: login, visão de conta, extrato, transferência

### Camada de dados (Data Lake)

```
accounts-service (H2 TCP :9092)
        │
        │ JDBC — leitura assíncrona
        ▼
  glue_job.py (PySpark local — simula AWS Glue)
        │
        │ overwrite
        ▼
  Parquet particionado por account_id / date
        │
        │ pyarrow read
        ▼
  query_daily.py (CLI para análise ad-hoc)
```

- Lê tabelas `ACCOUNTS` e `MOVEMENTS` via JDBC sem alterar a API do serviço
- Calcula posição diária (saldo inicial, créditos, débitos, saldo final) com janela regressiva
- Salva em `services/data-lake/output/daily_statement/`
- Stack: Python 3, PySpark 3.5+, pandas, pyarrow

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

### Fase 4 — Camada de dados

1. Habilitar H2 TCP server no `accounts-service` (porta 9092).
2. Implementar `glue_job.py` com PySpark para leitura via JDBC e geração de Parquet.
3. Implementar `query_daily.py` com pyarrow para consultas ad-hoc.
4. Documentar decisão em ADR (ver ADR-004).

### Fase 5 — Evolução

1. Hardening de segurança.
2. Testes integrados entre serviços.
3. Migração para PostgreSQL (substituir H2 in-memory).
4. Controle incremental no job de dados (watermark).
5. Preparar contrato para saída externa (futuro PIX).

## 5) Regras de domínio

### Transacional (serviços Java)

- Não permitir transferência com saldo insuficiente.
- Registrar toda movimentação no extrato.
- Transferência interna deve ser atômica (débito e crédito ocorrem juntos ou nenhum ocorre).
- JWT obrigatório nas rotas protegidas.
- Cliente PF deve existir no `people-service` antes de abrir conta no `accounts-service`.

### Dados (data-lake)

- O job lê o banco operacional sem impactar a API do serviço (conexão JDBC direta ao H2 TCP).
- O Parquet é sempre sobrescrito — não há estado incremental no MVP.
- O saldo diário é reconstruído de forma regressiva a partir do saldo atual; nunca é lido diretamente do extrato cumulativo.

## 6) Como usar SDD neste repo (prática)

Sempre que for iniciar uma funcionalidade:

1. Escreva o objetivo com `/speckit.specify`.
2. Detalhe decisões técnicas com `/speckit.plan`.
3. Quebre em tarefas com `/speckit.tasks`.
4. Implemente com `/speckit.implement`.
5. Revise consistência com `/speckit.analyze`.

Isso cria um ciclo claro: **especificar → planejar → executar → validar**.
