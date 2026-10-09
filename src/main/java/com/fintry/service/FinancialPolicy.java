package com.fintry.service;

import com.fintry.exception.InvalidFinancialValueException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Matches numeric(44,8) and numeric(52,16); inputs are never silently rounded. */
public final class FinancialPolicy {
    public static final int VALUE_PRECISION = 44;
    public static final int VALUE_SCALE = 8;
    public static final int MONEY_PRECISION = 52;
    public static final int MONEY_SCALE = 16;
    private FinancialPolicy() { }

    public static BigDecimal price(BigDecimal value) {
        return positive(value, VALUE_PRECISION, VALUE_SCALE, "Price");
    }

    public static BigDecimal quantity(BigDecimal value) {
        return positive(value, VALUE_PRECISION, VALUE_SCALE, "Quantity");
    }

    public static BigDecimal amount(BigDecimal value) {
        return positive(value, MONEY_PRECISION, MONEY_SCALE, "Amount");
    }

    public static BigDecimal balance(BigDecimal value) {
        BigDecimal result = exact(value, MONEY_PRECISION, MONEY_SCALE, "Balance");
        if (result.signum() < 0) throw invalid("Balance must not be negative");
        return result;
    }

    public static BigDecimal total(BigDecimal price, BigDecimal quantity) {
        return amount(price(price).multiply(quantity(quantity)));
    }

    public static BigDecimal average(BigDecimal cost, BigDecimal quantity) {
        return price(cost.divide(quantity(quantity), VALUE_SCALE, RoundingMode.HALF_EVEN));
    }

    public static BigDecimal decimal(BigDecimal value, boolean money) {
        return exact(value, money ? MONEY_PRECISION : VALUE_PRECISION,
                money ? MONEY_SCALE : VALUE_SCALE, "Value");
    }

    private static BigDecimal positive(BigDecimal value, int precision, int scale, String field) {
        BigDecimal result = exact(value, precision, scale, field);
        if (result.signum() <= 0) throw invalid(field + " must be greater than zero");
        return result;
    }

    private static BigDecimal exact(BigDecimal value, int precision, int scale, String field) {
        if (value == null) throw invalid(field + " is required");
        // Bound the exponent before setScale, avoiding allocations for inputs such as 1E+1000000.
        long integerDigits = (long) value.precision() - value.scale();
        if (integerDigits > precision - scale) throw invalid(field + " exceeds numeric range");
        if (value.stripTrailingZeros().scale() > scale) {
            throw invalid(field + " supports at most " + scale + " decimal places");
        }
        try {
            BigDecimal result = value.setScale(scale, RoundingMode.UNNECESSARY);
            if (result.precision() > precision) throw invalid(field + " exceeds numeric range");
            return result;
        } catch (ArithmeticException ex) {
            throw invalid(field + " supports at most " + scale + " decimal places");
        }
    }

    private static InvalidFinancialValueException invalid(String message) {
        return new InvalidFinancialValueException(message);
    }
}
