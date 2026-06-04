#!/usr/bin/env bash
set -uo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

p() { printf "%b\n" "$*"; }

# ──────────────────────────────────────────────
# Dados dos 10 usuários
# CPFs: 11 dígitos, validados pelo algoritmo brasileiro
# ──────────────────────────────────────────────
USERS=(    "admin"         "alice"         "bob"           "carol"           "dave"          "eve"             "frank"         "grace"         "henry"         "iris"          )
PASSES=(   "admin"         "alice123"      "bob123"        "carol123"        "dave123"       "eve123"          "frank123"      "grace123"      "henry123"      "iris123"       )
NAMES=(    "Admin User"    "Alice Silva"   "Bob Santos"    "Carol Oliveira"  "Dave Costa"    "Eve Fernandes"   "Frank Lima"    "Grace Alves"   "Henry Souza"   "Iris Pereira"  )
EMAILS=(   "admin@bank"    "alice@bank"    "bob@bank"      "carol@bank"      "dave@bank"     "eve@bank"        "frank@bank"    "grace@bank"    "henry@bank"    "iris@bank"     )
CPFS=(     "52998224725"   "11144477735"   "40730217027"   "98765432100"     "32165498791"   "45678901249"     "78901234505"   "15935785200"   "24681357928"   "00000000191"   )
BALANCES=( "5000.00"       "3000.00"       "2000.00"       "1500.00"         "1000.00"       "800.00"          "600.00"        "400.00"        "200.00"        "100.00"        )
NUMBERS=(  "00000001-0"    "00000002-0"    "00000003-0"    "00000004-0"      "00000005-0"    "00000006-0"      "00000007-0"    "00000008-0"    "00000009-0"    "00000010-0"    )

ACCOUNT_IDS=()

json_id() { echo "$1" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4; }

# ──────────────────────────────────────────────
# Banner e verificação de serviços
# ──────────────────────────────────────────────
printf "\n"
p "${BOLD}╔══════════════════════════════════════╗${RESET}"
p "${BOLD}║   🌱  smanioto-bank — Seed de Dados  ║${RESET}"
p "${BOLD}╚══════════════════════════════════════╝${RESET}"
printf "\n"

for check in "auth-service|8080|/auth/validate" "people-service|8081|/people" "accounts-service|8082|/accounts"; do
  svc=$(echo "$check" | cut -d'|' -f1)
  port=$(echo "$check" | cut -d'|' -f2)
  path=$(echo "$check" | cut -d'|' -f3)
  code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 2 "http://localhost:$port$path" 2>/dev/null) || code="000"
  if [ "$code" = "000" ]; then
    p "${RED}✗${RESET} $svc não está respondendo. Execute ${YELLOW}./start.sh${RESET} antes do seed."
    exit 1
  fi
done

p "${GREEN}✓${RESET} Serviços no ar. Iniciando seed..."
printf "\n"

# ──────────────────────────────────────────────
# 1/3 — Criar credenciais, clientes e contas
# ──────────────────────────────────────────────
p "${BOLD}[1/3] Criando usuários, clientes PF e contas...${RESET}"
printf "\n"

for i in "${!USERS[@]}"; do
  user="${USERS[$i]}"
  pass="${PASSES[$i]}"
  name="${NAMES[$i]}"
  email="${EMAILS[$i]}"
  cpf="${CPFS[$i]}"
  balance="${BALANCES[$i]}"
  number="${NUMBERS[$i]}"

  # Credenciais (ignora conflito 409 — usuário já existe)
  auth_code=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST http://localhost:8080/auth/register \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$user\",\"password\":\"$pass\"}") || auth_code="000"

  if [ "$auth_code" != "201" ] && [ "$auth_code" != "409" ]; then
    p "  ${RED}✗${RESET} ${BOLD}$user${RESET} — falha no registro (HTTP $auth_code)"
    ACCOUNT_IDS+=("")
    continue
  fi

  # Cliente PF
  customer_resp=$(curl -s -X POST http://localhost:8081/people \
    -H "Content-Type: application/json" \
    -d "{\"fullName\":\"$name\",\"cpf\":\"$cpf\",\"email\":\"$email\"}") || customer_resp="{}"

  customer_id=$(json_id "$customer_resp")
  if [ -z "$customer_id" ]; then
    p "  ${RED}✗${RESET} ${BOLD}$user${RESET} — falha ao criar cliente PF"
    ACCOUNT_IDS+=("")
    continue
  fi

  # Conta bancária com saldo inicial
  account_resp=$(curl -s -X POST http://localhost:8082/accounts \
    -H "Content-Type: application/json" \
    -d "{\"customerId\":\"$customer_id\",\"bank\":\"341\",\"branch\":\"0001\",\"number\":\"$number\",\"initialBalance\":$balance}") || account_resp="{}"

  account_id=$(json_id "$account_resp")
  if [ -z "$account_id" ]; then
    p "  ${RED}✗${RESET} ${BOLD}$user${RESET} — falha ao abrir conta"
    ACCOUNT_IDS+=("")
    continue
  fi

  ACCOUNT_IDS+=("$account_id")
  p "  ${GREEN}✓${RESET} ${BOLD}$(printf '%-10s' "$user")${RESET}  conta ${CYAN}$number${RESET}  saldo inicial ${CYAN}R\$ $balance${RESET}"
