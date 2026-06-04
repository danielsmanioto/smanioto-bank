#!/bin/bash

################################################################################
# Team Activity - Monitora atividade do time
#
# Uso: ./team_activity.sh [opções]
#
# Opções:
#   -d, --days N        Últimos N dias (padrão: 7)
#   -r, --repo REPO     Repositório específico (padrão: todos)
#   --author USER       Filtrar por autor
#   --since DATE        Data início (YYYY-MM-DD)
#   --until DATE        Data fim (YYYY-MM-DD)
#
# Exemplos:
#   ./team_activity.sh                    # Últimos 7 dias
#   ./team_activity.sh -d 30              # Últimos 30 dias
#   ./team_activity.sh -r seu-repo -d 7   # Repo específico
#   ./team_activity.sh --author seu-nome  # Por autor
################################################################################

set -o pipefail

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

# Defaults
DAYS=7
REPO=""
AUTHOR=""
SINCE=""
UNTIL=""

# ============================================================================
# Funções
# ============================================================================

print_header() {
    echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
}

print_section() {
    echo -e "\n${MAGENTA}▸ $1${NC}\n"
}

print_info() {
    echo -e "${CYAN}ℹ${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

# Mostrar ajuda
show_help() {
    cat << EOF
${CYAN}Team Activity${NC} - Monitora atividade do time nos repositórios

${YELLOW}Uso:${NC}
  ./team_activity.sh [opções]

${YELLOW}Opções:${NC}
  -d, --days N        Últimos N dias (padrão: 7)
  -r, --repo REPO     Repositório específico
  --author USER       Filtrar por autor específico
  --since DATE        Data de início (YYYY-MM-DD)
  --until DATE        Data de fim (YYYY-MM-DD)
  -h, --help         Mostrar esta ajuda

${YELLOW}Exemplos:${NC}
  ./team_activity.sh                    # Últimos 7 dias, todos repos
  ./team_activity.sh -d 30              # Últimos 30 dias
  ./team_activity.sh -r seu-repo        # Repo específico
  ./team_activity.sh --author username  # Por autor
  ./team_activity.sh --since 2026-02-01 # Desde data específica

EOF
    exit 0
}

# Parse argumentos
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                ;;
            -d|--days)
                DAYS="$2"
                shift 2
                ;;
            -r|--repo)
                REPO="$2"
                shift 2
                ;;
            --author)
                AUTHOR="$2"
                shift 2
                ;;
            --since)
                SINCE="$2"
                shift 2
                ;;
            --until)
                UNTIL="$2"
                shift 2
                ;;
            *)
                echo "Opção desconhecida: $1"
                show_help
                ;;
        esac
    done

    # Calcular SINCE se não informado
    if [ -z "$SINCE" ]; then
        SINCE=$(date -u -v-${DAYS}d "+%Y-%m-%d" 2>/dev/null || date -u -d "$DAYS days ago" "+%Y-%m-%d" 2>/dev/null)
    fi
}

# ============================================================================
# Main
# ============================================================================

main() {
    parse_args "$@"

    print_header "📊 Team Activity"
    print_info "Período: $SINCE até $(date +%Y-%m-%d)"

    if [ -n "$REPO" ]; then
        print_info "Repositório: $REPO"
    fi

    if [ -n "$AUTHOR" ]; then
        print_info "Autor: $AUTHOR"
    fi

    # ========================================================================
    # 1. Commits por Autor
    # ========================================================================
    print_section "Commits por Autor"

    local git_args="--since=$SINCE"
    [ -n "$UNTIL" ] && git_args="$git_args --until=$UNTIL"
    [ -n "$AUTHOR" ] && git_args="$git_args --author=$AUTHOR"

    if [ -n "$REPO" ]; then
        cd "$REPO" 2>/dev/null || { echo "Repo não encontrado"; exit 1; }
    fi

    # Verificar se é um repo git
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        echo "Erro: Não é um repositório Git"
        exit 1
    fi

    git shortlog -sn $git_args | head -20 | while read count author; do
        printf "  ${CYAN}%3d${NC}  %s\n" "$count" "$author"
    done

    # ========================================================================
    # 2. Commits por Dia
    # ========================================================================
    print_section "Commits por Dia (últimos 7 dias)"

    git log --date=short --format="%ad" $git_args | sort | uniq -c | tail -7 | while read count date; do
        local bar=""
        for ((i=0; i<count; i++)); do
            bar="${bar}▓"
        done
        printf "  ${GREEN}%s${NC} %-25s %2d commits\n" "$date" "$bar" "$count"
    done

    # ========================================================================
    # 3. Top 10 Commits Recentes
    # ========================================================================
    print_section "Top 10 Commits Recentes"

    git log --oneline $git_args | head -10 | nl | while read num hash msg; do
        printf "  ${MAGENTA}%2d${NC}  ${YELLOW}%s${NC} %s\n" "$num" "$hash" "$msg"
    done

    # ========================================================================
    # 4. Branches Ativos
    # ========================================================================
    print_section "Branches Recentemente Atualizados"

    git for-each-ref --sort='-committerdate:iso8601' --format='%(committerdate:short) %(refname:short) %(authorname)' refs/heads | head -10 | while read date branch author; do
        printf "  ${GREEN}%s${NC}  ${CYAN}%-30s${NC}  %s\n" "$date" "$branch" "$author"
    done

    # ========================================================================
    # 5. Mudanças por Arquivo
    # ========================================================================
    print_section "Top 10 Arquivos Modificados"

    git log --name-only --pretty=format: $git_args | grep -v '^$' | sort | uniq -c | sort -rn | head -10 | while read count file; do
        printf "  ${MAGENTA}%3d${NC}  %s\n" "$count" "$file"
    done

    # ========================================================================
    # 6. Estatísticas Gerais
    # ========================================================================
    print_section "Estatísticas"

    local total_commits=$(git rev-list --count HEAD $git_args)
    local total_files=$(git log --name-only --pretty=format: $git_args | grep -v '^$' | sort -u | wc -l)
    local total_authors=$(git shortlog -sn $git_args | wc -l)
    local additions=$(git log --numstat $git_args | awk '{a+=$1} END {print a}')
    local deletions=$(git log --numstat $git_args | awk '{d+=$2} END {print d}')

    printf "  ${CYAN}Total de commits:${NC}    %d\n" "$total_commits"
    printf "  ${CYAN}Arquivos modificados:${NC} %d\n" "$total_files"
    printf "  ${CYAN}Autores ativos:${NC}      %d\n" "$total_authors"
    printf "  ${CYAN}Linhas adicionadas:${NC}  ${GREEN}+%d${NC}\n" "$additions"
    printf "  ${CYAN}Linhas removidas:${NC}    ${RED}-%d${NC}\n" "$deletions"

    echo ""
}

main "$@"
