package com.fintry;

import com.fintry.security.AuthenticationClientResolver;
import com.fintry.security.LoginRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class AuthenticationClientResolverTests {
    private MockHttpServletRequest request(String peer, String forwarded) {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr(peer);
        if (forwarded != null) request.addHeader("X-Forwarded-For", forwarded);
        return request;
    }
    @Test void untrustedPeerCannotRotateBudgetUsingForwardedHeaders() {
        var resolver = new AuthenticationClientResolver("");
        var limiter = new LoginRateLimiter();
        for (int i=0; i<10; i++) limiter.checkLogin(resolver.resolve(request("192.0.2.1", "198.51.100."+i)));
        assertThatThrownBy(() -> limiter.checkLogin(resolver.resolve(request("192.0.2.1", "198.51.100.99"))))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(resolver.resolve(request("192.0.2.1", "malformed"))).isEqualTo("192.0.2.1");
    }
    @Test void clientsBehindTrustedProxyHaveIndependentLoginBudgets() {
        var resolver = new AuthenticationClientResolver("10.0.0.10");
        var limiter = new LoginRateLimiter();
        for (int i=0; i<10; i++) limiter.checkLogin(resolver.resolve(request("10.0.0.10", "192.0.2.1")));
        assertThatCode(() -> limiter.checkLogin(resolver.resolve(request("10.0.0.10", "192.0.2.2"))))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.checkLogin(resolver.resolve(request("10.0.0.10", "192.0.2.1"))))
                .isInstanceOf(ResponseStatusException.class);
    }
    @Test void rightmostUntrustedHopWinsOverAttackerSuppliedPrefix() {
        var resolver = new AuthenticationClientResolver("10.0.0.10,10.0.0.11");
        assertThat(resolver.resolve(request("10.0.0.10", "198.51.100.77, 192.0.2.1, 10.0.0.11")))
                .isEqualTo("192.0.2.1");
    }
    @Test void trustedPeerWithAbsentMalformedOrAmbiguousChainFailsClosed() {
        var resolver = new AuthenticationClientResolver("10.0.0.10");
        for (String value : new String[]{null, "", "example.com", "192.0.2.1,", "unknown", "10.0.0.10",
                "999.0.0.1", "192.0.2.1:123", "192.0.2.01", "192.0.2.1,".repeat(21), "a".repeat(2049)}) {
            assertThatThrownBy(() -> resolver.resolve(request("10.0.0.10", value)))
                    .isInstanceOf(ResponseStatusException.class);
        }
        var duplicate = request("10.0.0.10", "192.0.2.1");
        duplicate.addHeader("X-Forwarded-For", "192.0.2.2");
        assertThatThrownBy(() -> resolver.resolve(duplicate)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void literalIpv6ProxyAddressesAreCanonicalized() {
        var resolver = new AuthenticationClientResolver("::1");
        assertThat(resolver.resolve(request("0:0:0:0:0:0:0:1", "2001:db8::1")))
                .isEqualTo("2001:db8:0:0:0:0:0:1");
    }
    @Test void configurationRejectsHostnamesAndWildcardTrust() {
        for (String value : new String[]{"localhost", "*", "0.0.0.0/0", "10.0.0.1,example.com"})
            assertThatThrownBy(() -> new AuthenticationClientResolver(value)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void forwardedHeaderIsNotAnAlternateTrustChannel() {
        var request = request("10.0.0.10", null);
        request.addHeader("Forwarded", "for=192.0.2.1");
        assertThatThrownBy(() -> new AuthenticationClientResolver("10.0.0.10").resolve(request))
                .isInstanceOf(ResponseStatusException.class);
    }
}
