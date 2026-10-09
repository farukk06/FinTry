package com.fintry;

import com.fintry.dto.*;
import com.fintry.entity.*;
import com.fintry.exception.*;
import com.fintry.repository.*;
import com.fintry.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class FinancialCoreTests extends PostgreSqlTestSupport {
    @Autowired UserRepository users;
    @Autowired InstrumentRepository instruments;
    @Autowired VirtualAccountService accounts;
    @Autowired InstrumentService prices;
    @Autowired TransactionService trades;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired BalanceRequestService balanceRequests;
    Long userId;
    Long instrumentId;

    @BeforeEach void setup() {
        jdbc.execute("truncate transactions, portfolio_assets, balance_requests, virtual_accounts, instruments, users restart identity cascade");
        userId = users.save(User.builder().username("test").email("test@fintry.test").role(Role.USER).build()).getId();
        instrumentId = instruments.save(Instrument.builder().symbol("TEST").name("Test").type("STOCK").price(new BigDecimal("100")).build()).getId();
        accounts.createVirtualAccount(userId, new BigDecimal("1000"));
    }

    TradeRequest request(String quantity) {
        return TradeRequest.builder().userId(userId).instrumentId(instrumentId).quantity(new BigDecimal(quantity)).build();
    }

    @Test void fractionalQuantitySurvivesDatabaseReloadAndFullSale() {
        trades.buy(request("0.001"));
        assertThat(jdbc.queryForObject("select quantity from portfolio_assets", BigDecimal.class)).isEqualByComparingTo("0.001");
        assertThat(jdbc.queryForObject("select quantity from transactions", BigDecimal.class)).isEqualByComparingTo("0.001");
        assertThat(accounts.getByUserId(userId).getBalance()).isEqualByComparingTo("999.9");
        trades.sell(request("0.001"));
        assertThat(jdbc.queryForObject("select count(*) from portfolio_assets", Integer.class)).isZero();
        assertThat(accounts.getByUserId(userId).getBalance()).isEqualByComparingTo("1000");
    }

    @Test void eightPlaceInputsPreserveExactSixteenPlaceCashAndAverage() {
        BigDecimal price = new BigDecimal("0.12345678");
        BigDecimal quantity = new BigDecimal("0.87654321");
        prices.updateInstrumentPrice(instrumentId, price);
        trades.buy(request(quantity.toPlainString()));
        BigDecimal product = price.multiply(quantity);
        assertThat(jdbc.queryForObject("select total_amount from transactions", BigDecimal.class)).isEqualByComparingTo(product);
        assertThat(jdbc.queryForObject("select quantity from portfolio_assets", BigDecimal.class)).isEqualByComparingTo(quantity);
        assertThat(jdbc.queryForObject("select average_price from portfolio_assets", BigDecimal.class)).isEqualByComparingTo(price);
        assertThat(accounts.getByUserId(userId).getBalance()).isEqualByComparingTo(new BigDecimal("1000").subtract(product));
    }

    @Test void weightedAverageRetainsEightDecimalPlaces() {
        prices.updateInstrumentPrice(instrumentId, new BigDecimal("1.00000001"));
        trades.buy(request("1"));
        prices.updateInstrumentPrice(instrumentId, new BigDecimal("1.00000002"));
        trades.buy(request("1"));
        assertThat(jdbc.queryForObject("select average_price from portfolio_assets", BigDecimal.class)).isEqualByComparingTo("1.00000002");
    }

    @Test void legacyInvalidPriceCannotCreateTradeEvenWhenDatabaseConstraintIsAbsent() {
        trades.buy(request("1"));
        jdbc.execute("alter table instruments drop constraint ck_instruments_price");
        try {
            for (String price : new String[]{"-100", "0"}) {
                jdbc.execute("update instruments set price=" + price);
                assertThatThrownBy(() -> trades.buy(request("1"))).isInstanceOf(InvalidFinancialValueException.class);
                assertThatThrownBy(() -> trades.sell(request("1"))).isInstanceOf(InvalidFinancialValueException.class);
            }
            assertThat(accounts.getByUserId(userId).getBalance()).isEqualByComparingTo("900");
            assertThat(jdbc.queryForObject("select count(*) from transactions", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select quantity from portfolio_assets", BigDecimal.class)).isEqualByComparingTo("1");
        } finally {
            jdbc.execute("update instruments set price=100");
            jdbc.execute("alter table instruments add constraint ck_instruments_price check(price>0 and price<>'NaN'::numeric)");
        }
    }

    @Test void directServiceCallsRejectInvalidPricesAndInitialBalance() {
        for (String value : new String[]{"-1", "0", "0.000000001"}) {
            assertThatThrownBy(() -> prices.updateInstrumentPrice(instrumentId, new BigDecimal(value)))
                    .isInstanceOf(InvalidFinancialValueException.class);
            CreateInstrumentRequest request = new CreateInstrumentRequest();
            request.setPrice(new BigDecimal(value));
            assertThatThrownBy(() -> prices.createInstrument(request)).isInstanceOf(InvalidFinancialValueException.class);
        }
        assertThatThrownBy(() -> prices.updateInstrumentPrice(instrumentId, null)).isInstanceOf(InvalidFinancialValueException.class);
        assertThatThrownBy(() -> accounts.createVirtualAccount(userId, new BigDecimal("-5"))).isInstanceOf(InvalidFinancialValueException.class);
        assertThatThrownBy(() -> trades.buy(request("-1"))).isInstanceOf(InvalidFinancialValueException.class);
        assertThat(jdbc.queryForObject("select count(*) from transactions", Integer.class)).isZero();
    }

    @Test void concurrentBuysPreserveCloudExample() throws Exception {
        trades.buy(request("1"));
        assertThat(concurrent(true, () -> trades.buy(request("1")), () -> trades.buy(request("1"))))
                .allMatch(result -> !(result instanceof Throwable));
        assertThat(accounts.getByUserId(userId).getBalance()).isEqualByComparingTo("700");
        assertThat(jdbc.queryForObject("select quantity from portfolio_assets", BigDecimal.class)).isEqualByComparingTo("3");
        assertThat(jdbc.queryForObject("select count(*) from transactions", Integer.class)).isEqualTo(3);
    }

    @Test void concurrentFirstBuysCreateOnePosition() throws Exception {
        concurrent(true, () -> trades.buy(request("1")), () -> trades.buy(request("1")));
        assertThat(jdbc.queryForObject("select count(*) from portfolio_assets", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select quantity from portfolio_assets", BigDecimal.class)).isEqualByComparingTo("2");
        assertThat(accounts.getByUserId(userId).getBalance()).isEqualByComparingTo("800");
        assertThat(jdbc.queryForObject("select count(*) from transactions", Integer.class)).isEqualTo(2);
    }

    @Test void differentInstrumentsCannotOverspendSharedBalance() throws Exception {
        Long second = instruments.save(Instrument.builder().symbol("OTHER").name("Other").type("STOCK").price(new BigDecimal("100")).build()).getId();
        TradeRequest other = TradeRequest.builder().userId(userId).instrumentId(second).quantity(new BigDecimal("6")).build();
        List<Object> results = concurrent(true, () -> trades.buy(request("6")), () -> trades.buy(other));
        assertThat(results.stream().filter(InsufficientBalanceException.class::isInstance)).hasSize(1);
        assertThat(results.stream().filter(TransactionResponse.class::isInstance)).hasSize(1);
        assertThat(accounts.getByUserId(userId).getBalance()).isEqualByComparingTo("400");
        assertThat(jdbc.queryForObject("select count(*) from transactions", Integer.class)).isEqualTo(1);
    }

    @Test void concurrentSellsCannotOversell() throws Exception {
        trades.buy(request("2"));
        List<Object> results = concurrent(true, () -> trades.sell(request("1.5")), () -> trades.sell(request("1.5")));
        assertThat(results.stream().filter(InsufficientAssetException.class::isInstance)).hasSize(1);
        assertThat(results.stream().filter(TransactionResponse.class::isInstance)).hasSize(1);
        assertThat(jdbc.queryForObject("select quantity from portfolio_assets", BigDecimal.class)).isEqualByComparingTo("0.5");
        assertThat(accounts.getByUserId(userId).getBalance()).isEqualByComparingTo("950");
    }

    @Test void buyAndFullSellRemainConsistent() throws Exception {
        trades.buy(request("1"));
        assertThat(concurrent(true, () -> trades.buy(request("1")), () -> trades.sell(request("1"))))
                .allMatch(result -> result instanceof TransactionResponse);
        assertThat(jdbc.queryForObject("select quantity from portfolio_assets", BigDecimal.class)).isEqualByComparingTo("1");
        assertThat(accounts.getByUserId(userId).getBalance()).isEqualByComparingTo("900");
        assertThat(jdbc.queryForObject("select count(*) from transactions", Integer.class)).isEqualTo(3);
    }

    Long createBalanceRequest(String amount) {
        CreateBalanceRequest request = new CreateBalanceRequest();
        request.setUserId(userId);
        request.setRequestedAmount(new BigDecimal(amount));
        return balanceRequests.createRequest(request).getId();
    }

    @Test void parallelApprovalCreditsOnlyOnce() throws Exception {
        Long id = createBalanceRequest("100");
        List<Object> results = concurrentOn("balance_requests", id, () -> balanceRequests.approveRequest(id), () -> balanceRequests.approveRequest(id));
        assertThat(results.stream().filter(BusinessRuleException.class::isInstance)).hasSize(1);
        assertThat(results.stream().filter(BalanceRequestResponse.class::isInstance)).hasSize(1);
        assertThat(accounts.getByUserId(userId).getBalance()).isEqualByComparingTo("1100");
    }

    @Test void approvalAndTradeShareAccountLock() throws Exception {
        Long id = createBalanceRequest("100");
        assertThat(concurrent(true, () -> balanceRequests.approveRequest(id), () -> trades.buy(request("1"))))
                .noneMatch(result -> result instanceof Throwable);
        assertThat(accounts.getByUserId(userId).getBalance()).isEqualByComparingTo("1000");
        assertThat(jdbc.queryForObject("select quantity from portfolio_assets", BigDecimal.class)).isEqualByComparingTo("1");
    }

    @Test void separateApprovalsPreserveBothCredits() throws Exception {
        Long a = createBalanceRequest("100");
        Long b = createBalanceRequest("200");
        assertThat(concurrent(true, () -> balanceRequests.approveRequest(a), () -> balanceRequests.approveRequest(b)))
                .allMatch(result -> result instanceof BalanceRequestResponse);
        assertThat(accounts.getByUserId(userId).getBalance()).isEqualByComparingTo("1300");
    }

    @Test void historyFailureRollsBackCashAndPosition() {
        jdbc.execute("alter table transactions add constraint test_reject_buy check (type <> 'BUY')");
        try {
            assertThatThrownBy(() -> trades.buy(request("1"))).isInstanceOf(DataIntegrityViolationException.class);
            assertThat(accounts.getByUserId(userId).getBalance()).isEqualByComparingTo("1000");
            assertThat(jdbc.queryForObject("select count(*) from portfolio_assets", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from transactions", Integer.class)).isZero();
        } finally {
            jdbc.execute("alter table transactions drop constraint test_reject_buy");
        }
    }

    @Test void databaseRejectsDuplicatePositionAndOrphanRelations() {
        trades.buy(request("1"));
        assertThatThrownBy(() -> jdbc.update("insert into portfolio_assets(user_id,instrument_id,quantity,average_price) values(?,?,1,100)", userId, instrumentId))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.execute("insert into balance_requests(user_id,requested_amount,status,created_at) values(999999,1,'PENDING',current_timestamp)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("update portfolio_assets set instrument_id=999999"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("delete from users where id=?", userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void databaseRejectsInvalidFinancialValues() {
        for (String price : new String[]{"0", "-1", "'NaN'::numeric"}) {
            assertThatThrownBy(() -> jdbc.execute("update instruments set price=" + price)).isInstanceOf(DataIntegrityViolationException.class);
        }
        assertThatThrownBy(() -> jdbc.execute("update virtual_accounts set balance=-1")).isInstanceOf(DataIntegrityViolationException.class);
        trades.buy(request("1"));
        assertThatThrownBy(() -> jdbc.execute("update transactions set total_amount=101")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.execute("update portfolio_assets set quantity=0")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void missingUserAndNegativeRequestAreRejectedAtService() {
        CreateBalanceRequest request = new CreateBalanceRequest();
        request.setUserId(999999L);
        request.setRequestedAmount(BigDecimal.ONE);
        assertThatThrownBy(() -> balanceRequests.createRequest(request)).isInstanceOf(ResourceNotFoundException.class);
        request.setUserId(userId);
        request.setRequestedAmount(new BigDecimal("-1"));
        assertThatThrownBy(() -> balanceRequests.createRequest(request)).isInstanceOf(InvalidFinancialValueException.class);
        assertThat(jdbc.queryForObject("select count(*) from balance_requests", Integer.class)).isZero();
    }

    @SafeVarargs
    final List<Object> concurrent(boolean blockAccount, Supplier<?>... operations) throws Exception {
        return concurrentOn(blockAccount ? "virtual_accounts" : null, userId, operations);
    }

    @SafeVarargs
    final List<Object> concurrentOn(String lockedTable, Long key, Supplier<?>... operations) throws Exception {
        if (lockedTable != null && !List.of("virtual_accounts", "balance_requests").contains(lockedTable)) {
            throw new IllegalArgumentException("Unknown test lock target");
        }
        ExecutorService executor = Executors.newFixedThreadPool(operations.length);
        CountDownLatch ready = new CountDownLatch(operations.length);
        CountDownLatch start = new CountDownLatch(1);
        try (Connection blocker = dataSource.getConnection()) {
            blocker.setAutoCommit(false);
            if (lockedTable != null) {
                String keyColumn = "virtual_accounts".equals(lockedTable) ? "user_id" : "id";
                try (var statement = blocker.prepareStatement("select id from " + lockedTable + " where " + keyColumn + " = ? for update")) {
                    statement.setLong(1, key);
                    statement.executeQuery().close();
                }
            }
            List<Future<Object>> futures = new ArrayList<>();
            for (Supplier<?> operation : operations) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("Worker start timeout");
                    try { return operation.get(); } catch (RuntimeException ex) { return ex; }
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            if (lockedTable != null) {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
                int waiting;
                do {
                    waiting = jdbc.queryForObject("select count(*) from pg_stat_activity where datname = current_database() and wait_event_type = 'Lock' and lower(query) like ?", Integer.class, "%" + lockedTable + "%");
                    if (waiting == operations.length) break;
                    Thread.sleep(20);
                } while (System.nanoTime() < deadline);
                assertThat(waiting).as("Every worker must reach the PostgreSQL row lock").isEqualTo(operations.length);
            }
            blocker.rollback();
            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) results.add(future.get(10, TimeUnit.SECONDS));
            return results;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }
}
