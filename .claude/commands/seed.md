Popula o banco de dados com dados de teste.

Execute `./seed.sh` na raiz do projeto **depois que os serviços já estiverem rodando** (`./start.sh`).

O seed cria:
- 10 usuários no auth-service (admin/admin, alice/alice123, bob/bob123, ...)
- 10 clientes no people-service (com CPFs válidos)
- 10 contas bancárias no accounts-service com saldos entre R$ 100 e R$ 5.000

Use os dados gerados para testar transferências, extrato e autenticação no frontend em http://localhost:3000.
