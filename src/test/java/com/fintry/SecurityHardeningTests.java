package com.fintry;

import com.fintry.dto.RegisterRequest;
import com.fintry.service.AuthService;
import com.fintry.security.TokenService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="fintry.security.trusted-proxies=10.0.0.10")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class SecurityHardeningTests extends PostgreSqlTestSupport {
    private static final String PASSWORD="hardening-test-password";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired AuthService auth;
    @Autowired TokenService tokens;

    @BeforeEach void reset() {
        jdbc.execute("truncate transactions, portfolio_assets, balance_requests, virtual_accounts, instruments, users restart identity cascade");
    }
    private MockHttpServletRequestBuilder register(String username, String email, String client) {
        return post("/auth/register").with(req -> { req.setRemoteAddr(client); return req; })
                .contentType("application/json").content("{\"username\":\""+username+"\",\"email\":\""+email+"\",\"password\":\""+PASSWORD+"\"}");
    }
    @Test void usernameAndEmailConflictsShareHonestResponseAndPreserveExistingIdentity(CapturedOutput output) throws Exception {
        var user=auth.register(new RegisterRequest("existing", "private-canary@test.example", PASSWORD));
        var before=jdbc.queryForList("select * from users");
        int offset=output.getAll().length();
        for (var request : java.util.List.of(register("existing","other@test.example","192.0.2.11"),
                register("other","PRIVATE-CANARY@test.example","192.0.2.11"))) {
            mvc.perform(request).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Registration could not be completed"))
                    .andExpect(jsonPath("$.id").doesNotExist()).andExpect(jsonPath("$.role").doesNotExist())
                    .andExpect(header().exists("X-Correlation-ID"));
        }
        assertThat(jdbc.queryForList("select * from users")).isEqualTo(before);
        assertThat(jdbc.queryForObject("select count(*) from virtual_accounts",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select balance from virtual_accounts where user_id=?",BigDecimal.class,user.getId())).isZero();
        assertThat(output.getAll().substring(offset)).contains("REGISTRATION_CONFLICT", "sqlState=23505")
                .doesNotContain("existing", "private-canary", "other@test", PASSWORD, "password_hash", "$2a$", "Failing row", "insert into");
        mvc.perform(register("created","created@test.example","192.0.2.12"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("USER"));
    }
    @Test void conflictingRegistrationsConsumeBudgetWithoutBlockingLogin() throws Exception {
        auth.register(new RegisterRequest("existing", "existing@test.example", PASSWORD));
        for(int i=0;i<5;i++) mvc.perform(register("existing","probe"+i+"@test.example","192.0.2.21"))
                .andExpect(status().isConflict());
        mvc.perform(register("newidentity","new@test.example","192.0.2.21")).andExpect(status().isTooManyRequests());
        mvc.perform(post("/auth/login").with(req -> { req.setRemoteAddr("192.0.2.21"); return req; })
                .contentType("application/json").content("{\"email\":\"existing@test.example\",\"password\":\""+PASSWORD+"\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from users",Integer.class)).isEqualTo(1);
    }
    @Test void jdbcAndHibernateSuppressFailingUserRowIncludingHash(CapturedOutput output) throws Exception {
        jdbc.execute("alter table users add constraint hardening_reject_identity check(username <> 'hashcanary')");
        try {
            int offset=output.getAll().length();
            mvc.perform(register("hashcanary","hash-canary@test.example","192.0.2.31"))
                    .andExpect(status().isInternalServerError()).andExpect(header().exists("X-Correlation-ID"));
            assertThat(output.getAll().substring(offset)).contains("FINANCIAL_INTEGRITY_FAILURE", "sqlState=23514")
                    .doesNotContain("hashcanary", "hash-canary", "password_hash", PASSWORD, "$2a$", "$2b$",
                            "Failing row", "insert into", "PSQLException", "ConstraintViolationException");
            // Verify PGJDBC itself redacts DETAIL, independently of logger levels/HTTP handling.
            assertThatThrownBy(() -> jdbc.update("insert into users(username,email,role,password_hash,enabled) values(?,?,?,?,?)",
                    "hashcanary", "jdbc-private-canary@test.example", "USER", "hash-private-canary", true))
                    .isInstanceOf(org.springframework.dao.DataAccessException.class)
                    .satisfies(ex -> assertThat(((org.springframework.dao.DataAccessException)ex).getMostSpecificCause().getMessage())
                            .doesNotContain("Failing row", "hashcanary", "jdbc-private-canary", "hash-private-canary"));
            assertThat(jdbc.queryForObject("select count(*) from users",Integer.class)).isZero();
        } finally { jdbc.execute("alter table users drop constraint hardening_reject_identity"); }
    }
    @Test void financialConstraintFailurePreservesStateAndLogsOnlySafeMetadata(CapturedOutput output) throws Exception {
        var user=auth.register(new RegisterRequest("operator","operator@test.example",PASSWORD));
        jdbc.update("update users set role='ADMIN' where id=?",user.getId());
        jdbc.execute("alter table instruments add constraint hardening_reject_symbol check(symbol <> 'PRIVATE_CANARY')");
        try {
            int offset=output.getAll().length();
            mvc.perform(post("/instruments").header("Authorization","Bearer "+tokens.issue(user.getId()))
                    .contentType("application/json").content("{\"symbol\":\"PRIVATE_CANARY\",\"name\":\"private-instrument-canary\",\"type\":\"STOCK\",\"price\":123.45}"))
                    .andExpect(status().isInternalServerError()).andExpect(header().exists("X-Correlation-ID"));
            assertThat(output.getAll().substring(offset)).contains("FINANCIAL_INTEGRITY_FAILURE", "sqlState=23514")
                    .doesNotContain("PRIVATE_CANARY", "private-instrument-canary", "123.45", "Failing row", "insert into", "PSQLException");
            assertThat(jdbc.queryForObject("select count(*) from instruments",Integer.class)).isZero();
        } finally { jdbc.execute("alter table instruments drop constraint hardening_reject_symbol"); }
    }
    @Test void trustedProxyClientsHaveIndependentHttpLoginBudgetsAndSpoofedPrefixDoesNotHelp() throws Exception {
        for (int i=0;i<10;i++) mvc.perform(proxyLogin("198.51.100.44, 192.0.2.41")).andExpect(status().isUnauthorized());
        mvc.perform(proxyLogin("198.51.100.55, 192.0.2.41")).andExpect(status().isTooManyRequests());
        mvc.perform(proxyLogin("192.0.2.42")).andExpect(status().isUnauthorized());
        mvc.perform(proxyLogin("example.com")).andExpect(status().isBadRequest());
    }
    private MockHttpServletRequestBuilder proxyLogin(String forwarded) {
        return post("/auth/login").with(req -> { req.setRemoteAddr("10.0.0.10"); return req; })
                .header("X-Forwarded-For",forwarded).contentType("application/json")
                .content("{\"email\":\"missing@test.example\",\"password\":\"wrong-password\"}");
    }
}
