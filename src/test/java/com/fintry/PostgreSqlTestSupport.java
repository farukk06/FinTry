package com.fintry;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** No fallback to application.properties: Docker failures must fail the test run. */
public abstract class PostgreSqlTestSupport {
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
            .withDatabaseName("fintry_isolated_test")
            .withUsername("fintry_test")
            .withPassword("isolated_test_only")
            .withReuse(false);

    private static final java.security.KeyPair KEYS = keys();
    private static java.security.KeyPair keys() {
        try {
            var generator = java.security.KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        properties.add("spring.flyway.enabled", () -> "true");
        properties.add("spring.jpa.show-sql", () -> "false");
        properties.add("server.port", () -> "0");
        properties.add("fintry.security.jwt.private-key", () -> java.util.Base64.getEncoder().encodeToString(KEYS.getPrivate().getEncoded()));
        properties.add("fintry.security.jwt.public-key", () -> java.util.Base64.getEncoder().encodeToString(KEYS.getPublic().getEncoded()));
    }
}
