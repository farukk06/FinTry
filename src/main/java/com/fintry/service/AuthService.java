package com.fintry.service;

import com.fintry.dto.*;
import com.fintry.entity.*;
import com.fintry.repository.*;
import com.fintry.security.TokenService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.dao.DataIntegrityViolationException;
import com.fintry.exception.BusinessRuleException;
import com.fintry.exception.InvalidFinancialValueException;

@Service
@Validated
public class AuthService {
    private final UserRepository users;
    private final VirtualAccountRepository accounts;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final String dummyHash;
    public AuthService(UserRepository users, VirtualAccountRepository accounts, PasswordEncoder passwords, TokenService tokens) {
        this.users=users; this.accounts=accounts; this.passwords=passwords; this.tokens=tokens;
        this.dummyHash=passwords.encode(java.util.UUID.randomUUID().toString());
    }
    @Transactional
    public UserResponse register(@Valid RegisterRequest request) {
        checkPasswordBytes(request.password());
        final User user;
        try {
            user = users.saveAndFlush(User.builder().username(request.username()).email(normalize(request.email()))
                    .role(Role.USER).passwordHash(passwords.encode(request.password())).enabled(true).build());
        } catch (DataIntegrityViolationException ex) {
            // Legacy baseline schemas may use different unique constraint names.
            for (Throwable cause=ex; cause!=null; cause=cause.getCause()) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException violation &&
                        "23505".equals(violation.getSQLState())) {
                    throw new BusinessRuleException("Registration could not be completed; identity may already exist");
                }
            }
            throw ex;
        }
        accounts.saveAndFlush(VirtualAccount.builder().user(user).balance(FinancialPolicy.balance(BigDecimal.ZERO)).build());
        return summary(user);
    }
    @Transactional(readOnly=true)
    public AuthResponse login(@Valid LoginRequest request) {
        checkPasswordBytes(request.password());
        var user = users.findByEmailIgnoreCase(normalize(request.email())).orElse(null);
        String hash = user == null || user.getPasswordHash() == null ? dummyHash : user.getPasswordHash();
        boolean matches = passwords.matches(request.password(), hash);
        if (!matches || user == null || !user.isEnabled() || user.getPasswordHash() == null)
            throw new BadCredentialsException("Invalid email or password");
        return new AuthResponse(tokens.issue(user.getId()), "Bearer", tokens.expiresIn(), summary(user));
    }
    public static UserResponse summary(User user) {
        return UserResponse.builder().id(user.getId()).username(user.getUsername()).email(user.getEmail()).role(user.getRole()).build();
    }
    private static String normalize(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    private static void checkPasswordBytes(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new InvalidFinancialValueException("Password must not exceed 72 UTF-8 bytes");
    }
}
