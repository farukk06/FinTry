package com.fintry.dto;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import com.fintry.validation.FinancialPrecision;
public record BalanceCommand(@NotNull @Positive @FinancialPrecision(money=true) BigDecimal requestedAmount) { }
