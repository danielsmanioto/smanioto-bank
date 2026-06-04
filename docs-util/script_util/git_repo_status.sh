#!/usr/bin/env bash
# git_repo_status.sh
# Exibe um resumo do estado do repositório Git:
#   - Primeiro commit (data, autor, mensagem)
#   - Último commit  (data, autor, mensagem)
#   - Usuários que commitaram e quantidade de commits por usuário
#   - 5 arquivos mais alterados no histórico

set -euo pipefail

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
BOLD="\033[1m"
CYAN="\033[1;36m"
RESET="\033[0m"

section() {
  echo -e "\n${CYAN}${BOLD}==> $1${RESET}"
}

check_git_repo() {
  if ! git rev-parse --is-inside-work-tree &>/dev/null; then
    echo "Erro: diretório atual não é um repositório Git." >&2
    exit 1
  fi
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
check_git_repo

REPO_ROOT=$(git rev-parse --show-toplevel)
REPO_NAME=$(basename "$REPO_ROOT")
BRANCH=$(git symbolic-ref --short HEAD 2>/dev/null || git rev-parse --short HEAD)

echo -e "${BOLD}Repositório:${RESET} $REPO_NAME"
echo -e "${BOLD}Branch atual:${RESET} $BRANCH"

# -- Primeiro commit ---------------------------------------------------------
section "Primeiro commit"
git log -1 "$(git rev-list --max-parents=0 HEAD)" \
  --format="%C(yellow)Hash:%Creset %H%n%C(yellow)Data:%Creset %ad%n%C(yellow)Autor:%Creset %an <%ae>%n%C(yellow)Mensagem:%Creset %s" \
  --date=format:"%d/%m/%Y %H:%M:%S"

# -- Último commit -----------------------------------------------------------
section "Último commit"
git log -1 --format="%C(yellow)Hash:%Creset %H%n%C(yellow)Data:%Creset %ad%n%C(yellow)Autor:%Creset %an <%ae>%n%C(yellow)Mensagem:%Creset %s" \
  --date=format:"%d/%m/%Y %H:%M:%S"

# -- Usuários que commitaram e quantidade ------------------------------------
section "Usuários que commitaram (total de commits por usuário)"
git shortlog -sne --all | sort -rn | awk '{
  commits = $1
  $1 = ""
  sub(/^ /, "")
  printf "  %-5s %s\n", commits, $0
}'

# -- 5 arquivos mais alterados -----------------------------------------------
section "5 arquivos mais alterados no histórico"
git log --name-only --format="" --diff-filter=ACDM | \
  grep -v '^$' | \
  sort | uniq -c | sort -rn | head -5 | \
  awk '{printf "  %-6s %s\n", $1, $2}'
