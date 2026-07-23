package com.devops_platform.backend.common;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerConfig {

    @ServiceConnection
    @Bean
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:16");
    }
}
