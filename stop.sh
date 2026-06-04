#!/usr/bin/env bash

ROOT="$(cd "$(dirname "$0")" && pwd)"
PID_FILE="$ROOT/.pids"

# ──────────────────────────────────────────────
# Cores
# ──────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
RESET='\033[0m'

echo ""
echo -e "${BOLD}╔══════════════════════════════════════╗${RESET}"
echo -e "${BOLD}║        🛑  smanioto-bank             ║${RESET}"
echo -e "${BOLD}╚══════════════════════════════════════╝${RESET}"
echo ""

if [ ! -f "$PID_FILE" ] || [ ! -s "$PID_FILE" ]; then
  echo -e "  ${YELLOW}Nenhum serviço em execução encontrado (arquivo .pids ausente ou vazio).${RESET}"
  echo ""
  exit 0
fi

echo -e "${BOLD}Encerrando serviços...${RESET}"
echo ""

while IFS= read -r pid; do
  [ -z "$pid" ] && continue
  if kill -0 "$pid" 2>/dev/null; then
    kill -TERM "$pid" 2>/dev/null
    echo -e "  ${RED}✗${RESET} Processo ${BOLD}$pid${RESET} encerrado"
  else
    echo -e "  ${YELLOW}-${RESET} Processo ${BOLD}$pid${RESET} já não estava rodando"
  fi
done < "$PID_FILE"

rm -f "$PID_FILE"

echo ""
echo -e "${GREEN}${BOLD}╔══════════════════════════════════════╗${RESET}"
echo -e "${GREEN}${BOLD}║   ✅  Todos os serviços parados!     ║${RESET}"
echo -e "${GREEN}${BOLD}╚══════════════════════════════════════╝${RESET}"
echo ""
