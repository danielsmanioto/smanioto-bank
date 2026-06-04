# Plano Técnico — Spec 003

## Decisões técnicas

### Scripts shell
- Usar `bash` com `set -euo pipefail` para falhas explícitas.
- PIDs gravados em `.pids` (um por linha) na raiz do projeto.
- Logs gravados em `.logs/<serviço>.log`.
- `start.sh` aguarda ~8s após iniciar os JARs para que o Spring Boot esteja pronto antes de subir o frontend.
- `stop.sh` usa `kill -TERM` (graceful) e verifica se o processo existia antes de matar.

### Maven build
- `mvn package -q -DskipTests` para build rápido sem rodar testes.
- Detecta o JAR com `target/*.jar` — funciona independente da versão do artefato.

### Frontend
- `python3 -m http.server 3000` — sem dependências externas.

### README
- Badges via `shields.io` (estáticos, sem autenticação).
- Diagrama de arquitetura via Mermaid (renderizado nativamente pelo GitHub).
- Tabela de endpoints por serviço.
- Seção de fluxo de cadastro de ponta a ponta (exemplo prático).
