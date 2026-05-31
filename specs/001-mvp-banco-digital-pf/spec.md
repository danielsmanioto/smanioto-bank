# Feature Specification: MVP Banco Digital PF

**Feature Branch**: `[001-create-feature-branch]`

**Created**: 2026-05-31

**Status**: Draft

**Input**: User description: "Crie a especificação inicial do MVP para o projeto smanioto-bank com base no guia-dev.md: banco digital para PF com autenticação JWT, cadastro de cliente PF, conta bancária (banco, agência, número, saldo), extrato de movimentações, transferência interna entre contas com regra de saldo suficiente e registro atômico débito/crédito. Foque no início da estruturação e criação das specs em português."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Autenticar cliente PF (Priority: P1)

Como cliente pessoa física já cadastrado, quero entrar com minhas credenciais e receber uma sessão autenticada para acessar funcionalidades protegidas da conta.

**Why this priority**: Sem autenticação, não há acesso seguro às informações financeiras nem às operações de conta.

**Independent Test**: Pode ser testada isoladamente ao validar login com credenciais válidas e inválidas, e ao verificar que áreas protegidas exigem autenticação válida.

**Acceptance Scenarios**:

1. **Given** um cliente PF com credenciais ativas, **When** ele informa usuário e senha corretos, **Then** o sistema concede autenticação válida para acesso às rotas protegidas.
2. **Given** um cliente PF com credenciais ativas, **When** ele informa usuário ou senha incorretos, **Then** o sistema nega o acesso e informa falha de autenticação.
3. **Given** uma requisição para área protegida sem autenticação válida, **When** ela é processada, **Then** o sistema bloqueia o acesso.

---

### User Story 2 - Cadastrar cliente PF e conta bancária (Priority: P1)

Como operação inicial do banco digital, quero cadastrar um cliente PF e abrir sua conta com banco, agência, número e saldo inicial para habilitar movimentações.

**Why this priority**: O cadastro de cliente e criação de conta são pré-requisitos para qualquer transação e consulta de extrato.

**Independent Test**: Pode ser testada isoladamente criando cliente PF, abrindo conta vinculada e validando que os dados obrigatórios da conta foram registrados.

**Acceptance Scenarios**:

1. **Given** dados obrigatórios de cliente PF válidos, **When** o cadastro é solicitado, **Then** o cliente PF é registrado com sucesso.
2. **Given** um cliente PF existente, **When** a abertura de conta é solicitada com banco, agência, número e saldo inicial, **Then** a conta é criada e vinculada ao cliente.
3. **Given** uma tentativa de abrir conta sem cliente PF previamente cadastrado, **When** a solicitação é processada, **Then** o sistema rejeita a abertura.

---

### User Story 3 - Consultar extrato e transferir entre contas internas (Priority: P1)

Como cliente autenticado, quero consultar meu extrato e transferir valores para outra conta do mesmo banco para gerenciar meu dinheiro com rastreabilidade.

**Why this priority**: O valor central do MVP está em permitir movimentação real de saldo com histórico confiável de lançamentos.

**Independent Test**: Pode ser testada isoladamente executando transferência válida e inválida, verificando atualização de saldos e presença dos lançamentos no extrato de origem e destino.

**Acceptance Scenarios**:

1. **Given** duas contas internas ativas e saldo suficiente na conta de origem, **When** uma transferência interna é confirmada, **Then** o valor é debitado da origem, creditado no destino e ambos os lançamentos aparecem nos extratos.
2. **Given** saldo insuficiente na conta de origem, **When** a transferência interna é solicitada, **Then** a operação é recusada e nenhum saldo é alterado.
3. **Given** uma conta com movimentações registradas, **When** o cliente consulta o extrato, **Then** ele visualiza o histórico de lançamentos em ordem cronológica.

---

### Edge Cases

- Tentativa de transferência com valor igual ou menor que zero deve ser recusada sem alterar saldos.
- Tentativa de transferência para conta de destino inexistente deve ser recusada sem gerar lançamentos.
- Se ocorrer falha durante o processamento da transferência, o sistema deve garantir que débito e crédito não fiquem parcialmente aplicados.
- Requisições protegidas com autenticação inválida, expirada ou ausente devem ser negadas.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir cadastro de cliente pessoa física com os dados mínimos definidos para identificação do titular.
- **FR-002**: O sistema MUST permitir abertura de conta bancária apenas para cliente PF previamente cadastrado.
- **FR-003**: O sistema MUST registrar, para cada conta, os campos banco, agência, número da conta e saldo.
- **FR-004**: O sistema MUST autenticar clientes com usuário e senha e emitir autenticação baseada em JWT para acesso às funcionalidades protegidas.
- **FR-005**: O sistema MUST exigir autenticação válida em todas as operações protegidas de conta, extrato e transferência.
- **FR-006**: O sistema MUST disponibilizar consulta de extrato com histórico de movimentações da conta do cliente autenticado.
- **FR-007**: O sistema MUST permitir transferência interna entre contas do mesmo banco quando houver saldo suficiente na conta de origem.
- **FR-008**: O sistema MUST impedir transferência interna quando o saldo da conta de origem for insuficiente.
- **FR-009**: O sistema MUST registrar cada transferência interna com lançamento de débito na conta de origem e lançamento de crédito na conta de destino.
- **FR-010**: O sistema MUST garantir que débito e crédito da transferência interna sejam aplicados de forma atômica, sem estado parcial.
- **FR-011**: O sistema MUST preservar os saldos originais quando a transferência interna falhar em qualquer etapa.

### Key Entities *(include if feature involves data)*

- **Cliente PF**: Titular da conta bancária, identificado por dados pessoais e credenciais de acesso.
- **Credencial de Acesso**: Conjunto de usuário e senha associado ao cliente PF para autenticação.
- **Sessão Autenticada (JWT)**: Comprovante de autenticação utilizado para autorizar acesso às rotas protegidas.
- **Conta Bancária**: Conta de um cliente PF contendo banco, agência, número da conta e saldo atual.
- **Movimentação**: Registro financeiro associado a uma conta, incluindo tipo (débito/crédito), valor, data/hora e referência da operação.
- **Transferência Interna**: Operação entre conta de origem e conta de destino do mesmo banco, composta por débito e crédito vinculados.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% das tentativas de acesso a funcionalidades protegidas sem autenticação válida são bloqueadas.
- **SC-002**: 95% dos clientes PF conseguem concluir cadastro e abertura de conta sem intervenção manual na primeira tentativa.
- **SC-003**: 100% das transferências internas aprovadas geram atualização de saldo coerente entre origem e destino e dois lançamentos correspondentes no extrato.
- **SC-004**: 100% das transferências com saldo insuficiente são recusadas sem alteração de saldo.
- **SC-005**: Em testes de aceitação do MVP, usuários conseguem consultar o extrato e localizar uma transferência concluída em até 30 segundos.

## Assumptions

- O MVP atenderá exclusivamente clientes pessoa física (PF), sem suporte a pessoa jurídica nesta fase.
- Transferências contempladas nesta etapa são apenas entre contas internas do mesmo banco.
- Não haverá integração com transferência para outros bancos no MVP inicial.
- O saldo inicial da conta é informado no momento da abertura da conta.
- O acesso ao MVP ocorre por canais digitais autenticados, e a autenticação JWT é requisito obrigatório para rotas protegidas.
