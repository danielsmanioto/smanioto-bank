Inicia todos os serviços do smanioto-bank.

Execute o script `./start.sh` na raiz do projeto. Ele:
1. Compila os 3 serviços Java com Maven (skipTests)
2. Sobe auth-service (8080), people-service (8081), accounts-service (8082) e frontend (3000)
3. Faz health check em todos os endpoints

Se `JWT_SECRET` não estiver definido no ambiente, o script usa o valor padrão de desenvolvimento.

Após subir, verifique os logs em `.logs/` se algum serviço não responder.
