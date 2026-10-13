package com.fintry;
import com.fintry.security.LoginRateLimiter;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class LoginRateLimiterTests {
    @Test void limitsOneClientWithoutBlockingOtherClientsAndExpires() {
        class MutableClock extends Clock {
            Instant now=Instant.EPOCH;
            public ZoneId getZone(){return ZoneOffset.UTC;}
            public Clock withZone(ZoneId zone){return this;}
            public Instant instant(){return now;}
        }
        var clock=new MutableClock(); var limiter=new LoginRateLimiter(clock);
        for(int i=0;i<10;i++) limiter.check("client");
        assertThatThrownBy(() -> limiter.check("client")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatCode(() -> limiter.check("other")).doesNotThrowAnyException();
        clock.now=clock.now.plusSeconds(60);
        assertThatCode(() -> limiter.check("client")).doesNotThrowAnyException();
    }
    @Test void registrationsHaveSeparateStricterBudgetWithoutConsumingLoginBudget() {
        var limiter = new LoginRateLimiter();
        for (int i=0; i<5; i++) limiter.checkRegistration("client");
        assertThatThrownBy(() -> limiter.checkRegistration("client"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        for (int i=0; i<10; i++) limiter.checkLogin("client");
        assertThatCode(() -> limiter.checkRegistration("other")).doesNotThrowAnyException();
    }
    @Test void registrationAddressRotationIsBoundedAndAggregateBudgetExpires() {
        var clock = new Clock() {
            Instant now=Instant.EPOCH;
            public ZoneId getZone(){return ZoneOffset.UTC;}
            public Clock withZone(ZoneId zone){return this;}
            public Instant instant(){return now;}
        };
        var limiter = new LoginRateLimiter(clock);
        for (int i=0; i<30; i++) limiter.checkRegistration("client-"+i);
        assertThatThrownBy(() -> limiter.checkRegistration("new-client"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatCode(() -> limiter.checkLogin("new-client")).doesNotThrowAnyException();
        clock.now=clock.now.plusSeconds(60);
        assertThatCode(() -> limiter.checkRegistration("new-client")).doesNotThrowAnyException();
    }
    @Test void clientBudgetStorageIsBounded() {
        var limiter = new LoginRateLimiter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        for (int i=0; i<10_000; i++) limiter.checkLogin("client-"+i);
        assertThatThrownBy(() -> limiter.checkLogin("overflow"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatCode(() -> limiter.checkLogin("client-0")).doesNotThrowAnyException();
    }
}
