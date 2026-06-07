# ADR-005 — Migração para modelo ledger (saldo calculado)

**Status:** Proposto  
**Data:** 2026-06

## Contexto

O modelo atual armazena o saldo como campo `balance` na entidade `Account` e o atualiza via `UPDATE` a cada transferência. Isso funciona para o MVP graças ao lock pessimista (ADR-002), mas não reflete o modelo adotado por sistemas financeiros reais.

Em bancos reais, a tabela de movimentos (ledger) é a fonte da verdade. O saldo é sempre calculado como `Σ créditos − Σ débitos` — nunca armazenado diretamente. Lançamentos são imutáveis: só `INSERT`, nunca `UPDATE` ou `DELETE`.

## Decisão

Ainda não tomada — item de backlog.

## Mudanças necessárias para implementar

1. Remover o campo `balance` da entidade `Account` e da tabela `ACCOUNTS`.
2. Adicionar query no `MovementRepository`:
   ```java
   @Query("SELECT COALESCE(SUM(CASE WHEN m.type = 'CREDIT' THEN m.amount ELSE -m.amount END), 0) FROM Movement m WHERE m.accountId = :accountId")
   BigDecimal calculateBalance(@Param("accountId") UUID accountId);
   ```
3. Atualizar `AccountService.getAccount()` para calcular o saldo na consulta.
4. Remover `source.setBalance()` e `destination.setBalance()` do método `transfer()` — a transferência vira apenas dois `INSERT` em `movements`.
5. Atualizar `AccountResponse` para continuar retornando o saldo (calculado, não lido do campo).

## Consequências esperadas

**Positivas:**
- Auditoria total — cada centavo tem origem rastreável, sem `UPDATE` em saldo.
- Consistência garantida pela imutabilidade dos lançamentos, sem depender de lock.
- Alinhado com o modelo de ledger usado por instituições financeiras reais.

**Negativas / trade-offs:**
- `GET /accounts/{id}` passa a executar um `SUM` sobre todos os movimentos da conta — pode ser lento para contas com histórico longo sem índice adequado.
- Exige migração de dados: calcular o saldo atual de cada conta e inserir um lançamento inicial de crédito representando esse saldo.
- Remove o ADR-002 (lock pessimista) como necessidade — simplifica o código, mas é uma mudança estrutural relevante.

## Quando revisar

Ao migrar para PostgreSQL (pré-requisito natural, pois o H2 in-memory perde os dados ao reiniciar, tornando a migração de dados sem sentido no MVP atual).
