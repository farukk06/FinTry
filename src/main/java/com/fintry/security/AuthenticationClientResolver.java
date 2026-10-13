package com.fintry.security;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Exact trusted proxy addresses only. Never resolves DNS names from a request. */
@Component
public class AuthenticationClientResolver {
    private final Set<String> trusted;

    public AuthenticationClientResolver(@Value("${fintry.security.trusted-proxies:}") String proxies) {
        try {
            trusted = Arrays.stream(proxies.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                    .map(AuthenticationClientResolver::address).collect(Collectors.toUnmodifiableSet());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Trusted proxies must be literal IP addresses");
        }
    }

    public String resolve(HttpServletRequest request) {
        String peer = address(request.getRemoteAddr());
        if (!trusted.contains(peer)) return peer; // Ignore all forwarded headers from untrusted peers.
        var headers = request.getHeaders("X-Forwarded-For");
        if (!headers.hasMoreElements()) throw invalidChain();
        String header = headers.nextElement();
        if (headers.hasMoreElements() || header.length() > 2048) throw invalidChain();
        String[] chain = header.split(",", -1);
        if (chain.length > 20) throw invalidChain();
        try {
            // The nearest untrusted hop is authoritative; anything to its left can be spoofed.
            String[] parsed = Arrays.stream(chain).map(String::trim)
                    .map(AuthenticationClientResolver::address).toArray(String[]::new);
            for (int i = parsed.length - 1; i >= 0; i--) {
                if (!trusted.contains(parsed[i])) return parsed[i];
            }
        } catch (IllegalArgumentException ex) { throw invalidChain(); }
        throw invalidChain(); // Do not silently collapse clients onto a shared proxy budget.
    }

    private static String address(String value) {
        if (value == null || value.isEmpty() || value.length() > 45 || !value.matches("[0-9a-fA-F:.]+"))
            throw new IllegalArgumentException("Invalid IP address");
        if (!value.contains(":")) {
            String[] parts = value.split("\\.", -1);
            if (parts.length != 4) throw new IllegalArgumentException("Invalid IP address");
            for (String part : parts) {
                if (!part.matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(part) > 255)
                    throw new IllegalArgumentException("Invalid IP address");
            }
        }
        try { return InetAddress.getByName(value).getHostAddress(); }
        catch (java.net.UnknownHostException ex) { throw new IllegalArgumentException("Invalid IP address"); }
    }

    private static ResponseStatusException invalidChain() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid authentication proxy configuration or request");
    }
}
