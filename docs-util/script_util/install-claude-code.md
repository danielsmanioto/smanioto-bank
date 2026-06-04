# Guia de Instalação — Claude Code (VS Code + Terminal)

## Pré-requisitos

| Requisito | Versão mínima |
|-----------|---------------|
| macOS | 13 (Ventura) ou superior |
| VS Code | 1.98.0 ou superior |
| Node.js | 18 ou superior |

---

## 1. Instalar o Node.js (se não tiver)

Verifique se já tem instalado:

```bash
node --version
```

Se não tiver ou for menor que v18, instale via [nvm](https://github.com/nvm-sh/nvm):

```bash
nvm install 20
nvm use 20
```

---

## 2. Instalar o Claude Code CLI

```bash
npm install -g @anthropic-ai/claude-code
```

---

## 3. Fazer login e configurar

```bash
claude
```

Na primeira execução, abrirá o fluxo de autenticação no navegador. Faça login com sua **conta Anthropic** (plano Pro ou Max necessário).

---

## 4. Instalar a extensão no VS Code

Abra o VS Code e:

1. Pressione `Cmd+Shift+X` para abrir as extensões
2. Pesquise por **Claude Code**
3. Clique em **Install**

Ou instale direto pelo link:
```
vscode:extension/anthropic.claude-code
```

---

## 5. Fazer login na extensão

1. Clique no ícone de **faísca ✱** no canto superior direito do editor (precisa ter um arquivo aberto)
2. Clique em **Sign in**
3. Autorize no navegador

> **Alternativa:** clique em **✱ Claude Code** na barra de status (canto inferior direito) — funciona mesmo sem arquivo aberto.

---

## Usando no Terminal

Dentro do seu projeto, basta rodar:

```bash
claude
```

Claude terá acesso a todos os arquivos do projeto e poderá ler, editar e criar arquivos diretamente.

---

## Usando no VS Code

- **Abrir painel:** clique na faísca ✱ no topo direito do editor
- **Referenciar arquivos:** digite `@` seguido do nome do arquivo no prompt
- **Atalhos úteis:**

| Ação | Mac | Windows/Linux |
|------|-----|---------------|
| Alternar foco editor/Claude | `Cmd+Esc` | `Ctrl+Esc` |
| Nova conversa em aba | `Cmd+Shift+Esc` | `Ctrl+Shift+Esc` |
| Inserir referência de arquivo | `Option+K` | `Alt+K` |
| Abrir Command Palette | `Cmd+Shift+P` | `Ctrl+Shift+P` |

---

## Solução de Problemas

### `zsh: command not found: claude`
O CLI não foi instalado. Rode:
```bash
npm install -g @anthropic-ai/claude-code
```

### `Symbol not found: _ubrk_clone`
Seu macOS é muito antigo (requer macOS 13+). Atualize o sistema operacional.

### Ícone da faísca não aparece no VS Code
- Abra um arquivo (não só uma pasta)
- Verifique se a versão do VS Code é 1.98.0+
- Rode `Developer: Reload Window` no Command Palette

### Login não avança
Rode no Command Palette:
```
Developer: Reload Window
```
Ou abra o VS Code pelo terminal para herdar as variáveis de ambiente:
```bash
code .
```

---

## Links Úteis

- [Documentação oficial](https://code.claude.com/docs/en/vs-code)
- [Claude.ai](https://claude.ai)
- [Reportar problemas](https://github.com/anthropics/claude-code/issues)