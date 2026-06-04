"""
Consulta de Extrato Diário — lê os arquivos Parquet gerados pelo glue_job.py
e exibe a posição diária por conta.

Uso:
  python query_daily.py --account <uuid>
  python query_daily.py --account <uuid> --date 2026-06-04
  python query_daily.py --account <uuid> --from 2026-06-01 --to 2026-06-04
  python query_daily.py --list-accounts
"""

import argparse
import os
import sys

import pandas as pd
import pyarrow.dataset as ds


OUTPUT_PATH = os.path.join(os.path.dirname(__file__), "output", "daily_statement")


def load_dataset(account_id: str | None = None, date_from: str | None = None, date_to: str | None = None) -> pd.DataFrame:
    if not os.path.exists(OUTPUT_PATH):
        print(f"[ERRO] Diretório de saída não encontrado: {OUTPUT_PATH}")
        print("Execute 'python glue_job.py' primeiro para gerar os arquivos Parquet.")
        sys.exit(1)

    dataset = ds.dataset(OUTPUT_PATH, format="parquet", partitioning="hive")
    filters = []

    if account_id:
        filters.append(("account_id", "=", account_id))
    if date_from:
        filters.append(("date", ">=", date_from))
    if date_to:
        filters.append(("date", "<=", date_to))

    table = dataset.to_table(filter=_build_filter(filters) if filters else None)
    df = table.to_pandas()

    if df.empty:
        return df

    df = df.sort_values(["account_id", "date"])
    return df


def _build_filter(conditions):
    import pyarrow.compute as pc
    expr = None
    for col, op, val in conditions:
        field = ds.field(col)
        if op == "=":
            cond = field == val
        elif op == ">=":
            cond = field >= val
        elif op == "<=":
            cond = field <= val
        else:
            raise ValueError(f"Operador desconhecido: {op}")
        expr = cond if expr is None else expr & cond
    return expr


def list_accounts() -> None:
    if not os.path.exists(OUTPUT_PATH):
        print(f"[ERRO] {OUTPUT_PATH} não existe. Rode o glue_job.py primeiro.")
        sys.exit(1)
    dataset = ds.dataset(OUTPUT_PATH, format="parquet", partitioning="hive")
    df = dataset.to_table(columns=["account_id"]).to_pandas().drop_duplicates()
    print("\nContas disponíveis no data lake:")
    for acc in df["account_id"].tolist():
        print(f"  {acc}")


def print_daily(df: pd.DataFrame) -> None:
    if df.empty:
        print("Nenhum dado encontrado para os filtros informados.")
        return

    money = lambda v: f"R$ {float(v):>12.2f}"

    for account_id, group in df.groupby("account_id"):
        print(f"\n{'=' * 70}")
        print(f"  Conta: {account_id}")
        print(f"{'=' * 70}")
        print(f"  {'Data':<12}  {'Saldo Inicial':>14}  {'Créditos':>12}  {'Débitos':>12}  {'Saldo Final':>12}  {'Mov':>4}")
        print(f"  {'-' * 66}")

        for _, row in group.iterrows():
            date_str = str(row["date"])[:10]
            print(
                f"  {date_str:<12}  "
                f"{money(row['opening_balance'])}  "
                f"{money(row['total_credits'])}  "
                f"{money(row['total_debits'])}  "
                f"{money(row['closing_balance'])}  "
                f"{int(row['movement_count']):>4}"
            )

        # Detalhes das transações se solicitado
        if "transactions" in df.columns:
            print(f"\n  {'Lançamentos detalhados':}")
            print(f"  {'-' * 66}")
            for _, row in group.iterrows():
                if row["transactions"]:
                    date_str = str(row["date"])[:10]
                    print(f"  [{date_str}]")
                    for t in row["transactions"]:
                        tipo = t.get("type", "")
                        sinal = "+" if tipo == "CREDIT" else "-"
                        valor = float(t.get("amount", 0))
                        desc = t.get("description", "")
                        print(f"    {sinal}  R$ {valor:>10.2f}  {desc}")


def main():
    parser = argparse.ArgumentParser(description="Consulta de extrato diário via Parquet")
    parser.add_argument("--account", help="UUID da conta a consultar")
    parser.add_argument("--date", help="Data específica (YYYY-MM-DD)")
    parser.add_argument("--from", dest="date_from", help="Data inicial (YYYY-MM-DD)")
    parser.add_argument("--to", dest="date_to", help="Data final (YYYY-MM-DD)")
    parser.add_argument("--list-accounts", action="store_true", help="Lista todas as contas no data lake")
    args = parser.parse_args()

    if args.list_accounts:
        list_accounts()
        return

    if not args.account:
        parser.print_help()
        sys.exit(1)

    date_from = args.date or args.date_from
    date_to = args.date or args.date_to

    df = load_dataset(account_id=args.account, date_from=date_from, date_to=date_to)
    print_daily(df)


if __name__ == "__main__":
    main()
