Guia para criar um novo microsserviço no smanioto-bank.

Nome do serviço: $ARGUMENTS

Siga exatamente o padrão dos serviços existentes:

## 1. Estrutura de diretórios

```
services/<nome>-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/smanioto/bank/<nome>/
    │   │   ├── <Nome>ServiceApplication.java
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   └── service/
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/com/smanioto/bank/<nome>/
            ├── controller/
            └── service/
```

## 2. pom.xml

Copie o `pom.xml` de `services/people-service/` e ajuste:
- `<artifactId>`: `<nome>-service`
- `<name>`: nome legível

Dependências padrão: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `h2`, `spring-boot-starter-test`.

## 3. application.properties

```properties
server.port=<PRÓXIMA PORTA DISPONÍVEL>
spring.datasource.url=jdbc:h2:mem:<nome>db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
```

## 4. Convenções obrigatórias

- DTOs como `record` Java
- Injeção via construtor (sem `@Autowired` em campos)
- `UUID` como ID de todas as entidades
- Sem MapStruct ou ModelMapper — conversão manual
- Testes: `@ExtendWith(MockitoExtension.class)` para serviço, `@WebMvcTest` para controller

## 5. Registrar no start.sh e stop.sh

Adicionar `build_service` e `start_java_service` no `start.sh` com a porta escolhida.

## 6. Criar ADR se houver decisão arquitetural relevante

Use `/project:new-adr <título da decisão>`.
