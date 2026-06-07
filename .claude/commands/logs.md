Exibe os logs do serviço especificado em $ARGUMENTS.

Serviços disponíveis: `auth-service`, `people-service`, `accounts-service`, `frontend`.

**Exemplo de uso:**
- `/project:logs auth-service` → tail dos logs do auth-service

Execute:
```bash
tail -f .logs/<nome-do-servico>.log
```

Os logs ficam em `.logs/` na raiz do projeto e são sobrescritos a cada `./start.sh`.
