#!/usr/bin/env bash
# Agrega e exibe logs de todos os serviços smanioto-bank
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$ROOT/.logs"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

p() { printf "%b\n" "$*"; }

SERVICES=(auth-service people-service accounts-service frontend)
FOLLOW=false
FILTER=""
LINES=50
TARGET=""

usage() {
  p "${BOLD}Uso:${RESET} ./logs.sh [opções]"
  p ""
  p "${BOLD}Opções:${RESET}"
  p "  ${CYAN}-f${RESET}           Seguir logs em tempo real (tail -f)"
  p "  ${CYAN}-n LINHAS${RESET}    Número de linhas por serviço (padrão: 50)"
  p "  ${CYAN}-g TEXTO${RESET}     Filtrar por texto (grep)"
  p "  ${CYAN}-s SERVIÇO${RESET}   Mostrar apenas um serviço"
  p "                 Valores: auth, people, accounts, frontend"
  p "  ${CYAN}-e${RESET}           Mostrar apenas linhas de ERROR/WARN"
  p "  ${CYAN}-h${RESET}           Ajuda"
  p ""
  p "${BOLD}Exemplos:${RESET}"
  p "  ./logs.sh                  # últimas 50 linhas de todos"
  p "  ./logs.sh -f               # seguir todos em tempo real"
  p "  ./logs.sh -s auth -f       # seguir apenas auth-service"
  p "  ./logs.sh -g 'Exception'   # buscar exceções em todos"
  p "  ./logs.sh -e               # apenas erros e warnings"
  exit 0
}

ERRORS_ONLY=false

while getopts "fn:g:s:eh" opt; do
  case $opt in
    f) FOLLOW=true ;;
    n) LINES="$OPTARG" ;;
    g) FILTER="$OPTARG" ;;
    s) TARGET="$OPTARG" ;;
    e) ERRORS_ONLY=true ;;
    h) usage ;;
    *) usage ;;
  esac
done

if [ ! -d "$LOG_DIR" ]; then
  p "${RED}Diretório .logs não encontrado. Execute ./start.sh primeiro.${RESET}"
  exit 1
fi

# Resolve nome curto → nome do arquivo
resolve_service() {
  case "$1" in
    auth)     echo "auth-service" ;;
    people)   echo "people-service" ;;
    accounts) echo "accounts-service" ;;
    frontend) echo "frontend" ;;
    *)        echo "$1" ;;
  esac
}

# Lista de serviços a exibir
if [ -n "$TARGET" ]; then
  SERVICES=("$(resolve_service "$TARGET")")
fi

# ── Modo follow (-f): usa tail -f com prefixo colorido ─────────────────────
if $FOLLOW; then
  p "${BOLD}Seguindo logs em tempo real... ${YELLOW}(Ctrl+C para parar)${RESET}"
  p ""

  COLORS=("$GREEN" "$CYAN" "$YELLOW" "$RED")
  IDX=0
  PIDS=()

  for svc in "${SERVICES[@]}"; do
    log="$LOG_DIR/$svc.log"
    color="${COLORS[$IDX]}"
    IDX=$(( (IDX + 1) % 4 ))

    if [ ! -f "$log" ]; then
      p "${RED}[${svc}]${RESET} log não encontrado: $log"
      continue
    fi

    if $ERRORS_ONLY; then
      tail -f "$log" | grep --line-buffered -iE 'error|warn|exception|caused by' \
        | awk -v c="$color" -v r="$RESET" -v s="[$svc]" '{print c s r " " $0}' &
    elif [ -n "$FILTER" ]; then
      tail -f "$log" | grep --line-buffered "$FILTER" \
        | awk -v c="$color" -v r="$RESET" -v s="[$svc]" '{print c s r " " $0}' &
    else
      tail -f "$log" \
        | awk -v c="$color" -v r="$RESET" -v s="[$svc]" '{print c s r " " $0}' &
    fi
    PIDS+=($!)
  done

  trap 'kill "${PIDS[@]}" 2>/dev/null; exit 0' INT TERM
  wait
  exit 0
fi

# ── Modo snapshot: últimas N linhas de cada serviço ─────────────────────────
for svc in "${SERVICES[@]}"; do
  log="$LOG_DIR/$svc.log"

  p "${BOLD}╔══════════════════════════════════════════╗${RESET}"
  printf "%b" "${BOLD}║  $svc${RESET}"
  printf "%b\n" "${BOLD}$(printf '%*s' $((42 - ${#svc} - 2)) '')║${RESET}"
  p "${BOLD}╚══════════════════════════════════════════╝${RESET}"

  if [ ! -f "$log" ]; then
    p "${RED}  log não encontrado: $log${RESET}"
    echo ""
    continue
  fi

  content=$(tail -n "$LINES" "$log")

  if $ERRORS_ONLY; then
    content=$(echo "$content" | grep -iE 'error|warn|exception|caused by' || true)
  elif [ -n "$FILTER" ]; then
    content=$(echo "$content" | grep "$FILTER" || true)
  fi

  if [ -z "$content" ]; then
    p "${YELLOW}  (sem resultados para o filtro aplicado)${RESET}"
  else
    echo "$content" | while IFS= read -r line; do
      if echo "$line" | grep -qiE 'error|exception|caused by'; then
        printf "%b\n" "${RED}  $line${RESET}"
      elif echo "$line" | grep -qiE 'warn'; then
        printf "%b\n" "${YELLOW}  $line${RESET}"
      else
        echo "  $line"
      fi
    done
  fi

  echo ""
done
