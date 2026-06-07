# smanioto-bank

Sistema bancário MVP com arquitetura de microsserviços em Java/Spring Boot.

## Serviços e portas

| Serviço          | Porta | Responsabilidade                         |
|------------------|-------|------------------------------------------|
| auth-service     | 8080  | Autenticação, JWT (registro e login)     |
| people-service   | 8081  | Cadastro e consulta de clientes (CPF)    |
| accounts-service | 8082  | Contas bancárias, transferências, extrato|
| frontend         | 3000  | Interface HTML/CSS/JS via Node.js        |

## Stack

- **Java 17+**, **Spring Boot 3.3**, **Maven**
- **H2 in-memory** em todos os serviços (sem persistência entre restarts — ver ADR-001)
- **Spring Data JPA** para persistência
- **Spring Security + JWT** no auth-service
- **JUnit 5 + Mockito** para testes de unidade
- **Spring MockMvc** para testes de controller

## Como rodar

```bash
# Iniciar todos os serviços
./start.sh

# Popular banco com dados de teste (10 usuários + contas)
./seed.sh

# Parar tudo
./stop.sh

# Ver logs de um serviço
./logs.sh auth-service
```

A variável `JWT_SECRET` é lida do ambiente. O `start.sh` define um valor padrão para dev local:
```bash
export JWT_SECRET=meu-secret-local ./start.sh
```

## Estrutura de cada serviço

```
services/<nome>-service/
└── src/main/java/com/smanioto/bank/<nome>/
    ├── controller/   # endpoints HTTP, validação de entrada
    ├── dto/          # Request e Response records
    ├── model/        # entidades JPA
    ├── repository/   # interfaces Spring Data JPA
    ├── service/      # regras de negócio
    └── config/       # beans de configuração (Security, etc.)
```

## Convenções obrigatórias

- **Sem framework de mapeamento** (sem MapStruct, sem ModelMapper) — conversão manual DTO↔Entity (ADR-003)
- **Lock pessimista em transferências** — `findByIdForUpdate` no `AccountRepository` (ADR-002)
- **DTOs como Java records** — usar `record` para Request e Response, não classes
- **Injeção via construtor** — nunca `@Autowired` em campos
- **Validação com Bean Validation** — `@NotNull`, `@NotBlank`, `@Positive` nos DTOs de request
- **`BigDecimal` para valores monetários** — sempre com `scale=2` e `HALF_EVEN`
- **`UUID` como identificador** de todas as entidades

## Convenções de testes

- Testes de serviço: `@ExtendWith(MockitoExtension.class)` com mocks declarados via `@Mock`
- Testes de controller: `@WebMvcTest` + `MockMvc` para testar endpoints HTTP
- Nomenclatura: `deve<Acao>Quando<Condicao>` (ex: `deveTransferirComSucesso`)
- Não usar `@SpringBootTest` em testes de unidade — só para testes de integração

## Integração entre serviços

O `accounts-service` chama o `people-service` via HTTP usando `HttpPeopleClient` (implementação de `PeopleClient`). Não há service discovery — URLs hardcoded nas `application.properties`.

## Data-lake (`services/data-lake/`)

Camada de democratização de dados que simula um pipeline AWS Glue → S3 → Parquet (ver ADR-004).

### Stack de dados

- **Python 3**, **PySpark 3.5+**, **pandas 2.0+**, **pyarrow 14+**
- Lê do `accounts-service` via **JDBC H2 TCP** (porta 9092) — sem alterar a API do serviço
- Escreve **Parquet** particionado por `account_id/date` em `output/daily_statement/`

### Arquivos

| Arquivo | Responsabilidade |
|---|---|
| `glue_job.py` | Lê `ACCOUNTS` e `MOVEMENTS` via JDBC, calcula visão diária, salva Parquet |
| `query_daily.py` | CLI para consultar os Parquets (usa pyarrow — não precisa de Spark para leitura) |
| `run_job.sh` | Executa o job com verificação de dependências Python |
| `requirements.txt` | `pyspark`, `pandas`, `pyarrow` |

### Como executar

```bash
# O accounts-service precisa estar rodando (./start.sh na raiz)
cd services/data-lake
./run_job.sh                                        # gera os Parquets

python3 query_daily.py --list-accounts              # lista contas no data lake
python3 query_daily.py --account <uuid>             # extrato diário completo
python3 query_daily.py --account <uuid> --date 2026-06-04
python3 query_daily.py --account <uuid> --from 2026-06-01 --to 2026-06-04
```

### Estrutura de saída

```
output/daily_statement/
  account_id=<uuid>/
    date=<yyyy-mm-dd>/
      part-00000-....parquet   ← opening_balance, closing_balance, total_credits, total_debits, transactions[]
```

### Convenções do data-lake

- **Sem API nova no accounts-service** — o job conecta direto ao H2 TCP, nunca via REST
- **Job sempre reprocessa tudo** (`mode("overwrite")`) — sem controle incremental no MVP
- **Cálculo de saldo por janela regressiva** — `Window.partitionBy("account_id").orderBy("date".desc())` com soma acumulada; detalhes matemáticos no ADR-004
- **Consulta usa pyarrow**, não PySpark — mais leve, não sobe cluster Spark para leitura
- Ao migrar para PostgreSQL, só a `JDBC_URL` em `glue_job.py` precisa mudar

## ADRs

Decisões arquiteturais estão documentadas em `docs/adr/`:
- **ADR-001** — H2 in-memory
- **ADR-002** — Lock pessimista em transferências
- **ADR-003** — Sem framework de mapeamento
- **ADR-004** — Democratização de extrato via Parquet

## Variáveis de ambiente

| Variável     | Serviço       | Descrição                                 |
|--------------|---------------|-------------------------------------------|
| `JWT_SECRET` | auth-service  | Chave para assinar tokens JWT (obrigatória)|

## Ferramentas de IA usadas no projeto

O projeto usa **GitHub Copilot** e **Claude Code** em papéis complementares.

### GitHub Copilot
Autocompletar inline no editor. Funciona melhor quando os arquivos do serviço que está sendo editado estão abertos — ele aprende os padrões (records de DTO, injeção via construtor, `BigDecimal`, `UUID`) e os repete automaticamente.

### Claude Code (este contexto)
Tarefas que envolvem múltiplos arquivos, decisões arquiteturais ou contexto acumulado:

- Criar um novo serviço seguindo os padrões: use `/project:new-service <nome>`
- Registrar uma decisão técnica: use `/project:new-adr <título>`
- Rodar testes de um serviço: use `/project:test <auth|people|accounts|all>`
- Ao pedir para gerar código, mencionar explicitamente a ADR relevante ajuda a manter a consistência (ex: "seguindo o ADR-003, sem MapStruct")

### O que NÃO delegar à IA sem revisão
- Geração de CPFs ou dados pessoais reais em seeds ou testes
- Alteração nas regras de lock pessimista em `AccountService.transfer()` — qualquer mudança ali deve passar por revisão manual e ter um ADR
- Mudança no algoritmo de `scale` e `RoundingMode` dos valores monetários
- Lógica de cálculo de `opening_balance`/`closing_balance` em `glue_job.py` — a janela regressiva com `cum_net_desc` é sutil; qualquer alteração precisa de validação numérica manual
