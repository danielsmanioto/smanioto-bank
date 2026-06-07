"""
Glue Job Simulation — Democratização do Extrato

Simula um AWS Glue job que lê a tabela SOR (movements) do accounts-service
via JDBC, calcula a visão diária por conta e salva em Parquet particionado.

Pré-requisito: accounts-service rodando com H2 TCP server na porta 9092.
Execute com: python glue_job.py [--output ./output]
"""

import argparse
import glob
import os
import sys

# Workaround: Java 17+ removeu Subject.getSubject() usado pelo Hadoop/Spark 3.5
# Ver BACKLOG.md DT-001 para soluções definitivas
os.environ.setdefault(
    "JAVA_TOOL_OPTIONS",
    "--add-opens java.base/javax.security.auth=ALL-UNNAMED"
)

from pyspark.sql import SparkSession
from pyspark.sql import functions as F
from pyspark.sql.window import Window


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def find_h2_jar() -> str:
    m2 = os.path.expanduser("~/.m2/repository/com/h2database/h2")
    jars = glob.glob(os.path.join(m2, "**", "*.jar"), recursive=True)
    # Prefer non-sources/javadoc jars
    jars = [j for j in jars if "sources" not in j and "javadoc" not in j]
    if not jars:
        print("[ERROR] H2 JDBC driver não encontrado em ~/.m2. "
              "Execute 'mvn -f ../../services/accounts-service/pom.xml dependency:resolve' primeiro.")
        sys.exit(1)
    return jars[0]


def build_spark(h2_jar: str) -> SparkSession:
    return (
        SparkSession.builder
        .appName("smanioto-bank-democratizacao-extrato")
        .master("local[*]")
        .config("spark.jars", h2_jar)
        .config("spark.sql.session.timeZone", "America/Sao_Paulo")
        .getOrCreate()
    )


# ---------------------------------------------------------------------------
# Leitura das tabelas SOR via JDBC
# ---------------------------------------------------------------------------

JDBC_URL = "jdbc:h2:tcp://localhost:9092/mem:accountsdb"
JDBC_PROPS = {
    "driver": "org.h2.Driver",
    "user": "sa",
    "password": "",
}


def read_table(spark: SparkSession, table: str):
    return spark.read.jdbc(url=JDBC_URL, table=table, properties=JDBC_PROPS)


# ---------------------------------------------------------------------------
# Transformação: visão diária por conta
# ---------------------------------------------------------------------------

def compute_daily_statement(accounts, movements):
    """
    Para cada (account_id, date):
      - total_credits / total_debits / movement_count
      - opening_balance / closing_balance reconstruídos a partir do saldo atual

    Estratégia de saldo:
      current_balance (hoje) é conhecido.
      Percorremos os dias de forma regressiva acumulando net_change.
      closing_balance[D] = current_balance - Σ net_change dos dias APÓS D
      opening_balance[D] = closing_balance[D] - net_change[D]
    """
    # Normaliza nomes para lowercase
    acc = accounts.select(
        F.col("ID").alias("account_id"),
        F.col("BALANCE").alias("current_balance"),
    )

    mov = movements.select(
        F.col("ACCOUNTID").alias("account_id"),
        F.col("ID").alias("movement_id"),
        F.col("TYPE").alias("type"),
        F.col("AMOUNT").alias("amount"),
        F.col("DESCRIPTION").alias("description"),
        F.col("CREATEDAT").cast("timestamp").alias("created_at"),
    ).withColumn("date", F.to_date(F.col("created_at")))

    # Agrega por dia
    daily = mov.groupBy("account_id", "date").agg(
        F.sum(F.when(F.col("type") == "CREDIT", F.col("amount")).otherwise(F.lit(0)))
          .alias("total_credits"),
        F.sum(F.when(F.col("type") == "DEBIT", F.col("amount")).otherwise(F.lit(0)))
          .alias("total_debits"),
        F.count("*").alias("movement_count"),
    ).withColumn("net_change", F.col("total_credits") - F.col("total_debits"))

    # Junta com saldo atual
    daily = daily.join(acc, "account_id")

    # Acumulado do net_change do dia mais recente até D (inclusive), ordenado desc
    w_desc = (
        Window.partitionBy("account_id")
        .orderBy(F.col("date").desc())
        .rowsBetween(Window.unboundedPreceding, Window.currentRow)
    )
    daily = daily.withColumn("cum_net_desc", F.sum("net_change").over(w_desc))

    # closing_balance[D] = current_balance - (cum_net_desc[D] - net_change[D])
    daily = daily.withColumn(
        "closing_balance",
        F.col("current_balance") - (F.col("cum_net_desc") - F.col("net_change")),
    ).withColumn(
        "opening_balance",
        F.col("closing_balance") - F.col("net_change"),
    )

    # Coleta os lançamentos individuais como array de structs
    movements_struct = mov.groupBy("account_id", "date").agg(
        F.collect_list(
            F.struct(
                F.col("movement_id").alias("id"),
                F.col("type"),
                F.col("amount"),
                F.col("description"),
                F.col("created_at"),
            )
        ).alias("transactions")
    )

    result = daily.join(movements_struct, ["account_id", "date"]).select(
        "account_id",
        "date",
        F.round("opening_balance", 2).alias("opening_balance"),
        F.round("closing_balance", 2).alias("closing_balance"),
        F.round("total_credits", 2).alias("total_credits"),
        F.round("total_debits", 2).alias("total_debits"),
        "movement_count",
        "transactions",
    )

    return result


# ---------------------------------------------------------------------------
# Escrita em Parquet particionado
# ---------------------------------------------------------------------------

def write_parquet(df, output_path: str):
    out = os.path.join(output_path, "daily_statement")
    print(f"[INFO] Salvando Parquet em: {os.path.abspath(out)}")
    (
        df.write
        .partitionBy("account_id", "date")
        .mode("overwrite")
        .parquet(out)
    )
    print("[INFO] Job concluído com sucesso.")


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(
        description="Glue Job — Democratização do Extrato (simulação local)"
    )
    parser.add_argument("--output", default="./output", help="Diretório de saída dos arquivos Parquet")
    args = parser.parse_args()

    h2_jar = find_h2_jar()
    print(f"[INFO] Usando H2 JDBC driver: {h2_jar}")

    spark = build_spark(h2_jar)
    spark.sparkContext.setLogLevel("WARN")

    print("[INFO] Lendo tabelas SOR do accounts-service...")
    accounts = read_table(spark, "ACCOUNTS")
    movements = read_table(spark, "MOVEMENTS")

    print(f"[INFO] Contas: {accounts.count()} | Movimentos: {movements.count()}")

    daily = compute_daily_statement(accounts, movements)
    write_parquet(daily, args.output)

    spark.stop()


if __name__ == "__main__":
    main()
