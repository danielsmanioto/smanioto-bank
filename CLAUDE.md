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

## Comandos Maven (por serviço)

Todos os serviços usam **Maven 3.9+** com Java 17. Execute na raiz de cada serviço:

```bash
cd services/auth-service  # (ou people-service / accounts-service)

# Compilar e pular testes
mvn clean compile -DskipTests

# Rodar todos os testes
mvn test

# Rodar uma classe de teste específica
mvn test -Dtest=AuthControllerTest

# Rodar um método de teste específico
mvn test -Dtest=AuthControllerTest#deveRegistrarComSucesso

# Gerar JAR (sem executar)
mvn package -DskipTests

# Ver dependências do projeto
mvn dependency:tree

# Limpar artefatos gerados (target/)
mvn clean
```

### Testes via Claude Code

Use a skill `test` para rodar testes de um serviço:

```bash
/project:test auth       # testa auth-service
/project:test all        # testa os 3 serviços
```

> **Nota:** os testes não incluem `@SpringBootTest` — apenas unitários com `@WebMvcTest` (controller) e `@ExtendWith(MockitoExtension.class)` (service).

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

> **`.claude/agents/`**: os agentes definidos ali (`tech-lead`, `po`, `qa`, `desenvolvedor_backend`, `desenvolvedor_frontend`) descrevem um projeto diferente ("squad-virtual-checkout-supermercado") e não se aplicam ao smanioto-bank — ignore-os ao trabalhar neste repositório.

## Claude Code Skills

O projeto define skills customizadas (`.claude/commands/`) para tarefas comuns:

| Skill | Uso | Exemplo |
|---|---|---|
| `start` | Inicia todos os serviços (equivalente a `./start.sh`) | `/project:start` |
| `stop` | Para todos os serviços | `/project:stop` |
| `test` | Roda testes de um serviço | `/project:test auth` ou `/project:test all` |
| `logs` | Exibe logs de um serviço em tempo real | `/project:logs accounts` |
| `seed` | Popula banco com 10 usuários de teste | `/project:seed` |
| `datalake` | Executa o pipeline Glue → Parquet | `/project:datalake` |
| `new-adr` | Cria um novo ADR | `/project:new-adr "Título da Decisão"` |
| `new-service` | Guia para criar microsserviço | `/project:new-service <nome>` |

### Exemplos de uso

```bash
# Rodar testes do people-service e ver os logs
/project:test people
/project:logs people

# Criar uma nova decisão técnica
/project:new-adr "Migrar de H2 para PostgreSQL"

# Popular banco e ver extrato no data-lake
./start.sh
./seed.sh
/project:datalake
python3 services/data-lake/query_daily.py --list-accounts
```

## ADRs

Decisões arquiteturais estão documentadas em `docs/adr/`:
- **ADR-001** — H2 in-memory
- **ADR-002** — Lock pessimista em transferências
- **ADR-003** — Sem framework de mapeamento
- **ADR-004** — Democratização de extrato via Parquet
- **ADR-005** (Proposto) — Modelo ledger: saldo calculado a partir dos lançamentos (depende do ADR-006)
- **ADR-006** (Proposto) — Migração de H2 in-memory para PostgreSQL via Docker

> ADR-005 e ADR-006 ainda não foram implementados — o estado atual do código continua o descrito no ADR-001 (H2) e ADR-002 (saldo como campo em `Account`, não derivado dos lançamentos). Não assuma o modelo ledger ou PostgreSQL ao ler `AccountService`.

## Variáveis de ambiente

| Variável     | Serviço       | Descrição                                 |
|--------------|---------------|-------------------------------------------|
| `JWT_SECRET` | auth-service  | Chave para assinar tokens JWT (obrigatória)|

## Troubleshooting

### Portas já em uso

Se ao rodar `./start.sh` você receber erro de porta já em uso (8080, 8081, 8082, 3000):

```bash
# Listar processos nas portas
lsof -i :8080
lsof -i :8081
lsof -i :8082
lsof -i :3000

# Matar o processo (trocar PID)
kill -9 <PID>

# Ou simplesmente parar tudo
./stop.sh
```

### JWT_SECRET não definida

Se a autenticação falhar com erro `NullPointerException` em `JwtService`:

```bash
export JWT_SECRET=meu-secret-local-dev
./start.sh
```

Ou no `start.sh`, a variável já tem um valor padrão para dev local.

### H2 não carrega dados do seed

Verificar se o accounts-service expõe a porta TCP 9092 (necessária para data-lake):

```bash
# Deve estar aberto
lsof -i :9092

# Se não, verificar o log
./logs.sh -s accounts | grep -i "h2\|tcp"
```

### Testes falhando após alteração de entidade

Se um teste falha após mudar modelo JPA, limpar cache Maven:

```bash
cd services/<nome>-service
mvn clean test
```

### Frontend não conecta aos serviços

Verificar se todos os 3 microsserviços estão rodando:

```bash
./logs.sh -e    # mostra apenas erros
curl -i http://localhost:8080/auth/validate  # deve retornar 401 (sem token)
curl -i http://localhost:8082/accounts       # deve retornar erro de formato
```

### Data-lake: erro ao conectar no H2 TCP

Verificar se o accounts-service está com o servidor H2 TCP ativo:

```bash
cd services/data-lake
./run_job.sh
# Se falhar, aumentar o timeout ou verificar ./logs.sh -s accounts
```

## Git Workflow

### Convenção de branches

- `main` — sempre pronta para produção, protegida
- `feature/<nome>` — branches de features (ex: `feature/pix-integration`)
- `fix/<nome>` — branches de bugfix (ex: `fix/transfer-race-condition`)
- `docs/<nome>` — branches de documentação (ex: `docs/adr-004`)

### Convenção de commits

```
<tipo>(<escopo>): <mensagem curta>

<descrição opcional>
```

**Tipos:** `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

**Escopos:** nome do serviço ou área (`auth`, `people`, `accounts`, `frontend`, `data-lake`)

**Exemplos:**
```
feat(accounts): adiciona lock pessimista em transferências
fix(auth): corrige JWT expirado sendo aceito
docs(adr): registra decisão sobre H2 in-memory
```

### Processo de ADR

Toda decisão técnica relevante deve ter um ADR:

```bash
/project:new-adr "Título da Decisão"
# Cria docs/adr/ADR-NNN-titulo.md com template
# Edite o arquivo e commite:
git add docs/adr/ADR-NNN-titulo.md
git commit -m "docs(adr): ADR-NNN titulo da decisão"
```

Referencie o ADR ao implementar (ex: no comentário da classe ou em commits).

## IDE Setup

### VS Code + Spring Boot Extension Pack

Recomendado: instalar [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) que inclui:
- Language Support for Java
- Debugger for Java
- Test Runner for Java
- Visual Studio IntelliCode

### Abrindo o projeto

```bash
code .
```

### Debugging de testes

No VS Code, colocando breakpoint em um teste e clicando "Debug" acima do nome do teste, você entra no debugger direto.

### Copilot + Contexto

Para melhor autocompletar do Copilot:

1. Abra um arquivo do serviço que está desenvolvendo (ex: `AccountService.java`)
2. Abra o DTO ou entidade relacionada (ex: `AccountDTO.java`, `Account.java`)
3. O Copilot aprenderá os padrões (records, BigDecimal, UUID) e sugerirá autocompletes consistentes

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
