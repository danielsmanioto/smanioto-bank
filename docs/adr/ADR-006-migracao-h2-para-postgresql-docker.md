# ADR-006 — Migração de H2 in-memory para PostgreSQL via Docker

**Status:** Proposto  
**Data:** 2026-06

## Contexto

Os três serviços (`auth-service`, `people-service`, `accounts-service`) usam H2 in-memory como banco de dados (ADR-001). Essa decisão foi adequada para o MVP — zero dependências externas, sobe com um único comando — mas impõe limitações que bloqueiam a evolução do projeto:

- **Dados perdidos a cada restart** — inviabiliza testes com estado acumulado e a migração para ledger (ADR-005).
- **Sem ferramenta de migração de schema** — o `ddl-auto=create-drop` recria as tabelas a cada boot; impossível evoluir o schema de forma controlada.
- **Não reflete produção** — comportamentos específicos de PostgreSQL (tipos, índices, locks reais) não são testados localmente.
- **Data lake limitado** — o Glue Job usa H2 TCP (porta 9092) como workaround; com PostgreSQL, a conexão JDBC seria direta e mais estável.

## Decisão

Ainda não tomada — item de backlog. Depende de infraestrutura mínima (Docker) e de definir estratégia de migração de schema (Flyway ou Liquibase).

## Mudanças necessárias para implementar

### 1. Infraestrutura — `docker-compose.yml` na raiz

```yaml
services:
  auth-db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: authdb
      POSTGRES_USER: auth
      POSTGRES_PASSWORD: auth
    ports:
      - "5432:5432"

  people-db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: peopledb
      POSTGRES_USER: people
      POSTGRES_PASSWORD: people
    ports:
      - "5433:5432"

  accounts-db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: accountsdb
      POSTGRES_USER: accounts
      POSTGRES_PASSWORD: accounts
    ports:
      - "5434:5432"
```

### 2. Dependências Maven — trocar H2 por PostgreSQL + Flyway

Em cada `pom.xml`, remover `h2` e adicionar:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

### 3. `application.properties` — atualizar datasource

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/authdb
spring.datasource.username=auth
spring.datasource.password=auth
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

### 4. Migrations Flyway — `src/main/resources/db/migration/`

Criar scripts `V1__create_schema.sql` para cada serviço com o DDL atual (gerado pelo H2 ou escrito manualmente).

### 5. Atualizar `start.sh`

Adicionar `docker compose up -d` antes de compilar os serviços Java, com verificação de saúde dos containers.

### 6. Atualizar data lake

Substituir a URL JDBC do H2 TCP pela URL do PostgreSQL em `glue_job.py`:
```python
JDBC_URL = "jdbc:postgresql://localhost:5434/accountsdb"
JDBC_PROPS = {
    "driver": "org.postgresql.Driver",
    "user": "accounts",
    "password": "accounts",
}
```
Adicionar o driver PostgreSQL ao classpath do Spark.

## Consequências esperadas

**Positivas:**
- Dados persistem entre restarts — viabiliza o modelo ledger (ADR-005) e testes com estado.
- Schema versionado com Flyway — evoluções controladas e rastreáveis.
- Ambiente local fiel à produção.
- Data lake simplificado — sem necessidade do H2 TCP server na porta 9092.

**Negativas / trade-offs:**
- Docker passa a ser pré-requisito — aumenta o atrito para novos desenvolvedores.
- `./start.sh` fica mais complexo (precisa aguardar os containers ficarem healthy).
- Cada serviço precisa de scripts de migration Flyway — trabalho inicial de escrita do DDL.

## Dependências

- **ADR-001** (H2 in-memory) — será substituída por esta decisão.
- **ADR-005** (modelo ledger) — recomendado implementar junto, pois ambos exigem migração de dados.

## Quando revisar

Quando o projeto evoluir para além do MVP e houver necessidade de persistência real ou preparação para ambiente de staging/produção.
