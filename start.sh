#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$ROOT/.logs"
PID_FILE="$ROOT/.pids"

# ──────────────────────────────────────────────
# Cores (usando printf — portável em qualquer shell)
# ──────────────────────────────────────────────
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

p() { printf "%b\n" "$*"; }

# ──────────────────────────────────────────────
# Forçar JAVA_HOME a partir do Java em uso.
# Sempre sobrescreve — garante consistência mesmo
# que a variável já esteja definida incorretamente.
# ──────────────────────────────────────────────
JAVA_HOME=$(java -XshowSettings:properties -version 2>&1 \
  | grep 'java.home' \
  | awk -F'= ' '{print $2}')
export JAVA_HOME

# ──────────────────────────────────────────────
# Verificar pré-requisitos
# ──────────────────────────────────────────────
if ! command -v java >/dev/null 2>&1; then
  echo "Erro: Java não encontrado. Instale o JDK 17+."
  exit 1
fi
if ! command -v mvn >/dev/null 2>&1; then
  echo "Erro: Maven não encontrado. Instale o Maven 3.9+."
  exit 1
fi
if ! command -v python3 >/dev/null 2>&1; then
  echo "Erro: Python 3 não encontrado."
  exit 1
fi

# ──────────────────────────────────────────────
# Liberar portas que possam estar ocupadas
# (processos de execuções anteriores)
# ──────────────────────────────────────────────
for port in 8080 8081 8082 3000; do
  pids=$(lsof -ti ":$port" 2>/dev/null) || true
  if [ -n "$pids" ]; then
    echo "$pids" | xargs kill -9 2>/dev/null || true
    p "  ${YELLOW}►${RESET} Porta $port liberada"
  fi
done

# ──────────────────────────────────────────────
# Preparar diretórios e PID file
# ──────────────────────────────────────────────
mkdir -p "$LOG_DIR"
: > "$PID_FILE"

# ──────────────────────────────────────────────
# Funções auxiliares
# ──────────────────────────────────────────────
build_service() {
  local name=$1
  local dir=$2
  p "  ${YELLOW}►${RESET} Compilando ${BOLD}$name${RESET}..."
  (cd "$dir" && mvn package -q -DskipTests) || {
    echo "  Falha ao compilar $name. Verifique os logs do Maven acima."
    exit 1
  }
}

start_java_service() {
  local name=$1
  local dir=$2
  local port=$3
  local jar
  jar=$(ls "$dir/target/"*.jar 2>/dev/null | grep -v 'original' | head -1)
  nohup java -jar "$jar" > "$LOG_DIR/$name.log" 2>&1 &
  local pid=$!
  disown "$pid"   # desacopla do job table — sobrevive ao término do shell pai
  echo "$pid" >> "$PID_FILE"
  p "  ${GREEN}✓${RESET} ${BOLD}$name${RESET} iniciado ${CYAN}(PID $pid — porta $port)${RESET}"
}

wait_for_port() {
  local name=$1
  local port=$2
  local retries=20
  local i=0
  while [ $i -lt $retries ]; do
    if lsof -ti ":$port" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
    i=$((i + 1))
  done
  return 1
}

health_check() {
  local name=$1
  local url=$2
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 --max-time 5 "$url" 2>/dev/null) || code="000"
  if [ "$code" = "000" ]; then
    p "  ${RED}✗${RESET} ${BOLD}$name${RESET}  ${RED}sem resposta — veja .logs/$name.log${RESET}"
  else
    p "  ${GREEN}✓${RESET} ${BOLD}$name${RESET}  ${CYAN}HTTP $code${RESET}"
  fi
}

# ──────────────────────────────────────────────
# Banner
# ──────────────────────────────────────────────
printf "\n"
p "${BOLD}╔══════════════════════════════════════╗${RESET}"
p "${BOLD}║        🏦  smanioto-bank             ║${RESET}"
p "${BOLD}╚══════════════════════════════════════╝${RESET}"
printf "\n"
p "${BOLD}[1/3] Compilando serviços Java...${RESET}"
printf "\n"

build_service "auth-service"     "$ROOT/services/auth-service"
build_service "people-service"   "$ROOT/services/people-service"
build_service "accounts-service" "$ROOT/services/accounts-service"

# JWT secret para uso local — substitua por um valor gerado de forma segura em outros ambientes
export JWT_SECRET="${JWT_SECRET:-smanioto-bank-local-dev-secret-key-change-in-production}"

printf "\n"
p "${BOLD}[2/3] Iniciando serviços...${RESET}"
printf "\n"

start_java_service "auth-service"     "$ROOT/services/auth-service"     8080
start_java_service "people-service"   "$ROOT/services/people-service"   8081
start_java_service "accounts-service" "$ROOT/services/accounts-service" 8082

p "  ${YELLOW}►${RESET} Aguardando Spring Boot inicializar..."
wait_for_port "auth-service"     8080 && wait_for_port "people-service" 8081 && wait_for_port "accounts-service" 8082
sleep 2

# Frontend (Node.js — python3 no macOS sem Xcode CLT é apenas um stub)
nohup node "$ROOT/services/frontend/server.js" > "$LOG_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!
disown "$FRONTEND_PID"
echo "$FRONTEND_PID" >> "$PID_FILE"
wait_for_port "frontend" 3000
p "  ${GREEN}✓${RESET} ${BOLD}frontend${RESET} iniciado ${CYAN}(PID $FRONTEND_PID — porta 3000)${RESET}"

# ──────────────────────────────────────────────
# Health check
# ──────────────────────────────────────────────
printf "\n"
p "${BOLD}[3/3] Health check...${RESET}"
printf "\n"

health_check "auth-service"     "http://localhost:8080/auth/validate"
health_check "people-service"   "http://localhost:8081/people"
health_check "accounts-service" "http://localhost:8082/accounts"
health_check "frontend"         "http://localhost:3000"

# ──────────────────────────────────────────────
# Resumo
# ──────────────────────────────────────────────
printf "\n"
p "${GREEN}${BOLD}╔══════════════════════════════════════╗${RESET}"
p "${GREEN}${BOLD}║        Serviços disponíveis          ║${RESET}"
p "${GREEN}${BOLD}╚══════════════════════════════════════╝${RESET}"
printf "\n"
p "  ${BOLD}Frontend   →${RESET} ${CYAN}http://localhost:3000${RESET}"
p "  ${BOLD}Auth       →${RESET} ${CYAN}http://localhost:8080/auth${RESET}"
p "  ${BOLD}People     →${RESET} ${CYAN}http://localhost:8081/people${RESET}"
p "  ${BOLD}Accounts   →${RESET} ${CYAN}http://localhost:8082/accounts${RESET}"
printf "\n"
p "  ${BOLD}Logs       →${RESET} .logs/"
p "  ${BOLD}Para parar →${RESET} ${YELLOW}./stop.sh${RESET}"
printf "\n"
