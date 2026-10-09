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
}
