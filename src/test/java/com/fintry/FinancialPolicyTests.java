package com.fintry;

import com.fintry.service.FinancialPolicy;
import com.fintry.exception.InvalidFinancialValueException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class FinancialPolicyTests {
    @Test void preservesProductAndTrailingZeros() {
        assertThat(FinancialPolicy.total(new BigDecimal("0.00000001"), new BigDecimal("0.00000001")))
                .isEqualByComparingTo("0.0000000000000001");
        assertThat(FinancialPolicy.quantity(new BigDecimal("1.000000000"))).isEqualByComparingTo("1");
    }
    @Test void rejectsInvalidValuesWithoutRounding() {
        for (String value : new String[]{"-1", "0", "0.000000001", "1E+36", "1E+1000000", "1E-1000000"}) {
            assertThatThrownBy(() -> FinancialPolicy.price(new BigDecimal(value)))
                    .isInstanceOf(InvalidFinancialValueException.class);
        }
        assertThatThrownBy(() -> FinancialPolicy.price(null)).isInstanceOf(InvalidFinancialValueException.class);
        assertThatThrownBy(() -> FinancialPolicy.balance(new BigDecimal("-1"))).isInstanceOf(InvalidFinancialValueException.class);
        assertThat(FinancialPolicy.balance(BigDecimal.ZERO)).isZero();
    }
    @Test void preservesOldIntegerCapacityAndRejectsAccumulationOverflow() {
        BigDecimal max = new BigDecimal("999999999999999999999999999999999999.9999999999999999");
        assertThat(FinancialPolicy.balance(max)).isEqualByComparingTo(max);
        assertThatThrownBy(() -> FinancialPolicy.balance(max.add(BigDecimal.ONE)))
                .isInstanceOf(InvalidFinancialValueException.class);
    }
    @Test void averageUsesHalfEvenAtEightPlaces() {
        assertThat(FinancialPolicy.average(new BigDecimal("2.00000001"), new BigDecimal("2")))
                .isEqualByComparingTo("1.00000000");
        assertThat(FinancialPolicy.average(new BigDecimal("2.00000003"), new BigDecimal("2")))
                .isEqualByComparingTo("1.00000002");
    }
}
