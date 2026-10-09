package com.fintry;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FinTryApplicationTests extends PostgreSqlTestSupport {

    @Autowired JdbcTemplate jdbc;

    @Test
    void contextLoads() {
        assertThat(jdbc.queryForObject("select current_database()", String.class))
                .isEqualTo("fintry_isolated_test");
        assertThat(POSTGRES.getMappedPort(5432)).isNotEqualTo(5432);
        assertThat(jdbc.queryForObject("select version()", String.class)).contains("PostgreSQL 16");
    }

}
