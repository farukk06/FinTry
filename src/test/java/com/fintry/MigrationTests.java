package com.fintry;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.math.BigDecimal;
import java.util.UUID;
import com.fintry.config.FinancialSchemaVerifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class MigrationTests extends PostgreSqlTestSupport {
    record Database(Flyway flyway, JdbcTemplate jdbc, String schema) { }

    Database database(String target) {
        String schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
        Flyway flyway = flyway(schema, target);
        flyway.migrate();
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl() + "&currentSchema=" + schema,
                POSTGRES.getUsername(), POSTGRES.getPassword());
        return new Database(flyway, new JdbcTemplate(dataSource), schema);
    }

    Flyway flyway(String schema, String target) {
        return Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema).defaultSchema(schema).target(target).cleanDisabled(true)
                .baselineOnMigrate(false).locations("classpath:db/migration").load();
    }

    void seed(JdbcTemplate jdbc) {
        jdbc.execute("insert into users(id,username,email,role) values(1,'legacy','legacy@test','USER')");
        jdbc.execute("insert into instruments(id,symbol,name,type,price) values(1,'OLD','Old','STOCK',100.25)");
        jdbc.execute("insert into virtual_accounts(user_id,balance) values(1,999999999999999999999999999999999999.99)");
        jdbc.execute("insert into portfolio_assets(user_id,instrument_id,quantity,average_price) values(1,1,2.50,100.25)");
        jdbc.execute("insert into transactions(user_id,instrument_id,type,quantity,price,total_amount,transaction_time) values(1,1,'BUY',2,100.25,200.50,current_timestamp)");
        jdbc.execute("insert into balance_requests(user_id,requested_amount,status,created_at) values(1,10.25,'PENDING',current_timestamp)");
    }

    @Test void freshDatabaseMigratesWithExpectedPrecision() {
        Database db = database("latest");
        assertThat(db.flyway().info().current().getVersion().toString()).isEqualTo("3");
        assertThat(db.jdbc().queryForObject("select numeric_scale from information_schema.columns where table_schema=current_schema() and table_name='transactions' and column_name='quantity'", Integer.class)).isEqualTo(8);
        assertThat(db.jdbc().queryForObject("select numeric_precision from information_schema.columns where table_schema=current_schema() and table_name='virtual_accounts' and column_name='balance'", Integer.class)).isEqualTo(52);
        assertThatCode(() -> new FinancialSchemaVerifier(db.jdbc()).afterPropertiesSet()).doesNotThrowAnyException();
    }

    @Test void legacyMigrationPreservesEveryValueAndIntegerCapacity() {
        Database db = database("1");
        seed(db.jdbc());
        var before = db.jdbc().queryForList("select balance from virtual_accounts");
        flyway(db.schema(), "latest").migrate();
        assertThat(db.jdbc().queryForObject("select balance from virtual_accounts", BigDecimal.class))
                .isEqualByComparingTo((BigDecimal) before.getFirst().get("balance"));
        assertThat(db.jdbc().queryForObject("select quantity from portfolio_assets", BigDecimal.class)).isEqualByComparingTo("2.50");
        assertThat(db.jdbc().queryForObject("select average_price from portfolio_assets", BigDecimal.class)).isEqualByComparingTo("100.25");
        assertThat(db.jdbc().queryForObject("select total_amount from transactions", BigDecimal.class)).isEqualByComparingTo("200.50");
        assertThat(db.jdbc().queryForObject("select requested_amount from balance_requests", BigDecimal.class)).isEqualByComparingTo("10.25");
        assertThat(db.jdbc().queryForObject("select count(*) from transactions", Integer.class)).isEqualTo(1);
    }

    @Test void existingSchemaRequiresExplicitBaseline() {
        Database db = database("1");
        seed(db.jdbc());
        // Simulate a Hibernate-managed legacy database, only inside this disposable test schema.
        db.jdbc().execute("drop table flyway_schema_history");
        Flyway upgrade = flyway(db.schema(), "latest");
        assertThatThrownBy(upgrade::migrate).isInstanceOf(FlywayException.class);
        upgrade.baseline();
        upgrade.migrate();
        assertThat(db.jdbc().queryForObject("select count(*) from portfolio_assets", Integer.class)).isEqualTo(1);
    }

    @Test void duplicatesAbortWithoutMergingOrChangingPrecision() {
        Database db = database("1");
        seed(db.jdbc());
        db.jdbc().execute("insert into portfolio_assets(user_id,instrument_id,quantity,average_price) values(1,1,1,100)");
        assertThatThrownBy(() -> flyway(db.schema(), "latest").migrate()).isInstanceOf(FlywayException.class)
                .hasMessageContaining("duplicate portfolio");
        assertThat(db.jdbc().queryForObject("select count(*) from portfolio_assets", Integer.class)).isEqualTo(2);
        assertThat(db.jdbc().queryForObject("select numeric_scale from information_schema.columns where table_schema=current_schema() and table_name='portfolio_assets' and column_name='quantity'", Integer.class)).isEqualTo(2);
    }

    @Test void orphanRequestAbortsWithoutDeletingData() {
        Database db = database("1");
        seed(db.jdbc());
        db.jdbc().execute("insert into balance_requests(user_id,requested_amount,status,created_at) values(999,1,'PENDING',current_timestamp)");
        assertThatThrownBy(() -> flyway(db.schema(), "latest").migrate()).isInstanceOf(FlywayException.class)
                .hasMessageContaining("orphan");
        assertThat(db.jdbc().queryForObject("select count(*) from balance_requests", Integer.class)).isEqualTo(2);
    }

    @Test void corruptedFractionalHistoryAbortsWithoutRewritingHistory() {
        Database db = database("1");
        seed(db.jdbc());
        db.jdbc().execute("update transactions set quantity=0, total_amount=0.1");
        assertThatThrownBy(() -> flyway(db.schema(), "latest").migrate()).isInstanceOf(FlywayException.class)
                .hasMessageContaining("invalid financial");
        assertThat(db.jdbc().queryForObject("select quantity from transactions", BigDecimal.class)).isZero();
        assertThat(db.jdbc().queryForObject("select total_amount from transactions", BigDecimal.class)).isEqualByComparingTo("0.1");
    }

    @Test void oldSchemaIsRejectedByReadOnlyStartupGuard() {
        Database db = database("1");
        assertThatThrownBy(() -> new FinancialSchemaVerifier(db.jdbc()).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("financial schema is not ready");
        assertThat(db.jdbc().queryForObject("select numeric_scale from information_schema.columns where table_schema=current_schema() and table_name='instruments' and column_name='price'", Integer.class)).isEqualTo(2);
    }

    @Test void unexpectedNumericSchemaIsNotSilentlyNarrowed() {
        Database db = database("1");
        db.jdbc().execute("alter table instruments alter column price type numeric(44,10)");
        assertThatThrownBy(() -> flyway(db.schema(), "latest").migrate()).isInstanceOf(FlywayException.class)
                .hasMessageContaining("unexpected legacy numeric");
        assertThat(db.jdbc().queryForObject("select numeric_scale from information_schema.columns where table_schema=current_schema() and table_name='instruments' and column_name='price'", Integer.class)).isEqualTo(10);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "update instruments set price=-100",
            "update instruments set price=0",
            "update virtual_accounts set balance=-5",
            "update portfolio_assets set quantity=0",
            "update portfolio_assets set average_price=0",
            "update balance_requests set requested_amount=0",
            "update transactions set total_amount=1",
            "update instruments set price='NaN'::numeric"
    })
    void invalidLegacyValuesAbortAndLeaveSchemaUnchanged(String corruptSql) {
        Database db = database("1");
        seed(db.jdbc());
        db.jdbc().execute(corruptSql);
        assertThatThrownBy(() -> flyway(db.schema(), "latest").migrate()).isInstanceOf(FlywayException.class)
                .hasMessageContaining("invalid financial");
        assertThat(db.jdbc().queryForObject("select numeric_scale from information_schema.columns where table_schema=current_schema() and table_name='virtual_accounts' and column_name='balance'", Integer.class)).isEqualTo(2);
        assertThat(db.jdbc().queryForObject("select count(*) from transactions", Integer.class)).isEqualTo(1);
    }
}
