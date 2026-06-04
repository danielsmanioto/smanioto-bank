# ADR-001 — Banco de dados em memória (H2)

**Status:** Aceito  
**Data:** 2026-06

## Contexto

O smanioto-bank é um MVP com objetivo principal de validar a arquitetura de microsserviços e os fluxos de negócio bancário (abertura de conta, transferência, extrato). O projeto precisa rodar localmente com um único comando (`./start.sh`) sem dependências externas de infraestrutura.

As opções avaliadas foram:

- **H2 in-memory** — banco embedded, sem instalação, sobe com o processo Spring Boot
- **H2 em arquivo** — persiste entre reinicializações, mesma facilidade de setup
- **PostgreSQL via Docker** — banco real, exige Docker instalado e `docker compose up`
- **PostgreSQL local** — exige instalação e configuração manual

## Decisão

Usar **H2 in-memory** em todos os três serviços (`jdbc:h2:mem`).

## Justificativa

O objetivo do MVP é time-to-market: demonstrar os fluxos funcionando, não operar dados reais em produção. H2 in-memory elimina qualquer pré-requisito além de Java e Maven, reduzindo o atrito para qualquer desenvolvedor que clonar o repositório.

A perda de dados ao reiniciar é aceitável nessa fase porque o `seed.sh` repopula todo o estado necessário em segundos.

## Consequências

**Positivas:**
- Zero dependências externas — `./start.sh` é suficiente
- Schema criado automaticamente pelo Hibernate (`ddl-auto=create-drop`)
- Ambiente 100% reproduzível sem estado residual entre execuções

**Negativas/Limitações:**
- Dados perdidos a cada reinicialização dos serviços
- Não suporta múltiplas instâncias do mesmo serviço (estado não é compartilhado)
- Comportamento de transações e locks pode diferir de um banco relacional real em edge cases

## Quando revisar

Ao evoluir para um ambiente persistente (staging, produção ou testes de carga), substituir por PostgreSQL e externalizar a configuração via variáveis de ambiente.
