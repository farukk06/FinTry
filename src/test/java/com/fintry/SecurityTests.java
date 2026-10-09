package com.fintry;

import com.fintry.dto.*;
import com.fintry.entity.*;
import com.fintry.repository.*;
import com.fintry.service.*;
import com.fintry.security.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityTests extends PostgreSqlTestSupport {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired InstrumentRepository instruments;
    @Autowired PasswordEncoder passwords;
    @Autowired TokenService tokens;
    @Autowired JwtEncoder encoder;
    @Autowired AuthService auth;
    @Autowired TransactionService trades;
    @Autowired PortfolioService portfolios;
    @Autowired VirtualAccountService accounts;
    @Autowired BalanceRequestService requests;
    @Autowired InstrumentService prices;
    @Autowired UserService userService;
    static final String PASSWORD = "test-password-123";
    static String hash;
    Long alice, bob, admin, instrument, request;
    String aliceToken, bobToken, adminToken;

    @BeforeEach void setup() {
        SecurityContextHolder.clearContext();
        jdbc.execute("truncate transactions, portfolio_assets, balance_requests, virtual_accounts, instruments, users restart identity cascade");
        if (hash == null) hash = passwords.encode(PASSWORD);
        alice = user("alice", Role.USER);
        bob = user("bob", Role.USER);
        admin = user("admin", Role.ADMIN);
        for (Long id : List.of(alice, bob, admin)) jdbc.update("insert into virtual_accounts(user_id,balance) values(?,1000)", id);
        instrument = instruments.save(Instrument.builder().symbol("SEC").name("Security").type("STOCK").price(new BigDecimal("100")).build()).getId();
        request = jdbc.queryForObject("insert into balance_requests(user_id,requested_amount,status,created_at) values(?,100,'PENDING',current_timestamp) returning id", Long.class, bob);
        aliceToken=tokens.issue(alice); bobToken=tokens.issue(bob); adminToken=tokens.issue(admin);
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    Long user(String name, Role role) {
        return users.save(User.builder().username(name).email(name+"@test.example").role(role).passwordHash(hash).enabled(true).build()).getId();
    }
    MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }
    String financialSnapshot() {
        return List.of("virtual_accounts", "portfolio_assets", "transactions", "instruments", "balance_requests").stream()
            .map(table -> jdbc.queryForList("select * from " + table + " order by id").toString()).toList().toString();
    }

    @Test void registrationHashesPasswordAssignsUserAndCreatesZeroAccount() throws Exception {
        mvc.perform(post("/auth/register").contentType("application/json").content("{\"username\":\"newuser\",\"email\":\"NEW@test.example\",\"password\":\""+PASSWORD+"\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(jsonPath("$.password").doesNotExist());
        var user = users.findByEmailIgnoreCase("new@test.example").orElseThrow();
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(passwords.matches(PASSWORD, user.getPasswordHash())).isTrue();
        assertThat(jdbc.queryForObject("select balance from virtual_accounts where user_id=?", BigDecimal.class, user.getId())).isZero();
        mvc.perform(post("/auth/login").contentType("application/json").content("{\"email\":\"NEW@test.example\",\"password\":\""+PASSWORD+"\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty()).andExpect(jsonPath("$.expiresIn").value(900))
            .andExpect(jsonPath("$.user.role").value("USER"));
    }

    @Test void registrationRollbackIncludesUserWhenAccountWriteFails() {
        jdbc.execute("alter table virtual_accounts add constraint reject_zero_test check(balance <> 0)");
        try {
            assertThatThrownBy(() -> auth.register(new RegisterRequest("rollback", "rollback@test.example", PASSWORD)))
                .isInstanceOf(RuntimeException.class);
            assertThat(users.findByEmailIgnoreCase("rollback@test.example")).isEmpty();
            assertThat(users.count()).isEqualTo(3);
        } finally { jdbc.execute("alter table virtual_accounts drop constraint reject_zero_test"); }
    }

    @Test void concurrentDuplicateRegistrationLeavesExactlyOneIdentityAndAccount() throws Exception {
        var pool=Executors.newFixedThreadPool(2);
        var start=new CyclicBarrier(2);
        try {
            Callable<Boolean> register=() -> {
                start.await(5, TimeUnit.SECONDS);
                try { auth.register(new RegisterRequest("duplicate", "duplicate@test.example", PASSWORD)); return true; }
                catch (com.fintry.exception.BusinessRuleException ex) { return false; }
            };
            var first=pool.submit(register); var second=pool.submit(register);
            assertThat(List.of(first.get(15,TimeUnit.SECONDS), second.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
            assertThat(jdbc.queryForObject("select count(*) from users where username='duplicate'", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from virtual_accounts a join users u on a.user_id=u.id where u.username='duplicate'", Integer.class)).isEqualTo(1);
        } finally { pool.shutdownNow(); assertThat(pool.awaitTermination(10,TimeUnit.SECONDS)).isTrue(); }
    }

    @Test void privilegeAndIdentityFieldsAreRejected() throws Exception {
        for (String field : List.of("\"role\":\"ADMIN\"", "\"enabled\":true", "\"passwordHash\":\"fake\"", "\"balance\":9999")) {
            mvc.perform(post("/auth/register").contentType("application/json").content("{\"username\":\"injected\",\"email\":\"inject@test.example\",\"password\":\""+PASSWORD+"\","+field+"}"))
                .andExpect(status().isBadRequest());
        }
        String before=financialSnapshot();
        mvc.perform(bearer(post("/transactions/buy"), aliceToken).contentType("application/json")
                .content("{\"userId\":"+bob+",\"instrumentId\":"+instrument+",\"quantity\":1}"))
            .andExpect(status().isBadRequest());
        mvc.perform(bearer(post("/balance-requests"), aliceToken).contentType("application/json")
                .content("{\"userId\":"+bob+",\"requestedAmount\":100}"))
            .andExpect(status().isBadRequest());
        assertThat(financialSnapshot()).isEqualTo(before);
    }

    @Test void invalidAndLegacyCredentialsHaveSameError() throws Exception {
        users.save(User.builder().username("legacy").email("legacy@test.example").role(Role.ADMIN).build());
        for (String email : List.of("alice@test.example", "missing@test.example", "legacy@test.example")) {
            mvc.perform(post("/auth/login").contentType("application/json").content("{\"email\":\""+email+"\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }
        mvc.perform(post("/auth/login").contentType("application/json").content("{\"email\":\"legacy@test.example\",\"password\":\""+PASSWORD+"\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test void shortAndMultibyteOverlongPasswordsAreRejected() throws Exception {
        for (String password : List.of("short", "ş".repeat(40))) {
            mvc.perform(post("/auth/register").contentType("application/json")
                .content("{\"username\":\"invalid\",\"email\":\"invalid@test.example\",\"password\":\""+password+"\"}"))
                .andExpect(status().isBadRequest());
        }
        assertThat(users.count()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings={"/auth/me", "/accounts/me", "/portfolio/me", "/transactions/me", "/instruments", "/balance-requests/me", "/users", "/admin/balance-requests"})
    void anonymousReadsRequireAuthentication(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }

    @Test void eachRoleCanReadOnlyItsOwnFinancialData() throws Exception {
        for (String path : List.of("/accounts", "/portfolio", "/transactions")) {
            mvc.perform(bearer(get(path+"/me"),aliceToken)).andExpect(status().isOk());
            mvc.perform(bearer(get(path+"/user/"+alice),aliceToken)).andExpect(status().isOk());
            mvc.perform(bearer(get(path+"/user/"+bob),aliceToken)).andExpect(status().isForbidden());
            mvc.perform(bearer(get(path+"/user/"+bob),adminToken)).andExpect(status().isForbidden());
            mvc.perform(bearer(get(path+"/user/"+admin),adminToken)).andExpect(status().isOk());
        }
    }

    @Test void adminOperationsRejectUserWithoutFinancialMutationAndAllowAdmin() throws Exception {
        String before=financialSnapshot();
        for (String token : List.of(aliceToken,bobToken)) {
            mvc.perform(bearer(get("/users"),token)).andExpect(status().isForbidden());
            mvc.perform(bearer(get("/admin/balance-requests"),token)).andExpect(status().isForbidden());
            mvc.perform(bearer(put("/instruments/{id}/price",instrument),token).param("price","200")).andExpect(status().isForbidden());
            mvc.perform(bearer(post("/instruments"),token).contentType("application/json").content("{\"symbol\":\"NEW\",\"name\":\"New\",\"type\":\"STOCK\",\"price\":1}"))
                .andExpect(status().isForbidden());
            mvc.perform(bearer(put("/balance-requests/{id}/approve",request),token)).andExpect(status().isForbidden());
        }
        assertThat(financialSnapshot()).isEqualTo(before);
        mvc.perform(bearer(get("/users"),adminToken)).andExpect(status().isOk()).andExpect(jsonPath("$[0].passwordHash").doesNotExist());
        mvc.perform(bearer(get("/admin/balance-requests"),adminToken)).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(request));
        mvc.perform(bearer(put("/instruments/{id}/price",instrument),adminToken).param("price","200")).andExpect(status().isOk());
        mvc.perform(bearer(post("/instruments"),adminToken).contentType("application/json").content("{\"symbol\":\"NEW\",\"name\":\"New\",\"type\":\"STOCK\",\"price\":1}"))
            .andExpect(status().isOk());
        mvc.perform(bearer(put("/balance-requests/{id}/approve",request),adminToken)).andExpect(status().isOk());
        mvc.perform(bearer(put("/balance-requests/{id}/approve",request),adminToken)).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select balance from virtual_accounts where user_id=?",BigDecimal.class,bob)).isEqualByComparingTo("1100");
    }

    @Test void allAnonymousFinancialWritesFailWithoutMutation() throws Exception {
        String before=financialSnapshot();
        for (String path : List.of("/transactions/buy","/transactions/sell","/balance-requests","/instruments","/users","/accounts/user/1"))
            mvc.perform(post(path).contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/instruments/{id}/price",instrument).param("price","200")).andExpect(status().isUnauthorized());
        mvc.perform(put("/balance-requests/{id}/approve",request)).andExpect(status().isUnauthorized());
        assertThat(financialSnapshot()).isEqualTo(before);
    }

    @Test void tradeAndRequestIdentityComeFromBearerToken() throws Exception {
        String body="{\"instrumentId\":"+instrument+",\"quantity\":1}";
        mvc.perform(bearer(post("/transactions/buy"),aliceToken).contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(bearer(post("/transactions/sell"),aliceToken).contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(bearer(post("/balance-requests"),aliceToken).contentType("application/json").content("{\"requestedAmount\":10}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(alice));
        mvc.perform(bearer(get("/balance-requests/me"),aliceToken)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        assertThat(jdbc.queryForList("select distinct user_id from transactions",Long.class)).containsExactly(alice);
        assertThat(jdbc.queryForObject("select balance from virtual_accounts where user_id=?",BigDecimal.class,bob)).isEqualByComparingTo("1000");
    }

    @Test void legacyPublicCreationPathsAreClosedEvenForAdmin() throws Exception {
        for (String token:List.of(aliceToken,adminToken)) {
            mvc.perform(bearer(post("/users"),token).contentType("application/json").content("{\"username\":\"bypass\",\"email\":\"bypass@test.example\"}"))
                .andExpect(status().isForbidden());
            mvc.perform(bearer(post("/accounts/user/{id}",alice),token).param("balance","99999")).andExpect(status().isForbidden());
        }
    }

    String signed(String issuer, List<String> audience, String subject, Instant expires, Instant notBefore) {
        var claims=JwtClaimsSet.builder().issuer(issuer).audience(audience).subject(subject)
            .issuedAt(Instant.now().minusSeconds(3600)).expiresAt(expires).notBefore(notBefore).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(),claims)).getTokenValue();
    }
    @Test void invalidClaimsSignaturesAndMalformedTokensAreRejected() throws Exception {
        Instant now=Instant.now();
        var invalid=List.of("invalid", aliceToken.substring(0,aliceToken.lastIndexOf('.')+1)+"AAAA",
            signed("wrong",List.of("fintry-web"),alice.toString(),now.plusSeconds(900),now.minusSeconds(1)),
            signed("fintry",List.of("wrong"),alice.toString(),now.plusSeconds(900),now.minusSeconds(1)),
            signed("fintry",List.of("fintry-web"),alice.toString(),now.minusSeconds(120),now.minusSeconds(3600)),
            signed("fintry",List.of("fintry-web"),alice.toString(),now.plusSeconds(900),now.plusSeconds(120)),
            signed("fintry",List.of("fintry-web"),"not-an-id",now.plusSeconds(900),now.minusSeconds(1)),
            signed("fintry",List.of("fintry-web"),"999999",now.plusSeconds(900),now.minusSeconds(1)));
        var generator=java.security.KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
        var pair=generator.generateKeyPair();
        var key=new com.nimbusds.jose.jwk.RSAKey.Builder((java.security.interfaces.RSAPublicKey)pair.getPublic())
            .privateKey((java.security.interfaces.RSAPrivateKey)pair.getPrivate()).build();
        var otherEncoder=new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableJWKSet<>(new com.nimbusds.jose.jwk.JWKSet(key)));
        var foreignClaims=JwtClaimsSet.builder().issuer("fintry").audience(List.of("fintry-web")).subject(alice.toString())
            .issuedAt(now).expiresAt(now.plusSeconds(900)).build();
        String foreignToken=otherEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(),foreignClaims)).getTokenValue();
        mvc.perform(bearer(get("/accounts/me"),foreignToken)).andExpect(status().isUnauthorized());
        // A different algorithm is never accepted even with otherwise valid claim bytes.
        String alteredAlgorithm=java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8))
            + aliceToken.substring(aliceToken.indexOf('.'));
        mvc.perform(bearer(get("/accounts/me"),alteredAlgorithm)).andExpect(status().isUnauthorized());
        for(String token:invalid)
            mvc.perform(bearer(get("/accounts/me"),token)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        var noExpiry=JwtClaimsSet.builder().issuer("fintry").audience(List.of("fintry-web")).subject(alice.toString()).issuedAt(now).build();
        String token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(),noExpiry)).getTokenValue();
        mvc.perform(bearer(get("/accounts/me"),token)).andExpect(status().isUnauthorized());
    }

    @Test void claimedAdminRoleCannotOverrideDatabaseRole() throws Exception {
        var claims=JwtClaimsSet.builder().issuer("fintry").audience(List.of("fintry-web")).subject(alice.toString())
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(900)).claim("role","ADMIN").build();
        String token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(),claims)).getTokenValue();
        mvc.perform(bearer(get("/users"),token)).andExpect(status().isForbidden());
    }

    @Test void loginBudgetReturns429WithConsistentErrorBody() throws Exception {
        for (int i=0;i<10;i++) {
            mvc.perform(post("/auth/login").with(req -> { req.setRemoteAddr("10.99.0.1"); return req; })
                    .contentType("application/json").content("{\"email\":\"missing@test.example\",\"password\":\"incorrect-password\"}"))
                .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/auth/login").with(req -> { req.setRemoteAddr("10.99.0.1"); return req; })
                .contentType("application/json").content("{\"email\":\"missing@test.example\",\"password\":\"incorrect-password\"}"))
            .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.status").value(429)).andExpect(jsonPath("$.timestamp").exists());
    }

    @Test void roleDowngradeAndDisableTakeEffectForExistingToken() throws Exception {
        jdbc.update("update users set role='USER' where id=?",admin);
        mvc.perform(bearer(put("/instruments/{id}/price",instrument),adminToken).param("price","200")).andExpect(status().isForbidden());
        jdbc.update("update users set enabled=false where id=?",alice);
        mvc.perform(bearer(get("/accounts/me"),aliceToken)).andExpect(status().isUnauthorized());
    }

    @Test void directServiceCallsCannotBypassOwnershipOrRoleChecks() {
        var authority=List.of(new SimpleGrantedAuthority("ROLE_USER"));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(alice.toString(),"",authority));
        String before=financialSnapshot();
        assertThatThrownBy(() -> accounts.getByUserId(bob)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> portfolios.getUserPortfolio(bob)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> trades.getTransactionsByUserId(bob)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> trades.buy(TradeRequest.builder().userId(bob).instrumentId(instrument).quantity(BigDecimal.ONE).build()))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> trades.sell(TradeRequest.builder().userId(bob).instrumentId(instrument).quantity(BigDecimal.ONE).build()))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        var command=new CreateBalanceRequest(); command.setUserId(bob);command.setRequestedAmount(BigDecimal.ONE);
        assertThatThrownBy(() -> requests.createRequest(command)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> requests.approveRequest(request)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> requests.pendingRequests()).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> prices.updateInstrumentPrice(instrument,BigDecimal.ONE)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> accounts.createVirtualAccount(alice,BigDecimal.TEN)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> userService.getAllUsers()).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(financialSnapshot()).isEqualTo(before);
    }

    @Test void corsAcceptsOnlyConfiguredOriginAndNeverAuthenticationCookies() throws Exception {
        mvc.perform(options("/transactions/buy").header("Origin","http://localhost:5173").header("Access-Control-Request-Method","POST")
                .header("Access-Control-Request-Headers","authorization,content-type"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:5173"))
            .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        mvc.perform(options("/transactions/buy").header("Origin","https://untrusted.example").header("Access-Control-Request-Method","POST"))
            .andExpect(status().isForbidden());
    }
}
