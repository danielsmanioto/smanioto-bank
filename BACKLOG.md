# Backlog — smanioto-bank

Consolidação de issues, dívidas técnicas e melhorias planejadas.  
Issues detalhados: github.com/danielsmanioto/smanioto-bank/issues

---

## 🔴 Dívida Técnica

### DT-001 — PySpark incompatível com Java 25
**Origem:** erro ao rodar `./datalake.sh`  
**Erro:** `java.lang.UnsupportedOperationException: getSubject is not supported`

O Hadoop/Spark usa `javax.security.auth.Subject.getSubject()` que foi removido no Java 17+ (JEP 411). O PySpark 3.5.x depende de uma versão do Hadoop que ainda usa essa API.

**Soluções possíveis:**
- Adicionar flag JVM `--add-opens java.base/javax.security.auth=ALL-UNNAMED` ao iniciar o Spark
- Fazer downgrade para Java 17 LTS (compatível com PySpark 3.5)
- Aguardar PySpark 4.x que suporta Java 21+

**Workaround temporário:** adicionar ao `glue_job.py` antes de criar a SparkSession:
```python
import os
os.environ["JAVA_TOOL_OPTIONS"] = "--add-opens java.base/javax.security.auth=ALL-UNNAMED"
```

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
