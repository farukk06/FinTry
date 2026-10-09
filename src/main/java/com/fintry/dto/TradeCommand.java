package com.fintry.dto;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import com.fintry.validation.FinancialPrecision;
public record TradeCommand(@NotNull @Positive Long instrumentId,
        @NotNull @Positive @FinancialPrecision BigDecimal quantity) { }
