# ADR-002 — Lock pessimista em transferências

**Status:** Aceito  
**Data:** 2026-06  
**Arquivo relevante:** `accounts-service/.../repository/AccountRepository.java`

## Contexto

A operação de transferência lê o saldo de duas contas, verifica cobertura e atualiza ambos os registros dentro de uma única transação. Se duas transferências envolvendo a mesma conta ocorrerem simultaneamente, uma leitura suja ou uma condição de corrida pode resultar em saldo negativo ou dupla dedução.

As opções avaliadas foram:

- **Lock pessimista (`PESSIMISTIC_WRITE`)** — adquire `SELECT FOR UPDATE` no banco antes de ler; bloqueia outras transações concorrentes sobre as mesmas linhas
- **Lock otimista (`@Version`)** — detecta conflito no commit via campo de versão; lança `OptimisticLockException` e exige retry na camada de aplicação
- **Sem lock** — depende apenas da atomicidade da transação; seguro apenas se o banco garantir isolamento `SERIALIZABLE` por padrão (H2 usa `READ_COMMITTED`)

## Decisão

Usar **lock pessimista** (`LockModeType.PESSIMISTIC_WRITE`) no método `findByIdForUpdate` do `AccountRepository`.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select a from Account a where a.id = :accountId")
Optional<Account> findByIdForUpdate(@Param("accountId") UUID accountId);
```

## Justificativa

Transferências bancárias são operações de alta criticidade com baixa taxa de conflito esperada em um MVP de uma única instância. Lock pessimista garante corretude imediata sem lógica de retry na aplicação.

Lock otimista seria preferível em alta concorrência (escala horizontal), mas introduziria complexidade de retry que não agrega valor no estágio atual do projeto.

## Consequências

**Positivas:**
- Corretude garantida: impossível double-spend ou saldo negativo por corrida
- Implementação simples — uma anotação no repositório, sem lógica de retry
- Comportamento previsível e fácil de auditar

**Negativas/Limitações:**
- Serializa transferências concorrentes que envolvem a mesma conta (reduz throughput)
- Em escala horizontal com múltiplas instâncias, o lock de banco pode se tornar gargalo
- H2 in-memory suporta `SELECT FOR UPDATE`, mas o comportamento exato pode diferir de PostgreSQL em cenários de deadlock

## Quando revisar

Se o serviço escalar para múltiplas instâncias ou se a contenção de locks se tornar mensurável, avaliar lock otimista com retry idempotente ou uma arquitetura baseada em eventos (saga pattern).
