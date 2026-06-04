# Research: Front-end v1

## Decisão: Token JWT no localStorage vs sessionStorage

- **Decision**: sessionStorage
- **Rationale**: sessionStorage é apagado quando a aba/janela é fechada, o que é comportamento adequado para um banco. Não persiste entre sessões por padrão. Para MVP local, ambos são equivalentes em segurança, mas sessionStorage é semanticamente mais correto.
- **Alternatives considered**: localStorage (persiste após fechar o navegador — menos adequado para sessão bancária), cookie HttpOnly (requer backend configurado para emitir cookies — complexidade desnecessária no v1).

## Decisão: Servidor estático

- **Decision**: `python3 -m http.server 3000`
- **Rationale**: Zero dependências, já disponível em qualquer máquina com Python 3. Suficiente para servir arquivos estáticos em localhost.
- **Alternatives considered**: Node.js `http-server` (requer npm install), `live-server` (requer npm), nginx (overhead desnecessário para dev local).

## Decisão: Sem framework JS

- **Decision**: Vanilla JavaScript ES2022 (fetch API, async/await, módulos ES)
- **Rationale**: Alinhado com o `guia-dev.md` que especifica "HTML + CSS + JavaScript" explicitamente. Sem build step, sem `node_modules`, sem complexidade de bundler. Ideal para laboratório de aprendizado.
- **Alternatives considered**: React/Vue/Angular (overhead de tooling e aprendizado desnecessário para 4 telas simples).

## Decisão: CORS nos serviços backend

- **Decision**: `@CrossOrigin` em cada Controller + `application.properties` por serviço
- **Rationale**: Abordagem mais simples e explícita. Cada serviço declara explicitamente que aceita chamadas do front-end.
- **Alternatives considered**: `WebMvcConfigurer` global (mais elegante mas requer mais arquivos a criar por serviço).

## Decisão: Gestão de estado entre páginas

- **Decision**: sessionStorage para token JWT + URLSearchParams para passar `accountId` entre páginas
- **Rationale**: Zero framework, sem SPA routing. Cada página é um HTML independente que lê o token do sessionStorage e o accountId da URL. Simples e direto.
- **Alternatives considered**: SPA com hash routing (mais complexo, requer JS router).
