# Tasks — Spec 003: Documentação e Scripts de Execução

## Phase 1: Scripts

- [x] T001 Criar `start.sh` na raiz — compila os 3 serviços Java com Maven, inicia os 4 processos em background, grava PIDs em `.pids`, logs em `.logs/`, exibe URLs de acesso
- [x] T002 Criar `stop.sh` na raiz — lê `.pids`, encerra cada processo com kill, remove o arquivo `.pids`
- [x] T003 Tornar `start.sh` e `stop.sh` executáveis (`chmod +x`) e adicionar `.logs/` e `.pids` ao `.gitignore`

## Phase 2: README

- [x] T004 Reescrever `README.md` com: badges, visão geral, diagrama Mermaid, tabela de serviços, pré-requisitos, instruções de início rápido, referência completa da API, estrutura do projeto e fluxo SDD
