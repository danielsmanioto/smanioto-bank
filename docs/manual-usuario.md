# 📖 Manual do Usuário — smanioto-bank

Este guia explica como usar o banco digital do zero: desde criar sua conta até realizar transferências e consultar o data lake de extrato diário.

---

## 📋 Sumário

- [Antes de começar](#-antes-de-começar)
- [Passo 1 — Registrar credenciais](#-passo-1--registrar-credenciais)
- [Passo 2 — Cadastrar seu perfil de cliente](#-passo-2--cadastrar-seu-perfil-de-cliente)
- [Passo 3 — Abrir sua conta bancária](#-passo-3--abrir-sua-conta-bancária)
- [Passo 4 — Fazer login no frontend](#-passo-4--fazer-login-no-frontend)
- [Passo 5 — Ver sua conta e saldo](#-passo-5--ver-sua-conta-e-saldo)
- [Passo 6 — Consultar extrato](#-passo-6--consultar-extrato)
- [Passo 7 — Realizar transferência](#-passo-7--realizar-transferência)
- [Passo 8 — Data Lake: extrato diário analítico](#-passo-8--data-lake-extrato-diário-analítico)
- [Sair do sistema](#-sair-do-sistema)
- [Dicas e observações](#-dicas-e-observações)

---

## ⚙️ Antes de começar

### Subir os serviços

Na raiz do projeto, execute:

```bash
./start.sh
```

Aguarde o health check confirmar que todos os serviços estão respondendo:

```
[3/3] Health check...

  ✓ auth-service     HTTP 401
  ✓ people-service   HTTP 405
  ✓ accounts-service HTTP 405
  ✓ frontend         HTTP 200
```

Abra o navegador em **http://localhost:3000** para acessar a interface.

### Atalho: usar dados prontos

Se quiser pular os passos 1 a 3 e já ter 10 usuários com contas e extrato populado, rode:

```bash
./seed.sh
```

Os usuários criados são: `admin/admin`, `alice/alice123`, `bob/bob123`, `carol/carol123` e outros. Os IDs das contas ficam salvos em `docs/seed-users.md`.

> **Importante:** o banco usa H2 in-memory. Todos os dados são perdidos ao parar os serviços. Você precisará repetir os passos 1 a 3 (ou rodar `./seed.sh`) a cada reinicialização.

---

## 👤 Passo 1 — Registrar credenciais

O cadastro de usuário é feito via API (não há tela de cadastro no frontend). Abra um terminal e execute:

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "joao", "password": "senha123"}'
```

**Resposta esperada:** `201 Created` (sem corpo).

> Guarde o `username` e `password` — você vai precisar deles no login.

---

## 🪪 Passo 2 — Cadastrar seu perfil de cliente

Com as credenciais criadas, cadastre seus dados pessoais de Pessoa Física:

```bash
curl -X POST http://localhost:8081/people \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "João da Silva",
    "cpf": "12345678909",
    "email": "joao@email.com"
  }'
```

> **CPF:** envie somente os 11 dígitos numéricos, sem pontos ou traço (ex: `12345678909`). O sistema valida o dígito verificador.

**Resposta esperada:** `201 Created`

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "fullName": "João da Silva",
  "cpf": "12345678909",
  "email": "joao@email.com"
}
```

> **Salve o campo `id`** — você precisará dele no próximo passo.

---

## 🏦 Passo 3 — Abrir sua conta bancária

Use o `id` do cliente obtido no passo anterior:

```bash
curl -X POST http://localhost:8082/accounts \
  -H "Content-Type: application/json" \
  -d '{"customerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"}'
```

**Resposta esperada:** `201 Created`

```json
{
  "id": "f1e2d3c4-b5a6-7890-1234-abcdef567890",
  "bank": "341",
  "branch": "0001",
  "number": "00012345-6",
  "balance": 0.00,
  "customerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

> **Salve o campo `id` da conta** — você precisará dele no login do frontend e para o data lake.

---

## 🔑 Passo 4 — Fazer login no frontend

Acesse **http://localhost:3000** no navegador e preencha os três campos:

| Campo | O que colocar |
|---|---|
| **Usuário** | O `username` criado no passo 1 (ex: `joao`) |
| **Senha** | A `password` criada no passo 1 (ex: `senha123`) |
| **ID da Conta** | O `id` da conta criada no passo 3 (UUID) |

Clique em **Entrar**. Se as informações estiverem corretas, você será redirecionado para a tela da conta.

---

## 💰 Passo 5 — Ver sua conta e saldo

Após o login você verá o painel com banco, agência, número da conta e saldo atual atualizado em tempo real.

---

## 📊 Passo 6 — Consultar extrato

Na tela da conta, clique em **Ver Extrato**. A tela mostrará todas as movimentações em ordem cronológica:

| Data | Tipo | Descrição | Valor (R$) |
|---|---|---|---|
| 04/06/2026 10:30 | CRÉDITO | Transferência recebida | +500,00 |
| 04/06/2026 11:00 | DÉBITO | Transferência enviada | -200,00 |

Clique em **← Voltar para Conta** para retornar ao painel.

---

## 🔄 Passo 7 — Realizar transferência

Na tela da conta, clique em **Transferir** e preencha:

| Campo | O que colocar |
|---|---|
| **ID da Conta Destino** | O `id` (UUID) da conta que vai receber o dinheiro |
| **Valor (R$)** | O valor a transferir (ex: `150.00`) |

**Regras:**
- Saldo insuficiente → transferência bloqueada com mensagem de erro.
- O débito e o crédito são simultâneos (atômicos) — ou os dois acontecem ou nenhum.
- Só é possível transferir entre contas do mesmo banco.

---

## 📈 Passo 8 — Data Lake: extrato diário analítico

O data lake processa os dados do banco operacional e gera uma visão analítica com saldo inicial, créditos, débitos e saldo final por dia e por conta. É executado separadamente dos serviços.

### Pré-requisitos

- Serviços rodando (`./start.sh`)
- Dados populados (`./seed.sh` ou passos 1 a 3)
- Python 3 instalado

### Executar o pipeline

Na raiz do projeto:

```bash
./datalake.sh
```

O script verifica se o `accounts-service` está acessível, instala as dependências Python automaticamente (`pyspark`, `pandas`, `pyarrow`) e processa os dados. Ao final exibe os comandos de consulta.

### Consultar os dados gerados

```bash
cd services/data-lake

# Listar todas as contas disponíveis no data lake
python3 query_daily.py --list-accounts

# Extrato diário completo de uma conta
python3 query_daily.py --account <uuid-da-conta>

# Extrato de uma data específica
python3 query_daily.py --account <uuid-da-conta> --date 2026-06-06

# Extrato de um período
python3 query_daily.py --account <uuid-da-conta> --from 2026-06-01 --to 2026-06-06
```

### O que o data lake calcula

Para cada conta e cada dia com movimentações:

| Campo | Descrição |
|---|---|
| `opening_balance` | Saldo no início do dia |
| `total_credits` | Soma dos créditos do dia |
| `total_debits` | Soma dos débitos do dia |
| `closing_balance` | Saldo no final do dia |
| `movement_count` | Número de lançamentos |
| `transactions` | Lista detalhada dos lançamentos |

Os arquivos Parquet ficam em `services/data-lake/output/daily_statement/` particionados por `account_id/date`.

> **Nota:** o data lake sempre reprocessa todo o histórico. Se você rodar `./datalake.sh` novamente, os arquivos anteriores são sobrescritos com os dados mais recentes.

---

## 🚪 Sair do sistema

Clique no botão **Sair** disponível em qualquer tela. O sistema limpará sua sessão e redirecionará para o login.

> A sessão expira automaticamente após **1 hora** de inatividade.

---

## 💡 Dicas e observações

**Como testar com dois usuários ao mesmo tempo:**

Repita os passos 1 a 3 com credenciais diferentes. Use uma aba anônima do navegador para logar com o segundo usuário simultaneamente.

**Como encontrar o ID de uma conta perdida:**

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "joao", "password": "senha123"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

echo "Token: $TOKEN"
```

**Os dados somem depois de parar os serviços?**

Sim. O banco H2 é in-memory — escolha de projeto para manter o ambiente simples (ver ADR-001). Use `./seed.sh` para repopular rapidamente.

**O que fazer se o login falhar?**

| Mensagem | Causa provável |
|---|---|
| "Credenciais inválidas" | Username ou senha errados |
| Tela não avança | ID da conta incorreto ou conta inexistente |
| Tela em branco / erro | Algum serviço caiu — verifique com `./logs.sh -e` |

**O data lake não encontra dados?**

Certifique-se de ter rodado `./seed.sh` ou criado pelo menos uma transferência antes de executar `./datalake.sh`. O data lake só processa contas que têm movimentações registradas.
