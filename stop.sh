#!/usr/bin/env bash

ROOT="$(cd "$(dirname "$0")" && pwd)"
PID_FILE="$ROOT/.pids"

# ──────────────────────────────────────────────
# Cores (usando printf — portável em qualquer shell)
# ──────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
RESET='\033[0m'

p() { printf "%b\n" "$*"; }

printf "\n"
p "${BOLD}╔══════════════════════════════════════╗${RESET}"
p "${BOLD}║        🛑  smanioto-bank             ║${RESET}"
p "${BOLD}╚══════════════════════════════════════╝${RESET}"
printf "\n"

if [ ! -f "$PID_FILE" ] || [ ! -s "$PID_FILE" ]; then
  p "  ${YELLOW}Nenhum serviço em execução encontrado (arquivo .pids ausente ou vazio).${RESET}"
  printf "\n"
  exit 0
fi

p "${BOLD}Encerrando serviços...${RESET}"
printf "\n"

while IFS= read -r pid; do
  [ -z "$pid" ] && continue
  if kill -0 "$pid" 2>/dev/null; then
    kill -TERM "$pid" 2>/dev/null
    p "  ${RED}✗${RESET} Processo ${BOLD}$pid${RESET} encerrado"
  else
    p "  ${YELLOW}-${RESET} Processo ${BOLD}$pid${RESET} já não estava rodando"
  fi
done < "$PID_FILE"

rm -f "$PID_FILE"

printf "\n"
p "${GREEN}${BOLD}╔══════════════════════════════════════╗${RESET}"
p "${GREEN}${BOLD}║   ✅  Todos os serviços parados!     ║${RESET}"
p "${GREEN}${BOLD}╚══════════════════════════════════════╝${RESET}"
printf "\n"
