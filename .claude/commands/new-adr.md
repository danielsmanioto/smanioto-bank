Cria um novo Architecture Decision Record (ADR) para documentar uma decisão técnica.

O título da decisão é: $ARGUMENTS

Siga o padrão existente em `docs/adr/`. O próximo número sequencial deve ser verificado com `ls docs/adr/`.

Crie o arquivo `docs/adr/ADR-00N-<slug-do-titulo>.md` com a estrutura:

```markdown
# ADR-00N — <Título>

**Status:** Proposto | Aceito | Obsoleto  
**Data:** <YYYY-MM>

## Contexto

<Qual problema ou situação motivou essa decisão?>

## Decisão

<O que foi decidido?>

## Justificativa

<Por que essa opção foi escolhida em vez das alternativas?>

## Alternativas consideradas

- **Opção A** — <descrição e motivo de rejeição>
- **Opção B** — <descrição e motivo de rejeição>

## Consequências

**Positivas:**
- <benefício>

**Negativas / trade-offs:**
- <custo ou limitação>
```

Após criar o arquivo, adicione uma linha no `docs/adr/README.md` referenciando o novo ADR.
