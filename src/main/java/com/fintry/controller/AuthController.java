package com.fintry.controller;

import com.fintry.dto.*;
import com.fintry.service.AuthService;
import com.fintry.security.*;
import com.fintry.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;
    private final AccountAccess access;
    private final UserRepository users;
    private final LoginRateLimiter limiter;
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        limiter.check(http.getRemoteAddr());
        return auth.register(request);
    }
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        limiter.check(http.getRemoteAddr());
        return auth.login(request);
    }
    @GetMapping("/me")
    public UserResponse me() { return AuthService.summary(users.findById(access.userId()).orElseThrow()); }
}
