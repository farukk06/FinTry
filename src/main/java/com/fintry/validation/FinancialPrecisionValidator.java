package com.fintry.validation;

import com.fintry.service.FinancialPolicy;
import com.fintry.exception.InvalidFinancialValueException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class FinancialPrecisionValidator implements ConstraintValidator<FinancialPrecision, BigDecimal> {
    private boolean money;
    @Override public void initialize(FinancialPrecision annotation) { money = annotation.money(); }
    @Override public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) return true; // Null is handled by @NotNull/service validation.
        try { FinancialPolicy.decimal(value, money); return true; }
        catch (InvalidFinancialValueException ex) { return false; }
    }
}
