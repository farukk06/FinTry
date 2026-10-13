package com.fintry.security;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Single-node budgets, including an aggregate registration budget against address rotation. */
@Component
public class LoginRateLimiter {
    private final Clock clock;
    private final Map<String, Budget> clients = new HashMap<>();
    private Budget registrations;
    private record Budget(long window, int count) { }
    public LoginRateLimiter() { this(Clock.systemUTC()); }
    public LoginRateLimiter(Clock clock) { this.clock = clock; }
    public void check(String client) { checkLogin(client); }
    public synchronized void checkLogin(String client) { check("login:" + client, 10, false); }
    public synchronized void checkRegistration(String client) { check("register:" + client, 5, true); }
    private void check(String client, int limit, boolean registration) {
        long now = clock.millis();
        clients.entrySet().removeIf(e -> now - e.getValue().window() >= 60_000);
        if (registrations != null && now - registrations.window() >= 60_000) registrations = null;
        var budget = clients.get(client);
        if ((budget != null && budget.count() >= limit) || (budget == null && clients.size() >= 10_000)
                || (registration && registrations != null && registrations.count() >= 30))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many authentication attempts");
        clients.put(client, new Budget(budget == null ? now : budget.window(), budget == null ? 1 : budget.count()+1));
        if (registration) registrations = new Budget(registrations == null ? now : registrations.window(),
                registrations == null ? 1 : registrations.count()+1);
    }
}
