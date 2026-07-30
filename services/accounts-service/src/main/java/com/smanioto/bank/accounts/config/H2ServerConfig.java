package com.smanioto.bank.accounts.config;

import java.sql.SQLException;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class H2ServerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(H2ServerConfig.class);

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2TcpServer() throws SQLException {
        LOGGER.info("Configurando servidor H2 TCP na porta 9092...");
        Server server = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092");
        LOGGER.info("Servidor H2 TCP criado com sucesso - porta: 9092, allowOthers: true");
        return server;
    }
}
