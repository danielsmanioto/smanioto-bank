# ADR-003 — Sem framework de mapeamento de objetos

**Status:** Aceito  
**Data:** 2026-06

## Contexto

A camada de apresentação precisa converter entidades JPA em DTOs de resposta (ex: `Account` → `AccountResponse`, `Movement` → `MovementResponse`). Existem três abordagens comuns:

- **MapStruct** — geração de código em tempo de compilação via annotation processor; zero overhead em runtime
- **ModelMapper / Dozer** — mapeamento por reflexão em runtime; configuração mais simples, overhead mensurável
- **Mapeamento manual** — construção explícita do DTO no próprio controller ou em um método de fábrica

## Decisão

Usar **mapeamento manual** direto no controller, construindo os DTOs via construtores de record Java.

```java
return new AccountResponse(
    account.getId(),
    account.getCustomerId(),
    account.getBank(),
    account.getBranch(),
    account.getNumber(),
    account.getBalance()
);
```

## Justificativa

O MVP tem poucos campos por entidade e poucos endpoints. O custo de adicionar e configurar MapStruct (plugin Maven, interfaces de mapper, testes de mapeamento) supera o benefício nesse estágio.

Java Records reduzem o boilerplate das classes DTO para praticamente zero, tornando o mapeamento manual legível e sem necessidade de geração de código adicional.

## Consequências

**Positivas:**
- Zero dependências e zero configuração de build adicional
- Mapeamento explícito — cada campo visível no código sem indireção
- Records garantem imutabilidade e `equals`/`hashCode`/`toString` sem código extra
- Erros de compilação imediatos se um campo mudar de tipo ou for removido da entidade

**Negativas/Limitações:**
- À medida que o número de campos ou entidades cresce, os controllers acumulam código de mapeamento
- Não há convenção automática para novos campos — cada adição requer edição manual em múltiplos pontos (entidade, DTO, mapeamento)

## Quando revisar

Se entidades crescerem para mais de ~10 campos mapeáveis, ou se o mesmo mapeamento precisar ser reutilizado em múltiplos pontos (ex: eventos, respostas de listas e detalhes), introduzir MapStruct com interfaces de mapper dedicadas.
