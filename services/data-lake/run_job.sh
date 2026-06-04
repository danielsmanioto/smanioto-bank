#!/usr/bin/env bash
# Executa o Glue Job local de democratização do extrato.
# Pré-requisitos: accounts-service rodando + Python 3 + dependências instaladas.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "============================================================"
echo "  smanioto-bank — Glue Job: Democratização do Extrato"
echo "============================================================"

# Instala dependências se necessário
if ! python3 -c "import pyspark" 2>/dev/null; then
    echo "[INFO] Instalando dependências Python..."
    pip3 install -r requirements.txt
fi

echo "[INFO] Iniciando job PySpark..."
python3 glue_job.py --output ./output

echo ""
echo "[OK] Parquet gerado em: $SCRIPT_DIR/output/daily_statement/"
echo ""
echo "Para consultar:"
echo "  python3 query_daily.py --list-accounts"
echo "  python3 query_daily.py --account <uuid>"
echo "  python3 query_daily.py --account <uuid> --date YYYY-MM-DD"
