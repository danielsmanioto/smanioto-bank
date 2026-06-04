# Tasks — Democratização do Extrato (spec 004)

## Backlog

- [x] T001 — Habilitar H2 TCP Server no accounts-service (`H2ServerConfig.java`, porta 9092)
- [x] T002 — Atualizar `application.properties` com `DB_CLOSE_DELAY=-1` para manter o banco vivo via TCP
- [x] T003 — Remover `<scope>runtime</scope>` do H2 no `pom.xml` (necessário para compilar `H2ServerConfig`)
- [x] T004 — Criar `services/data-lake/glue_job.py` com PySpark lendo via JDBC e salvando Parquet
- [x] T005 — Implementar cálculo de `opening_balance` e `closing_balance` por day com window functions
- [x] T006 — Salvar Parquet particionado por `account_id` e `date` em modo `overwrite`
- [x] T007 — Criar `services/data-lake/query_daily.py` para consulta ad-hoc dos Parquet
- [x] T008 — Criar `services/data-lake/run_job.sh` como ponto de entrada do job
- [x] T009 — Criar `services/data-lake/requirements.txt`
- [x] T010 — Criar `specs/004-democratizacao-extrato/spec.md`
- [x] T011 — Criar `docs/adr/ADR-004-democratizacao-extrato-parquet.md`
- [x] T012 — Atualizar `docs/adr/README.md` com entrada do ADR-004
- [x] T013 — Atualizar `README.md` com nova camada de arquitetura (data lake)
