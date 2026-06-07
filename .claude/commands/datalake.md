Executa o pipeline de democratização de dados (data-lake) e consulta os resultados.

Argumento em $ARGUMENTS: `run`, `query <uuid>`, `list`, ou `query <uuid> --date YYYY-MM-DD`.

**Pré-requisito:** o `accounts-service` precisa estar rodando (`./start.sh` na raiz) pois o job lê via JDBC H2 TCP na porta 9092.

---

## Rodar o job (gera os Parquets)

```bash
cd services/data-lake
./run_job.sh
```

O job PySpark (`glue_job.py`) lê as tabelas `ACCOUNTS` e `MOVEMENTS` do accounts-service via JDBC, calcula a visão diária por conta e salva em `output/daily_statement/` particionado por `account_id/date`.

---

## Consultar os dados gerados

```bash
cd services/data-lake

# Listar contas disponíveis no data lake
python3 query_daily.py --list-accounts

# Extrato diário completo de uma conta
python3 query_daily.py --account <uuid>

# Extrato de uma data específica
python3 query_daily.py --account <uuid> --date 2026-06-04

# Extrato de um período
python3 query_daily.py --account <uuid> --from 2026-06-01 --to 2026-06-04
```

---

## Instalar dependências (primeira vez)

```bash
cd services/data-lake
pip3 install -r requirements.txt
```

Dependências: `pyspark>=3.5.0`, `pandas>=2.0.0`, `pyarrow>=14.0.0`.

O H2 JDBC driver é reutilizado do cache Maven (`~/.m2`) — não precisa baixar separado.

---

## Estrutura de saída

```
services/data-lake/output/daily_statement/
  account_id=<uuid>/
    date=<yyyy-mm-dd>/
      part-00000-....parquet
```

Cada partição contém: `opening_balance`, `closing_balance`, `total_credits`, `total_debits`, `movement_count`, `transactions[]`.

Decisão arquitetural documentada no ADR-004 (`docs/adr/ADR-004-democratizacao-extrato-parquet.md`).
