# Spec 003 — Documentação e Scripts de Execução

## Objetivo

Produzir um `README.md` completo e visualmente agradável que explique o projeto, sua arquitetura e como rodá-lo localmente, além de dois scripts de conveniência (`start.sh` e `stop.sh`) para subir e parar todos os serviços com um único comando.

## User Stories

### US1 — README rico e navegável
**Como** desenvolvedor que chega ao repositório pela primeira vez,  
**quero** encontrar um README com visão geral, arquitetura, pré-requisitos, referência de API e instruções de execução,  
**para que** eu consiga rodar e entender o projeto sem precisar explorar o código-fonte.

### US2 — Script de inicialização unificado
**Como** desenvolvedor,  
**quero** executar `./start.sh` na raiz do projeto,  
**para que** todos os 4 serviços (auth, people, accounts, frontend) subam automaticamente em background, com feedback visual do progresso.

### US3 — Script de parada unificado
**Como** desenvolvedor,  
**quero** executar `./stop.sh` na raiz do projeto,  
**para que** todos os serviços iniciados pelo `start.sh` sejam encerrados de forma limpa.

## Critérios de Aceitação

- README exibe badges de tecnologia no topo.
- README contém diagrama de arquitetura (Mermaid).
- README documenta todos os endpoints da API com método, path, body e resposta de exemplo.
- `start.sh` compila os serviços Java, inicia os 4 processos em background, exibe URLs de acesso.
- `start.sh` grava PIDs em `.pids` e logs em `.logs/`.
- `stop.sh` lê `.pids` e encerra cada processo, informando se já estava parado.
- `.logs/` e `.pids` adicionados ao `.gitignore`.
