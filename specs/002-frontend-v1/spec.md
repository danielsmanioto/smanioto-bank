# Feature Specification: Front-end v1 — Interface Web do Banco Digital

**Feature Branch**: `002-frontend-v1`

**Created**: 2026-06-04

**Status**: Draft

**Input**: User description: "Criar front-end v1 com HTML + CSS + JS — telas de login, visão de conta, extrato e transferência consumindo as APIs já prontas dos serviços auth-service, people-service e accounts-service."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Autenticar pela interface web (Priority: P1)

Como cliente PF com credenciais já cadastradas, quero acessar uma página de login no navegador, informar usuário e senha e entrar na minha área bancária sem precisar usar ferramentas de desenvolvedor ou APIs diretamente.

**Why this priority**: Sem login visual nenhuma outra tela é acessível. É o ponto de entrada obrigatório de toda a jornada do cliente na interface.

**Independent Test**: Pode ser testada abrindo a página de login no navegador, informando credenciais válidas e verificando redirecionamento para a tela de conta; e informando credenciais inválidas e verificando mensagem de erro sem redirecionamento.

**Acceptance Scenarios**:

1. **Given** um cliente com credenciais ativas, **When** ele informa usuário e senha corretos na tela de login e confirma, **Then** ele é redirecionado para a tela de visão de conta.
2. **Given** um cliente na tela de login, **When** ele informa usuário ou senha incorretos, **Then** o sistema exibe mensagem de falha de autenticação e não redireciona.
3. **Given** um cliente que tenta acessar diretamente uma tela protegida sem estar autenticado, **When** a página carrega, **Then** ele é redirecionado automaticamente para a tela de login.

---

### User Story 2 - Visualizar saldo e dados da conta (Priority: P1)

Como cliente autenticado, quero ver na tela os dados da minha conta (banco, agência, número) e meu saldo atual assim que entro na área bancária.

**Why this priority**: É a tela central de onde o cliente navega para as demais funcionalidades. Sem ela o cliente não sabe qual conta está operando nem qual o saldo disponível.

**Independent Test**: Pode ser testada após login bem-sucedido verificando que banco, agência, número da conta e saldo são exibidos corretamente na tela.

**Acceptance Scenarios**:

1. **Given** um cliente autenticado com conta aberta, **When** ele acessa a tela de conta, **Then** o sistema exibe banco, agência, número da conta e saldo atual.
2. **Given** um cliente autenticado, **When** ele clica em "Sair", **Then** a sessão é encerrada e ele é redirecionado para a tela de login.

---

### User Story 3 - Consultar extrato de movimentações (Priority: P1)

Como cliente autenticado, quero visualizar o histórico de lançamentos da minha conta em ordem cronológica para acompanhar minhas movimentações.

**Why this priority**: O extrato é o instrumento de controle financeiro do cliente. Sem ele o cliente não pode verificar se transferências foram realizadas corretamente.

**Independent Test**: Pode ser testada verificando que a tela de extrato exibe todos os lançamentos com tipo (débito/crédito), valor, data e descrição em ordem cronológica.

**Acceptance Scenarios**:

1. **Given** um cliente autenticado com movimentações registradas, **When** ele acessa a tela de extrato, **Then** ele visualiza os lançamentos em ordem cronológica com tipo, valor, data e descrição.
2. **Given** um cliente autenticado sem movimentações, **When** ele acessa a tela de extrato, **Then** o sistema exibe mensagem indicando extrato vazio.

---

### User Story 4 - Realizar transferência entre contas (Priority: P1)

Como cliente autenticado com saldo suficiente, quero preencher um formulário de transferência informando conta destino e valor para transferir dinheiro para outra conta do mesmo banco sem precisar usar APIs diretamente.

**Why this priority**: A transferência é a principal operação transacional do MVP. É o valor central do banco digital para o cliente.

**Independent Test**: Pode ser testada preenchendo o formulário com conta destino válida e saldo suficiente e verificando mensagem de sucesso e atualização do saldo; e testando rejeição com saldo insuficiente ou conta destino inválida.

