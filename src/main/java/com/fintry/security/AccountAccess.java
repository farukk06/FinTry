package com.fintry.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

@Component("accountAccess")
public class AccountAccess {
    public Long userId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AuthenticationCredentialsNotFoundException("Authentication required");
        try { return Long.valueOf(auth.getName()); }
        catch (NumberFormatException ex) { throw new AuthenticationCredentialsNotFoundException("Invalid identity"); }
    }
    public boolean owns(Long id) {
        return id != null && id.equals(userId());
    }
}
