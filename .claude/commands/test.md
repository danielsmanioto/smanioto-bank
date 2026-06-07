Roda os testes do serviço especificado em $ARGUMENTS.

Serviços disponíveis: `auth`, `people`, `accounts`, ou `all` para rodar todos.

**Exemplos de uso:**
- `/project:test auth` → roda testes do auth-service
- `/project:test all` → roda testes dos 3 serviços

Execute o comando Maven correspondente:

- Para um serviço específico:
```bash
cd services/<nome>-service && mvn test
```

- Para todos:
```bash
cd services/auth-service && mvn test
cd services/people-service && mvn test
cd services/accounts-service && mvn test
```

Os testes usam JUnit 5 + Mockito. Não requerem os serviços rodando — são testes de unidade com H2 embedded.
