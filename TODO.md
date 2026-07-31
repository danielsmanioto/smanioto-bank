# TODO — smanioto-bank

Lista de tarefas do projeto. Marque `[x]` quando concluído.

## Frontend

- [x] Extrato — opção de salvar/exportar em PDF (`statement.html`, via impressão do navegador)
- [ ] Extrato — filtro por período (data inicial/final)
- [ ] Extrato — paginação para contas com muitas movimentações
- [ ] Feedback visual de loading nas telas (login, conta, extrato, transferência)

## Segurança (Fase 4 — Hardening)

- [ ] Rate limiting no auth-service (login/registro)
- [ ] Expiração e renovação (refresh) de token JWT
- [ ] Validação de força de senha no cadastro
- [ ] Auditoria de tentativas de login inválidas

## Testes

- [ ] Testes de integração entre auth-service, people-service e accounts-service
- [ ] Testes end-to-end do fluxo completo (cadastro → login → transferência → extrato)

## Data-lake

- [ ] Job incremental com watermark (spec 005 futura) — hoje o job sempre reprocessa tudo

## Roadmap / Futuro

- [ ] Integração com outros bancos (base para PIX — spec 002)
- [ ] Migração de H2 para PostgreSQL (ver ADR-001)