**Acceptance Scenarios**:

1. **Given** um cliente autenticado com saldo suficiente, **When** ele informa conta destino válida e valor positivo e confirma a transferência, **Then** o sistema exibe confirmação de sucesso e o saldo é atualizado.
2. **Given** um cliente autenticado com saldo insuficiente, **When** ele tenta realizar uma transferência, **Then** o sistema exibe mensagem de rejeição sem alterar saldo.
3. **Given** um cliente autenticado, **When** ele informa conta destino inexistente, **Then** o sistema exibe mensagem de erro de conta destino não encontrada.

---

### Edge Cases

- Sessão expirada durante navegação: ao tentar uma ação protegida com token expirado, o sistema redireciona para login com mensagem informativa.
- Campos obrigatórios em branco no formulário de transferência: o sistema impede submissão e indica o campo faltante.
- Valor de transferência igual a zero ou negativo: o sistema impede submissão e exibe mensagem de validação.
- Perda de conectividade com os serviços: o sistema exibe mensagem de erro genérica sem travar a interface.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST exibir uma tela de login com campos para usuário e senha e botão de confirmação.
- **FR-002**: O sistema MUST redirecionar para a tela de conta após autenticação bem-sucedida.
- **FR-003**: O sistema MUST exibir mensagem de erro ao falhar o login, sem redirecionar o cliente.
- **FR-004**: O sistema MUST proteger as telas de conta, extrato e transferência, redirecionando para login quando não autenticado.
- **FR-005**: O sistema MUST exibir na tela de conta os campos: banco, agência, número da conta e saldo atual.
- **FR-006**: O sistema MUST oferecer botão de logout que encerra a sessão e redireciona para login.
- **FR-007**: O sistema MUST exibir na tela de extrato os lançamentos da conta do cliente em ordem cronológica com tipo, valor, data e descrição.
- **FR-008**: O sistema MUST exibir mensagem de extrato vazio quando não houver movimentações.
- **FR-009**: O sistema MUST exibir um formulário de transferência com campos para ID da conta destino e valor.
- **FR-010**: O sistema MUST exibir confirmação de sucesso após transferência aprovada.
- **FR-011**: O sistema MUST exibir mensagem de rejeição quando a transferência falhar, sem alterar a tela de saldo.
- **FR-012**: O sistema MUST validar campos obrigatórios no formulário de transferência antes de enviar.

### Key Entities

- **Sessão do Cliente**: Estado de autenticação mantido no navegador, associado ao token JWT obtido no login.
- **Dados de Conta**: Conjunto de informações da conta bancária visíveis ao cliente — banco, agência, número e saldo.
- **Lançamento de Extrato**: Registro de movimentação exibido no extrato — tipo, valor, data, descrição.
- **Formulário de Transferência**: Conjunto de inputs para ID da conta destino e valor a transferir.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: O cliente consegue fazer login e acessar a tela de conta em menos de 10 segundos em conexão local.
- **SC-002**: 100% das telas protegidas redirecionam para login quando acessadas sem autenticação válida.
- **SC-003**: O cliente consegue consultar extrato e localizar uma transferência específica em menos de 30 segundos.
- **SC-004**: O cliente consegue concluir uma transferência bem-sucedida em menos de 5 cliques a partir da tela de conta.
- **SC-005**: 100% das mensagens de erro do backend são exibidas de forma legível na interface, sem expor detalhes técnicos ao cliente.

## Assumptions

- O front-end v1 é executado localmente e se comunica diretamente com cada serviço backend rodando em localhost em portas distintas.
- Não há requisito de design elaborado nesta versão — interface funcional e legível é suficiente.
- O cliente PF e a conta bancária já foram cadastrados via API antes de usar o front-end; o front-end v1 não inclui tela de cadastro de cliente ou abertura de conta.
- A autenticação usa o token JWT retornado pelo auth-service, armazenado na sessão do navegador durante o uso.
- Suporte apenas a desktop/navegador moderno; responsividade mobile está fora do escopo do v1.
- Os serviços backend precisam permitir chamadas do front-end (CORS configurado para localhost).
