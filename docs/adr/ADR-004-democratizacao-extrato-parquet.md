# ADR-004 — Democratização do Extrato via PySpark + Parquet

**Status:** Aceito  
**Data:** 2026-06-04

## Contexto

O accounts-service registra todas as movimentações bancárias na tabela `MOVEMENTS` (SOR — System of Record). Para habilitar análises históricas — como posição diária de saldo, totais de crédito/débito por período — sem impactar o banco operacional, precisamos de uma camada de dados separada que:

1. Leia os dados do SOR de forma assíncrona.
2. Produza uma visão agregada por dia (extrato diário).
3. Permita consultas ad-hoc por analistas sem acesso direto ao banco transacional.

As opções avaliadas foram:

- **REST endpoint de exportação** — adicionar `/internal/export/movements` no accounts-service; simples, mas cria acoplamento e pressão de carga no serviço operacional.
- **JDBC direto ao H2 via TCP** — H2 suporta modo TCP server; o job conecta diretamente ao banco, sem alterar a API do serviço.
- **Exportação para arquivo intermediário (CSV/JSON)** — requer etapa extra de dump manual; não simula bem o padrão Glue real.

## Decisão

Habilitar o **H2 TCP Server** no accounts-service (porta 9092) e usar um script **PySpark local** (simulando AWS Glue) que lê via JDBC, processa e salva em **Parquet** particionado por `account_id/date`.

## Justificativa

A abordagem JDBC → PySpark → Parquet é a mais fiel ao padrão real de Data Lake em nuvem (RDS → Glue → S3/Parquet → Athena). Isso permite aprender o pipeline completo de democratização sem alterar contratos de API do serviço operacional. O H2 TCP server é uma feature nativa da biblioteca H2, sem dependência adicional.

## Estrutura Implementada

```
services/data-lake/
  glue_job.py        — PySpark: lê JDBC → calcula visão diária → salva Parquet
  query_daily.py     — CLI: consulta Parquet por conta e/ou data
  run_job.sh         — Script de execução com verificação de dependências
  requirements.txt   — pyspark, pandas, pyarrow
  output/
    daily_statement/
      account_id=<uuid>/
        date=<yyyy-mm-dd>/
          part-XXXXX.parquet
```

## Cálculo de Saldo

- **closing_balance[D]** = `current_balance` − Σ `net_change` para dias posteriores a D  
- **opening_balance[D]** = `closing_balance[D]` − `net_change[D]`  
- `net_change` = `total_credits` − `total_debits` do dia  

Implementado com `Window.partitionBy("account_id").orderBy("date".desc)` e `sum().over(w)` acumulado.

## Consequências

**Positivas:**
- Nenhum endpoint novo exposto no accounts-service.
- Parquet é imutável e auto-documentado (schema embarcado).
- Particionamento por `account_id/date` permite predicate pushdown eficiente em Athena/Spark.
- Simula fielmente o padrão Glue → S3 de produção.

**Negativas/Limitações:**
- H2 in-memory ainda requer que o accounts-service esteja rodando durante a execução do job.
- Sem incremental: o job sempre reprocessa todo o histórico (modo `overwrite`).
- Em produção real, o banco seria PostgreSQL em servidor dedicado e o job rodaria em horário de baixo tráfego.

## Quando revisar

Ao migrar para PostgreSQL, substituir a `JDBC_URL` do H2 TCP pela URL do Postgres e, opcionalmente, adicionar controle incremental (watermark por `CREATEDAT`).
