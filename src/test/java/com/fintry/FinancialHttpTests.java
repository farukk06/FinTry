package com.fintry;

import com.fintry.entity.*;
import com.fintry.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FinancialHttpTests extends PostgreSqlTestSupport {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired InstrumentRepository instruments;
    @Autowired DataSource dataSource;
    Long userId;
    Long instrumentId;

    @BeforeEach void setup() {
        jdbc.execute("truncate transactions, portfolio_assets, balance_requests, virtual_accounts, instruments, users restart identity cascade");
        userId = users.save(User.builder().username("http").email("http@test").role(Role.USER).build()).getId();
        instrumentId = instruments.save(Instrument.builder().symbol("HTTP").name("HTTP").type("STOCK").price(new BigDecimal("100")).build()).getId();
    }

    @Test void priceEndpointsRejectInvalidValuesWithConsistentErrorBody() throws Exception {
        for (String price : new String[]{"0", "-100", "0.000000001", "1E+36", "abc"}) {
            mvc.perform(put("/instruments/{id}/price", instrumentId).param("price", price))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").isNotEmpty()).andExpect(jsonPath("$.timestamp").exists());
        }
        mvc.perform(put("/instruments/{id}/price", instrumentId)).andExpect(status().isBadRequest());
        for (String price : new String[]{"0", "-1", "null", "0.000000001"}) {
            mvc.perform(post("/instruments").contentType("application/json")
                    .content("{\"symbol\":\"BAD\",\"name\":\"Bad\",\"type\":\"STOCK\",\"price\":" + price + "}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        }
        assertThat(jdbc.queryForObject("select price from instruments", BigDecimal.class)).isEqualByComparingTo("100");
    }

    @Test void validTrailingZerosAreAcceptedAndMissingInstrumentIs404() throws Exception {
        mvc.perform(put("/instruments/{id}/price", instrumentId).param("price", "1.000000000"))
                .andExpect(status().isOk());
        mvc.perform(put("/instruments/999999/price").param("price", "1"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test void initialBalanceMustBeNonnegativeAndZeroIsAllowed() throws Exception {
        mvc.perform(post("/accounts/user/{id}", userId).param("balance", "-5"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(post("/accounts/user/{id}", userId).param("balance", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(0));
        mvc.perform(post("/accounts/user/{id}", userId).param("balance", "10"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }

    @Test void missingUserAndInvalidBalanceRequestsProduce404Or400() throws Exception {
        mvc.perform(post("/balance-requests").contentType("application/json")
                .content("{\"userId\":999999,\"requestedAmount\":100}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(post("/balance-requests").contentType("application/json")
                .content("{\"userId\":" + userId + ",\"requestedAmount\":-1}"))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from balance_requests", Integer.class)).isZero();
    }

    @Test void tradesRejectInvalidQuantitiesAndPreserveReloadedFraction() throws Exception {
        mvc.perform(post("/accounts/user/{id}", userId).param("balance", "1000")).andExpect(status().isOk());
        for (String quantity : new String[]{"0", "-1", "null", "0.000000001"}) {
            mvc.perform(post("/transactions/buy").contentType("application/json").content(tradeJson(quantity)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        }
        mvc.perform(post("/transactions/buy").contentType("application/json").content(tradeJson("0.001")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(0.001));
        mvc.perform(get("/transactions/user/{id}", userId)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(0.001)).andExpect(jsonPath("$[0].totalAmount").value(0.1));
        mvc.perform(get("/portfolio/user/{id}", userId)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(0.001)).andExpect(jsonPath("$[0].totalValue").value(0.1));
    }

    @Test void lockTimeoutReturns409WithoutAnyFinancialMutation() throws Exception {
        mvc.perform(post("/accounts/user/{id}", userId).param("balance", "1000")).andExpect(status().isOk());
        try (Connection blocker = dataSource.getConnection()) {
            blocker.setAutoCommit(false);
            try (var statement = blocker.prepareStatement("select id from virtual_accounts where user_id=? for update")) {
                statement.setLong(1, userId);
                statement.executeQuery().close();
            }
            mvc.perform(post("/transactions/buy").contentType("application/json").content(tradeJson("1")))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
            blocker.rollback();
        }
        assertThat(jdbc.queryForObject("select balance from virtual_accounts", BigDecimal.class)).isEqualByComparingTo("1000");
        assertThat(jdbc.queryForObject("select count(*) from transactions", Integer.class)).isZero();
    }

    @Test void concurrentAccountCreationReturnsOneSuccessAndOneConflict() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier start = new CyclicBarrier(2);
        try {
            Callable<Integer> create = () -> {
                start.await(5, TimeUnit.SECONDS);
                return mvc.perform(post("/accounts/user/{id}", userId).param("balance", "10"))
                        .andReturn().getResponse().getStatus();
            };
            Future<Integer> a = executor.submit(create);
            Future<Integer> b = executor.submit(create);
            assertThat(java.util.List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
            assertThat(jdbc.queryForObject("select count(*) from virtual_accounts", Integer.class)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private String tradeJson(String quantity) {
        return "{\"userId\":" + userId + ",\"instrumentId\":" + instrumentId + ",\"quantity\":" + quantity + "}";
    }
}
