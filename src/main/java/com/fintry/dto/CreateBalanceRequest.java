package com.fintry.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateBalanceRequest {

    @NotNull(message = "User id is required")
    private Long userId;

    @NotNull(message = "Requested amount is required")
    @Positive(message = "Requested amount must be greater than zero")
    @com.fintry.validation.FinancialPrecision(money = true)
    private BigDecimal requestedAmount;
}
