# Feature Specification: Democratização do Extrato

**Feature Branch**: `004-democratizacao-extrato`

**Created**: 2026-06-04

**Status**: Implementado

**Input**: Issue #10 — manter tabela SOR por tabela, visão diária do extrato via Parquet, job PySpark simulando AWS Glue rodando local.

---

## Contexto

O accounts-service mantém a tabela `MOVEMENTS` como **SOR (System of Record)** de todos os lançamentos bancários. Para habilitar análises históricas e democratizar o acesso a esses dados sem impactar o serviço operacional, esta spec introduz uma camada de **Data Lake local** que:

1. Lê as tabelas SOR via JDBC (H2 TCP)
2. Calcula uma visão agregada **diária** por conta
3. Persiste em **Parquet** particionado por `account_id/date`
4. Permite consultas ad-hoc sobre a posição diária

---

## User Scenarios & Testing

### User Story 1 — Executar job de democratização (Priority: P1)

Como engenheiro de dados, quero executar um job PySpark (simulando AWS Glue) que leia o SOR do banco e gere Parquet diário, para que analistas possam consumir o extrato sem acessar o banco operacional.

**Acceptance Scenarios**:

1. **Given** o accounts-service está rodando com H2 TCP na porta 9092 e existem movimentos registrados, **When** `./run_job.sh` é executado, **Then** arquivos Parquet são gerados em `output/daily_statement/account_id=<uuid>/date=<yyyy-mm-dd>/`.

2. **Given** o job foi executado com sucesso, **When** o mesmo job é re-executado, **Then** os arquivos Parquet são sobrescritos (modo `overwrite`) sem duplicar dados.

3. **Given** não há movimentos para uma conta em um determinado dia, **Then** nenhuma partição é gerada para esse dia (sem linhas vazias).

---

### User Story 2 — Consultar posição diária (Priority: P1)

Como analista, quero consultar a posição diária de uma conta (saldo abertura, saldo fechamento, créditos, débitos) lendo diretamente os arquivos Parquet.

**Acceptance Scenarios**:

1. **Given** Parquet gerado, **When** `python query_daily.py --account <uuid>` é executado, **Then** o extrato diário completo é exibido no terminal com saldo inicial, saldo final, total de créditos e débitos por dia.

2. **Given** Parquet gerado, **When** `python query_daily.py --account <uuid> --date 2026-06-04` é executado, **Then** apenas o dia solicitado é exibido.

3. **Given** Parquet gerado, **When** `python query_daily.py --list-accounts` é executado, **Then** todos os UUIDs de contas disponíveis no data lake são listados.

---

## Modelo de Dados

### Tabelas SOR (accounts-service — H2)

| Tabela     | Papel                        |
|------------|------------------------------|
| `ACCOUNTS` | Saldo corrente por conta     |
| `MOVEMENTS` | Histórico imutável de lançamentos (SOR) |

### Schema do Parquet — `daily_statement`

| Coluna           | Tipo           | Descrição                                    |
|------------------|----------------|----------------------------------------------|
| `account_id`     | STRING         | UUID da conta (chave de partição)            |
| `date`           | DATE           | Data do extrato (chave de partição)          |
| `opening_balance`| DECIMAL(19,2)  | Saldo no início do dia                       |
| `closing_balance`| DECIMAL(19,2)  | Saldo ao final do dia                        |
| `total_credits`  | DECIMAL(19,2)  | Soma dos créditos no dia                     |
| `total_debits`   | DECIMAL(19,2)  | Soma dos débitos no dia                      |
| `movement_count` | INTEGER        | Quantidade de lançamentos no dia             |
| `transactions`   | ARRAY<STRUCT>  | Lançamentos individuais (desnormalizado)     |

### Particionamento

```
output/daily_statement/
  account_id=<uuid>/
    date=2026-06-04/
      part-00000-....parquet
```

---

## Arquitetura

```
accounts-service (H2 TCP :9092)
        │
        │ JDBC
        ▼
  glue_job.py (PySpark local)
        │
        │ Parquet (overwrite)
        ▼
  output/daily_statement/
        │
        │ pyarrow read
        ▼
  query_daily.py (CLI)
```

---

## Dependências e Pré-requisitos

| Componente        | Versão     | Papel                           |
|-------------------|------------|---------------------------------|
| accounts-service  | em execução | Fonte SOR via H2 TCP :9092     |
| Python            | ≥ 3.10     | Runtime do job                  |
| PySpark           | ≥ 3.5      | Engine de processamento         |
| PyArrow           | ≥ 14.0     | Leitura dos Parquet no query CLI|
| pandas            | ≥ 2.0      | Formatação da saída             |
| H2 JDBC driver    | ≥ 2.2      | Conexão ao banco SOR            |
