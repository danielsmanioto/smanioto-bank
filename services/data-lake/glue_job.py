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
import subprocess
import sys


def _java_major_version(java_bin: str) -> int:
    try:
        r = subprocess.run([java_bin, "-version"], capture_output=True, text=True)
        for line in r.stderr.splitlines():
            if "version" in line:
                # "openjdk version \"17.0.3\"" or "openjdk version \"25.0.1\""
                import re
                m = re.search(r'"(\d+)', line)
                if m:
                    return int(m.group(1))
    except Exception:
        pass
    return 0


def _fix_java_home():
    """Garante que JAVA_HOME aponta para Java 17 ou 20.
    Hadoop 3.4.x usa Subject.getSubject() que lança UnsupportedOperationException no Java 21+.
    """
    # Verifica o Java atual
    java_home = os.environ.get("JAVA_HOME", "")
    current_java = os.path.join(java_home, "bin", "java") if java_home else "java"
    if not os.path.isfile(current_java):
        current_java = "java"

    version = _java_major_version(current_java)
    if 17 <= version <= 20:
        # Java compatível — apenas garante que JAVA_HOME está correto
        if not java_home or not os.path.isfile(os.path.join(java_home, "bin", "java")):
            r = subprocess.run([current_java, "-XshowSettings:properties", "-version"],
                               capture_output=True, text=True)
            for line in r.stderr.splitlines():
                if "java.home" in line:
                    os.environ["JAVA_HOME"] = line.split("=", 1)[1].strip()
        return

    # Java 21+ (ou não detectado): busca Java 17 via java_home (macOS) ou candidatos conhecidos
    print(f"[WARN] Java {version} detectado. Hadoop 3.4.x requer Java 17-20. Buscando Java 17...")
    candidates = []
    try:
        r = subprocess.run(["/usr/libexec/java_home", "-v", "17"],
                           capture_output=True, text=True)
        if r.returncode == 0 and r.stdout.strip():
            candidates.append(r.stdout.strip())
    except FileNotFoundError:
        pass
    try:
        r = subprocess.run(["/usr/libexec/java_home", "-v", "20"],
                           capture_output=True, text=True)
        if r.returncode == 0 and r.stdout.strip():
            candidates.append(r.stdout.strip())
    except FileNotFoundError:
        pass

    for home in candidates:
        java_bin = os.path.join(home, "bin", "java")
        v = _java_major_version(java_bin)
        if 17 <= v <= 20:
            print(f"[INFO] Usando Java {v} em: {home}")
            os.environ["JAVA_HOME"] = home
            return

    print(f"[ERROR] Java 17 ou 20 não encontrado. O job pode falhar com Java {version}+.")


_fix_java_home()

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
    # spark.driver.extraClassPath evita que o Spark passe o JAR pelo Hadoop FileSystem
    # (que chama Subject.getSubject(), removido no Java 21+)
    return (
        SparkSession.builder
        .appName("smanioto-bank-democratizacao-extrato")
        .master("local[*]")
        .config("spark.driver.extraClassPath", h2_jar)
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


def _jdbc(spark: SparkSession, query: str):
    return spark.read.jdbc(url=JDBC_URL, table=f"({query}) t", properties=JDBC_PROPS)


def read_accounts(spark: SparkSession):
    # H2 retorna UUID como binário via JDBC; CAST para VARCHAR produz o formato legível.
    return _jdbc(spark, "SELECT CAST(ID AS VARCHAR) AS ID, BALANCE FROM ACCOUNTS")


def read_movements(spark: SparkSession):
    # JPA (SpringPhysicalNamingStrategy) converte camelCase → snake_case.
    # H2 2.x persiste @Enumerated(EnumType.STRING) como ENUM('CREDIT','DEBIT'),
    # tipo SQL OTHER que o Spark não mapeia; CAST para VARCHAR resolve.
    return _jdbc(spark,
        "SELECT CAST(ID AS VARCHAR) AS ID,"
        " CAST(ACCOUNT_ID AS VARCHAR) AS ACCOUNT_ID,"
        " CAST(TRANSFER_ID AS VARCHAR) AS TRANSFER_ID,"
        " CAST(TYPE AS VARCHAR) AS TYPE,"
        " AMOUNT, DESCRIPTION, CREATED_AT"
        " FROM MOVEMENTS"
    )


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
        F.col("ACCOUNT_ID").alias("account_id"),
        F.col("ID").alias("movement_id"),
        F.col("TYPE").alias("type"),
        F.col("AMOUNT").alias("amount"),
        F.col("DESCRIPTION").alias("description"),
        F.col("CREATED_AT").cast("timestamp").alias("created_at"),
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
    accounts = read_accounts(spark)
    movements = read_movements(spark)

    print(f"[INFO] Contas: {accounts.count()} | Movimentos: {movements.count()}")

    daily = compute_daily_statement(accounts, movements)
    write_parquet(daily, args.output)

    spark.stop()


if __name__ == "__main__":
    main()
