#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$ROOT/.logs"
PID_FILE="$ROOT/.pids"

# ──────────────────────────────────────────────
# Cores
# ──────────────────────────────────────────────
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

# ──────────────────────────────────────────────
# Verificar pré-requisitos
# ──────────────────────────────────────────────
if ! command -v java &>/dev/null; then
  echo "Erro: Java não encontrado. Instale o JDK 17+."
  exit 1
fi
if ! command -v mvn &>/dev/null; then
  echo "Erro: Maven não encontrado. Instale o Maven 3.9+."
  exit 1
fi
if ! command -v python3 &>/dev/null; then
  echo "Erro: Python 3 não encontrado."
  exit 1
fi

# ──────────────────────────────────────────────
# Preparar diretórios e PID file
# ──────────────────────────────────────────────
mkdir -p "$LOG_DIR"
> "$PID_FILE"

# ──────────────────────────────────────────────
# Funções auxiliares
# ──────────────────────────────────────────────
build_service() {
  local name=$1
  local dir=$2
  echo -e "  ${YELLOW}►${RESET} Compilando ${BOLD}$name${RESET}..."
  (cd "$dir" && mvn package -q -DskipTests) || {
    echo "  Falha ao compilar $name. Verifique $LOG_DIR/$name-build.log"
    exit 1
  }
}

start_java_service() {
  local name=$1
  local dir=$2
  local port=$3
  local jar
  jar=$(ls "$dir/target/"*.jar 2>/dev/null | grep -v 'original' | head -1)
  java -jar "$jar" > "$LOG_DIR/$name.log" 2>&1 &
  local pid=$!
  echo "$pid" >> "$PID_FILE"
  echo -e "  ${GREEN}✓${RESET} ${BOLD}$name${RESET} iniciado ${CYAN}(PID $pid — porta $port)${RESET}"
}

# ──────────────────────────────────────────────
# Banner
# ──────────────────────────────────────────────
echo ""
echo -e "${BOLD}╔══════════════════════════════════════╗${RESET}"
echo -e "${BOLD}║        🏦  smanioto-bank             ║${RESET}"
echo -e "${BOLD}╚══════════════════════════════════════╝${RESET}"
echo ""
echo -e "${BOLD}[1/2] Compilando serviços Java...${RESET}"
echo ""

build_service "auth-service"     "$ROOT/services/auth-service"
build_service "people-service"   "$ROOT/services/people-service"
build_service "accounts-service" "$ROOT/services/accounts-service"

echo ""
echo -e "${BOLD}[2/2] Iniciando serviços...${RESET}"
echo ""

start_java_service "auth-service"     "$ROOT/services/auth-service"     8080
start_java_service "people-service"   "$ROOT/services/people-service"   8081
start_java_service "accounts-service" "$ROOT/services/accounts-service" 8082

echo -e "  ${YELLOW}►${RESET} Aguardando Spring Boot inicializar (8s)..."
sleep 8

# Frontend
cd "$ROOT/services/frontend"
python3 -m http.server 3000 > "$LOG_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!
echo "$FRONTEND_PID" >> "$PID_FILE"
echo -e "  ${GREEN}✓${RESET} ${BOLD}frontend${RESET} iniciado ${CYAN}(PID $FRONTEND_PID — porta 3000)${RESET}"
cd "$ROOT"

# ──────────────────────────────────────────────
# Resumo
# ──────────────────────────────────────────────
echo ""
echo -e "${GREEN}${BOLD}╔══════════════════════════════════════╗${RESET}"
echo -e "${GREEN}${BOLD}║   ✅  Todos os serviços no ar!       ║${RESET}"
echo -e "${GREEN}${BOLD}╚══════════════════════════════════════╝${RESET}"
echo ""
echo -e "  ${BOLD}Frontend   →${RESET} ${CYAN}http://localhost:3000${RESET}"
echo -e "  ${BOLD}Auth       →${RESET} ${CYAN}http://localhost:8080/auth${RESET}"
echo -e "  ${BOLD}People     →${RESET} ${CYAN}http://localhost:8081/people${RESET}"
echo -e "  ${BOLD}Accounts   →${RESET} ${CYAN}http://localhost:8082/accounts${RESET}"
echo ""
echo -e "  ${BOLD}Logs       →${RESET} .logs/"
echo -e "  ${BOLD}Para parar →${RESET} ${YELLOW}./stop.sh${RESET}"
echo ""
