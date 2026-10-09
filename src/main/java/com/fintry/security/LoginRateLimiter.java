package com.fintry.security;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Bounded per-client login budget. Use a trusted gateway/shared limiter when deploying multiple nodes. */
@Component
public class LoginRateLimiter {
    private final Clock clock;
    private final Map<String, Budget> clients = new HashMap<>();
    private record Budget(long window, int count) { }
    public LoginRateLimiter() { this(Clock.systemUTC()); }
    public LoginRateLimiter(Clock clock) { this.clock = clock; }
    public synchronized void check(String client) {
        long now = clock.millis();
        clients.entrySet().removeIf(e -> now - e.getValue().window() >= 60_000);
        var budget = clients.get(client);
        if ((budget != null && budget.count() >= 10) || (budget == null && clients.size() >= 10_000))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many authentication attempts");
        clients.put(client, new Budget(budget == null ? now : budget.window(), budget == null ? 1 : budget.count()+1));
    }
}
