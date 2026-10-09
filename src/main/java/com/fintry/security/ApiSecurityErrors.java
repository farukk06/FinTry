package com.fintry.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;

public final class ApiSecurityErrors {
    private ApiSecurityErrors() { }
    public static void write(HttpServletResponse response, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        if (status == HttpStatus.UNAUTHORIZED) response.setHeader("WWW-Authenticate", "Bearer");
        String message = status == HttpStatus.UNAUTHORIZED ? "Authentication required" : "Access denied";
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":"
                + status.value() + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"message\":\"" + message + "\"}");
    }
}
