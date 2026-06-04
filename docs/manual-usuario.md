# 📖 Manual do Usuário — smanioto-bank

Este guia explica como usar o banco digital do zero: desde criar sua conta até realizar transferências.

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
- [Sair do sistema](#-sair-do-sistema)
- [Dicas e observações](#-dicas-e-observações)

---

## ⚙️ Antes de começar

Os serviços precisam estar no ar. Na raiz do projeto, execute:

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

> **Importante:** o banco usa banco de dados em memória (H2). Todos os dados são perdidos ao parar os serviços com `./stop.sh`. Você precisará repetir os passos 1 a 3 a cada reinicialização.

---

## 👤 Passo 1 — Registrar credenciais

O cadastro de usuário é feito via API (não há tela de cadastro no frontend). Abra um terminal e execute:

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "joao", "password": "senha123"}'
```

**Resposta esperada:** `201 Created` (sem corpo).

> Guarde bem o `username` e `password` — você vai precisar deles no login.

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

**Resposta esperada:** `201 Created` com os dados do cliente.

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "fullName": "João da Silva",
  "cpf": "12345678909",
  "email": "joao@email.com"
}
```

> **Salve o campo `id`** — você precisará dele no próximo passo para abrir a conta.

---

## 🏦 Passo 3 — Abrir sua conta bancária

Use o `id` do cliente obtido no passo anterior:

```bash
curl -X POST http://localhost:8082/accounts \
  -H "Content-Type: application/json" \
  -d '{"customerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"}'
```

Substitua o valor de `customerId` pelo `id` real retornado no passo 2.

**Resposta esperada:** `201 Created` com os dados da conta.

```json
{
  "id": "f1e2d3c4-b5a6-7890-1234-abcdef567890",
  "bank": "341",
  "branch": "0001",
  "accountNumber": "00012345-6",
  "balance": 0.00,
  "customerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

> **Salve o campo `id` da conta** — você precisará dele toda vez que fizer login no frontend.

---

## 🔑 Passo 4 — Fazer login no frontend

Acesse **http://localhost:3000** no navegador. Você verá a tela de login:

```
┌─────────────────────────────────┐
│         smanioto-bank           │
│        Banco Digital PF         │
│                                 │
│  Usuário  [________________]    │
│  Senha    [________________]    │
│  ID Conta [________________]    │
│                                 │
│         [ Entrar ]              │
└─────────────────────────────────┘
```

Preencha os três campos:

| Campo | O que colocar |
|---|---|
| **Usuário** | O `username` criado no passo 1 (ex: `joao`) |
| **Senha** | A `password` criada no passo 1 (ex: `senha123`) |
| **ID da Conta** | O `id` da conta criada no passo 3 (UUID longo) |

Clique em **Entrar**. Se as informações estiverem corretas, você será redirecionado para a tela da conta.

---

## 💰 Passo 5 — Ver sua conta e saldo

Após o login, você verá o painel principal da conta:

```
┌─────────────────────────────────┐
│ smanioto-bank            [Sair] │
│                                 │
│  Banco          341             │
│  Agência        0001            │
│  Número da Conta  00012345-6    │
│  Saldo Disponível  R$ 0,00      │
│                                 │
│  [Ver Extrato]  [Transferir]    │
└─────────────────────────────────┘
```

A tela exibe automaticamente todos os dados da sua conta e o saldo atual.

---

## 📊 Passo 6 — Consultar extrato

Na tela da conta, clique em **Ver Extrato**. A tela mostrará todas as movimentações em ordem cronológica:

| Data | Tipo | Descrição | Valor (R$) |
|---|---|---|---|
| 04/06/2026 10:30 | CRÉDITO | Transferência recebida | +500,00 |
| 04/06/2026 11:00 | DÉBITO | Transferência enviada | -200,00 |

Se não houver movimentações ainda, aparecerá a mensagem:  
*"Nenhuma movimentação registrada ainda."*

Clique em **← Voltar para Conta** para retornar ao painel.

---

## 🔄 Passo 7 — Realizar transferência

Na tela da conta, clique em **Transferir**. Você verá o formulário de transferência:

```
┌─────────────────────────────────┐
│ Transferência            [Sair] │
│ ← Voltar para Conta             │
│                                 │
│  ID da Conta Destino            │
│  [____________________________] │
│                                 │
│  Valor (R$)                     │
│  [____________________________] │
│                                 │
│         [ Transferir ]          │
└─────────────────────────────────┘
```

| Campo | O que colocar |
|---|---|
| **ID da Conta Destino** | O `id` (UUID) da conta que vai receber o dinheiro |
| **Valor (R$)** | O valor a transferir (ex: `150.00`) |

Clique em **Transferir**. Uma mensagem de sucesso ou erro aparecerá logo abaixo do botão.

> **Atenção:** para transferir, você precisa do `id` da conta destino. Peça para a outra pessoa rodar o passo 3 e te passar o UUID da conta dela.

**Regras:**
- Saldo insuficiente → transferência bloqueada com mensagem de erro.
- O débito na sua conta e o crédito na conta destino são simultâneos (atômicos).

---

## 🚪 Sair do sistema

Clique no botão **Sair** disponível em qualquer tela. O sistema limpará sua sessão e redirecionará para a tela de login.

> A sessão também expira automaticamente após **1 hora** de inatividade.

---

## 💡 Dicas e observações

**Como testar com dois usuários ao mesmo tempo:**

Repita os passos 1 a 3 com credenciais diferentes para criar um segundo usuário e conta. Use uma aba anônima do navegador para logar com o segundo usuário ao mesmo tempo.

**Como encontrar o ID de uma conta:**

Se você perdeu o `id` da conta, consulte diretamente na API:

```bash
# Primeiro faça login para obter o token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "joao", "password": "senha123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

echo "Token: $TOKEN"
```

**Os dados somem depois de parar os serviços?**

Sim. O banco H2 é in-memory — foi uma escolha de projeto para manter o ambiente simples. Quando você rodar `./stop.sh` e `./start.sh` novamente, precisará recriar os dados (passos 1 a 3).

**O que fazer se o login falhar?**

| Mensagem | Causa provável |
|---|---|
| "Credenciais inválidas" | Username ou senha errados |
| Tela não avança | ID da conta está incorreto ou a conta não existe |
| Tela em branco / erro | Algum serviço caiu — verifique com `./start.sh` |
