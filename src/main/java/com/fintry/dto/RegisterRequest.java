package com.fintry.dto;

import jakarta.validation.constraints.*;
public record RegisterRequest(
        @NotBlank @Size(min=3, max=64) @Pattern(regexp="[A-Za-z0-9_.-]+") String username,
        @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(min=12, max=72) String password) { }
