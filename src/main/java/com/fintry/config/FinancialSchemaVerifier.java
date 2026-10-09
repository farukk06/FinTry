package com.fintry.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.List;

/** Hibernate type validation alone does not guarantee numeric precision/scale or CHECK constraints. */
@Component
@DependsOnDatabaseInitialization
public class FinancialSchemaVerifier implements InitializingBean {
    private final JdbcTemplate jdbc;
    public FinancialSchemaVerifier(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public void afterPropertiesSet() {
        for (String field : List.of("instruments.price", "portfolio_assets.quantity", "portfolio_assets.average_price",
                "transactions.quantity", "transactions.price", "virtual_accounts.balance",
                "transactions.total_amount", "balance_requests.requested_amount")) {
            String[] parts = field.split("\\.");
            boolean money = field.endsWith(".balance") || field.endsWith(".total_amount") || field.endsWith(".requested_amount");
            Integer count = jdbc.queryForObject("""
                    select count(*) from information_schema.columns
                    where table_schema=current_schema() and table_name=? and column_name=?
                    and data_type='numeric' and numeric_precision=? and numeric_scale=? and is_nullable='NO'
                    """, Integer.class, parts[0], parts[1], money ? 52 : 44, money ? 16 : 8);
            if (count == null || count != 1) throw notReady(field);
        }
        for (String constraint : List.of("ck_instruments_price", "ck_virtual_accounts_balance",
                "ck_portfolio_assets_quantity", "ck_portfolio_assets_average_price", "ck_transactions_quantity",
                "ck_transactions_price", "ck_transactions_total_amount", "ck_transactions_total_consistent",
                "ck_balance_requests_requested_amount", "uk_portfolio_user_instrument", "uk_virtual_account_user", "fk_balance_request_user")) {
            Integer count = jdbc.queryForObject("""
                    select count(*) from pg_constraint c join pg_namespace n on n.oid=c.connamespace
                    where n.nspname=current_schema() and c.conname=? and c.convalidated
                    """, Integer.class, constraint);
            if (count == null || count != 1) throw notReady(constraint);
        }
    }

    private IllegalStateException notReady(String field) {
        return new IllegalStateException("FinTry financial schema is not ready: " + field
                + ". No schema changes were executed by this check. Follow docs/financial-core-migration.md; "
                + "existing database migration requires separate approval.");
    }
}