done

printf "\n"

# ──────────────────────────────────────────────
# 2/3 — Transferências para popular o extrato
# ──────────────────────────────────────────────
p "${BOLD}[2/3] Realizando transferências para popular o extrato...${RESET}"
printf "\n"

do_transfer() {
  local fi=$1 ti=$2 amount=$3
  local from_id="${ACCOUNT_IDS[$fi]:-}" to_id="${ACCOUNT_IDS[$ti]:-}"
  local from_user="${USERS[$fi]}" to_user="${USERS[$ti]}"
  [ -z "$from_id" ] || [ -z "$to_id" ] && return
  code=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST http://localhost:8082/accounts/transfer \
    -H "Content-Type: application/json" \
    -d "{\"fromAccountId\":\"$from_id\",\"toAccountId\":\"$to_id\",\"amount\":$amount}") || code="000"
  if [ "$code" = "200" ]; then
    p "  ${GREEN}✓${RESET} ${BOLD}$(printf '%-8s' "$from_user")${RESET} → ${BOLD}$(printf '%-8s' "$to_user")${RESET}  ${CYAN}R\$ $amount${RESET}"
  else
    p "  ${RED}✗${RESET} ${BOLD}$from_user${RESET} → ${BOLD}$to_user${RESET}  falhou (HTTP $code)"
  fi
}

# Cadeia descendente: admin → alice → bob → carol → dave → eve
do_transfer 0 1 500.00
do_transfer 1 2 200.00
do_transfer 2 3 100.00
do_transfer 3 4  75.00
do_transfer 4 5  50.00
# Transferências extras para admin ter extrato rico
do_transfer 9 0  50.00   # iris → admin
do_transfer 0 5 100.00   # admin → eve
do_transfer 6 0 150.00   # frank → admin

printf "\n"

# ──────────────────────────────────────────────
# 3/3 — Salvar resumo e exibir acesso rápido
# ──────────────────────────────────────────────
p "${BOLD}[3/3] Salvando resumo...${RESET}"
printf "\n"

SUMMARY_FILE="$ROOT/docs/seed-users.md"
{
  echo "# 👥 Usuários de Seed — smanioto-bank"
  echo ""
  echo "> Gerado por \`./seed.sh\`. Válido apenas enquanto os serviços estiverem no ar (H2 in-memory)."
  echo ""
  echo "| Usuário | Senha | Saldo Inicial | ID da Conta |"
  echo "|---------|-------|--------------|-------------|"
  for i in "${!USERS[@]}"; do
    acc="${ACCOUNT_IDS[$i]:-n/a}"
    echo "| \`${USERS[$i]}\` | \`${PASSES[$i]}\` | R\$ ${BALANCES[$i]} | \`$acc\` |"
  done
  echo ""
  echo "## Acesso padrão"
  echo ""
  echo "- **Usuário:** \`admin\`"
  echo "- **Senha:** \`admin\`"
  echo "- **ID da Conta:** \`${ACCOUNT_IDS[0]:-n/a}\`"
} > "$SUMMARY_FILE"

# Resumo no terminal
p "${CYAN}${BOLD}  Usuário      Senha          Saldo Inicial${RESET}"
p "  ────────────────────────────────────────"
for i in "${!USERS[@]}"; do
  printf "  ${GREEN}%-12b${RESET} %-15b R\$ %b\n" "${USERS[$i]}" "${PASSES[$i]}" "${BALANCES[$i]}"
done

printf "\n"
admin_acc="${ACCOUNT_IDS[0]:-n/a}"
p "  ${BOLD}Acesso rápido (admin):${RESET}"
p "  Usuário  → ${CYAN}admin${RESET}"
p "  Senha    → ${CYAN}admin${RESET}"
p "  Conta ID → ${CYAN}$admin_acc${RESET}"
printf "\n"
p "  Todos os IDs salvos em ${YELLOW}docs/seed-users.md${RESET}"
printf "\n"
p "${GREEN}${BOLD}╔══════════════════════════════════════╗${RESET}"
p "${GREEN}${BOLD}║   ✅  Seed concluído!                ║${RESET}"
p "${GREEN}${BOLD}╚══════════════════════════════════════╝${RESET}"
printf "\n"
