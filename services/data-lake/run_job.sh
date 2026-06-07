#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
# smanioto-bank — Data Lake: Democratização do Extrato
#
# O que este script faz:
#   1. Verifica se o accounts-service está no ar (H2 TCP na porta 9092)
#   2. Instala dependências Python se necessário (pyspark, pandas, pyarrow)
#   3. Executa o glue_job.py (PySpark local) que:
#        - Lê as tabelas ACCOUNTS e MOVEMENTS do banco operacional via JDBC
#        - Calcula a posição diária de cada conta (saldo inicial, créditos,
#          débitos, saldo final, lançamentos)
#        - Salva o resultado em Parquet particionado por account_id/date
#   4. Mostra como consultar os dados gerados
#
# Pré-requisitos:
#   - accounts-service rodando  →  ./start.sh (na raiz do projeto)
#   - Dados populados           →  ./seed.sh  (na raiz do projeto)
#   - Python 3 instalado
#
# Saída gerada:
#   services/data-lake/output/daily_statement/
#     account_id=<uuid>/
#       date=<yyyy-mm-dd>/
#         part-00000-....parquet
# ──────────────────────────────────────────────────────────────────────────────

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Garante que JAVA_HOME aponta para o Java em uso — necessário para o PySpark subir
JAVA_HOME=$(java -XshowSettings:properties -version 2>&1 | grep 'java.home' | awk -F'= ' '{print $2}')
export JAVA_HOME

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

p() { printf "%b\n" "$*"; }

# ──────────────────────────────────────────────
# Banner
# ──────────────────────────────────────────────
printf "\n"
p "${BOLD}╔══════════════════════════════════════════════╗${RESET}"
p "${BOLD}║   📊  smanioto-bank — Data Lake              ║${RESET}"
p "${BOLD}║   Democratização do Extrato (PySpark local)  ║${RESET}"
p "${BOLD}╚══════════════════════════════════════════════╝${RESET}"
printf "\n"

# ──────────────────────────────────────────────
# 1. Verificar accounts-service (H2 TCP :9092)
# ──────────────────────────────────────────────
p "${BOLD}[1/3] Verificando accounts-service (H2 TCP :9092)...${RESET}"

if ! lsof -ti ":9092" >/dev/null 2>&1; then
    p "  ${RED}✗ accounts-service não está rodando na porta 9092.${RESET}"
    p "  ${YELLOW}► Execute na raiz do projeto:${RESET}"
    p "      ${CYAN}./start.sh${RESET}   # sobe todos os serviços"
    p "      ${CYAN}./seed.sh${RESET}    # popula com dados de teste (opcional)"
    exit 1
fi

p "  ${GREEN}✓ accounts-service no ar — H2 TCP acessível na porta 9092${RESET}"
printf "\n"

# ──────────────────────────────────────────────
# 2. Instalar dependências Python
# ──────────────────────────────────────────────
p "${BOLD}[2/3] Verificando dependências Python...${RESET}"

MISSING=0
for pkg in pyspark pandas pyarrow; do
    if ! python3 -c "import $pkg" 2>/dev/null; then
        MISSING=1
        break
    fi
done

if [ "$MISSING" -eq 1 ]; then
    p "  ${YELLOW}► Instalando dependências (pyspark, pandas, pyarrow)...${RESET}"
    pip3 install -r requirements.txt
    p "  ${GREEN}✓ Dependências instaladas${RESET}"
else
    p "  ${GREEN}✓ Dependências já instaladas${RESET}"
fi
printf "\n"

# ──────────────────────────────────────────────
# 3. Executar o Glue Job (PySpark)
# ──────────────────────────────────────────────
p "${BOLD}[3/3] Executando Glue Job (PySpark)...${RESET}"
p "  ${CYAN}Lendo ACCOUNTS e MOVEMENTS via JDBC → calculando posição diária → salvando Parquet${RESET}"
printf "\n"

python3 glue_job.py --output ./output

# ──────────────────────────────────────────────
# Resultado
# ──────────────────────────────────────────────
printf "\n"
p "${GREEN}${BOLD}╔══════════════════════════════════════════════╗${RESET}"
p "${GREEN}${BOLD}║   ✅  Parquet gerado com sucesso!            ║${RESET}"
p "${GREEN}${BOLD}╚══════════════════════════════════════════════╝${RESET}"
printf "\n"
p "  ${BOLD}Saída:${RESET} ${CYAN}$SCRIPT_DIR/output/daily_statement/${RESET}"
printf "\n"
p "  ${BOLD}Como consultar os dados:${RESET}"
printf "\n"
p "  ${YELLOW}# Listar contas disponíveis no data lake${RESET}"
p "  ${CYAN}python3 query_daily.py --list-accounts${RESET}"
printf "\n"
p "  ${YELLOW}# Extrato diário completo de uma conta${RESET}"
p "  ${CYAN}python3 query_daily.py --account <uuid>${RESET}"
printf "\n"
p "  ${YELLOW}# Extrato de uma data específica${RESET}"
p "  ${CYAN}python3 query_daily.py --account <uuid> --date YYYY-MM-DD${RESET}"
printf "\n"
p "  ${YELLOW}# Extrato de um período${RESET}"
p "  ${CYAN}python3 query_daily.py --account <uuid> --from YYYY-MM-DD --to YYYY-MM-DD${RESET}"
printf "\n"
