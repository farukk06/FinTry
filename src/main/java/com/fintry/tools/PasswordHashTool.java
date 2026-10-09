package com.fintry.tools;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Offline, console-only provisioning helper. Does not start Spring or open a database connection. */
public final class PasswordHashTool {
    private PasswordHashTool() { }
    public static void main(String[] args) {
        var console=System.console();
        if (args.length != 0 || console == null) throw new IllegalStateException("Use an interactive console without arguments");
        char[] password=console.readPassword("New password: ");
        char[] confirmation=console.readPassword("Confirm password: ");
        try {
            if (password == null || confirmation == null || !Arrays.equals(password, confirmation))
                throw new IllegalArgumentException("Passwords must match");
            String value=new String(password);
            if (value.isBlank() || value.length()<12 || value.getBytes(StandardCharsets.UTF_8).length>72)
                throw new IllegalArgumentException("Use at least 12 characters and at most 72 UTF-8 bytes");
            console.printf("BCrypt hash (store securely): %s%n", new BCryptPasswordEncoder(12).encode(value));
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (confirmation != null) Arrays.fill(confirmation, '\0');
        }
    }
}
