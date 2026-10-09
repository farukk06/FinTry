package com.fintry;

import org.junit.jupiter.api.Test;
import org.flywaydb.core.api.FlywayException;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class AuthenticationMigrationTests extends PostgreSqlTestSupport {
    private final MigrationTests support = new MigrationTests();
    private MigrationTests.Database database(String target) { return support.database(target); }
    private org.flywaydb.core.Flyway flyway(String schema, String target) { return support.flyway(schema, target); }
    private void seed(org.springframework.jdbc.core.JdbcTemplate jdbc) { support.seed(jdbc); }
    @Test void v2ToV3PreservesIdentitiesRolesAndEveryFinancialRow() {
        var db=database("2"); seed(db.jdbc());
        db.jdbc().execute("insert into users(id,username,email,role) values(2,'admin','admin@test','ADMIN')");
        var identities=db.jdbc().queryForList("select id,username,email,role from users order by id");
        var tables=List.of("instruments","virtual_accounts","portfolio_assets","transactions","balance_requests");
        var before=tables.stream().map(t -> db.jdbc().queryForList("select * from "+t+" order by id")).toList();
        var migration=flyway(db.schema(),"3"); migration.migrate(); migration.validate();
        assertThat(db.jdbc().queryForList("select id,username,email,role from users order by id")).isEqualTo(identities);
        assertThat(tables.stream().map(t -> db.jdbc().queryForList("select * from "+t+" order by id")).toList()).isEqualTo(before);
        assertThat(db.jdbc().queryForList("select enabled from users",Boolean.class)).containsOnly(false);
        assertThat(db.jdbc().queryForObject("select count(*) from users where password_hash is not null",Integer.class)).isZero();
        assertThat(db.jdbc().queryForObject("select balance from virtual_accounts",BigDecimal.class)).isEqualByComparingTo("999999999999999999999999999999999999.99");
    }
    @Test void ambiguousLegacyEmailsAbortV3WithoutModifyingV2() {
        var db=database("2"); seed(db.jdbc());
        db.jdbc().execute("insert into users(id,username,email,role) values(2,'duplicate','LEGACY@test','USER')");
        assertThatThrownBy(() -> flyway(db.schema(),"3").migrate()).isInstanceOf(FlywayException.class).hasMessageContaining("ambiguous email");
        assertThat(db.jdbc().queryForObject("select count(*) from information_schema.columns where table_schema=current_schema() and table_name='users' and column_name='password_hash'",Integer.class)).isZero();
        assertThat(db.jdbc().queryForObject("select count(*) from users",Integer.class)).isEqualTo(2);
    }
    @Test void enabledIdentityRequiresHashAndNormalizedEmailIsUnique() {
        var db=database("3"); seed(db.jdbc());
        assertThatThrownBy(() -> db.jdbc().execute("update users set enabled=true"))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> db.jdbc().execute("insert into users(id,username,email,role) values(2,'duplicate',' LEGACY@test ','USER')"))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
