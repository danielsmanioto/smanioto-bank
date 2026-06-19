# Backlog — smanioto-bank

Consolidação de issues, dívidas técnicas e melhorias planejadas.  
Issues detalhados: github.com/danielsmanioto/smanioto-bank/issues

---

## 🔴 Dívida Técnica

### ~~DT-001 — PySpark incompatível com Java 25~~ ✅ RESOLVIDO
**Origem:** erro ao rodar `./datalake.sh`  
**Erro:** `java.lang.UnsupportedOperationException: getSubject is not supported`

**Diagnóstico real (resolvido em 2026-06-19):**
- `JAVA_TOOL_OPTIONS` e `_JAVA_OPTIONS` **não aceitam** `--add-opens` no Java 25 (saem com "Unrecognized option")
- `JDK_JAVA_OPTIONS` aceita a flag, mas não resolve: o problema não é acesso modular — `Subject.getSubject(AccessControlContext)` foi removido e lança `UnsupportedOperationException` no Java 21+, independente de `--add-opens`
- Hadoop 3.4.2 (bundled no PySpark 4.1.2) ainda usa `Subject.getSubject()` em várias classes internas (`UserGroupInformation`, `ViewFileSystem`, etc.)
- **Dois bugs adicionais descobertos no processo:**
  - `JAVA_HOME` apontava para caminho inexistente, corrompendo silenciosamente o `spark-class`
  - H2 2.x persiste `@Enumerated(EnumType.STRING)` como `ENUM('CREDIT','DEBIT')` (tipo `OTHER`), que o Spark JDBC não mapeia

**Solução aplicada em `glue_job.py`:**
1. `_fix_java_home()` — detecta Java 21+ e usa `java_home -v 17` (Corretto 17, instalado) automaticamente
2. `read_accounts()` e `read_movements()` — leem via query SQL com `CAST(UUID AS VARCHAR)` e `CAST(ENUM AS VARCHAR)` 

**Referência:** ADR-004

---

## 🟡 Evolução Arquitetural

### EA-001 — Modelo ledger: saldo calculado a partir dos lançamentos
**Origem:** ADR-005 (Proposto)  
**Dependência:** EA-002 (banco persistente)

Remover o campo `balance` da entidade `Account` e calcular o saldo sempre como `SUM` dos movimentos. Alinha o projeto com o modelo de sistemas financeiros reais — lançamentos são imutáveis, saldo é derivado.

---

### EA-002 — Migrar H2 in-memory para PostgreSQL via Docker
**Origem:** ADR-006 (Proposto) · Issue #13  
**Dependência:** nenhuma

Substituir os 3 bancos H2 por containers PostgreSQL via `docker-compose.yml`. Adicionar Flyway para versionamento de schema. Atualizar `start.sh` para subir os containers antes dos serviços Java. Simplifica também o data lake (remove necessidade do H2 TCP).

---

## 🟢 Funcionalidades e Melhorias

### FM-001 — Testes unitários
**Origem:** Issue #7

Ampliar cobertura de testes unitários nos três serviços. Atualmente existem testes para `AccountService`, `CustomerService` e `UserCredentialsService`, mas faltam casos de borda e cenários de erro.

---

### FM-002 — Testes de integração E2E
**Origem:** Issue #8

Criar suite de testes E2E que exercite o fluxo completo: registro → login → abertura de conta → transferência → extrato. Avaliar uso de RestAssured ou Testcontainers para subir os serviços em ambiente controlado.

---

### FM-003 — Testes de performance por serviço
**Origem:** Issue #9

Implementar testes de carga nos endpoints críticos (transferência, extrato). Avaliar k6 (já instalado no ambiente) para cenários de múltiplos usuários simultâneos e medir impacto do lock pessimista sob concorrência.

---

### FM-004 — Logs estruturados para observabilidade
**Origem:** Issue #14

Adicionar logs estruturados (JSON) nos serviços Java com correlação de requisições. Avaliar uso de Spring Boot Actuator + Logback com encoder JSON. Centralizar logs do data lake junto com os serviços.

---

### FM-005 — Containerizar todos os serviços
**Origem:** Issue #13

Criar `Dockerfile` para cada serviço Java e para o frontend. Unificar tudo no `docker-compose.yml` (junto com o PostgreSQL do EA-002) para que `docker compose up` substitua o `./start.sh`.

---

## Ordem sugerida de implementação

```
DT-001  →  EA-002  →  EA-001  →  FM-005
                               →  FM-001  →  FM-002  →  FM-003
                               →  FM-004
```

Resolver a dívida técnica do PySpark (DT-001) desbloqueia o data lake.  
Migrar para PostgreSQL (EA-002) é pré-requisito natural para ledger (EA-001) e containerização (FM-005).
