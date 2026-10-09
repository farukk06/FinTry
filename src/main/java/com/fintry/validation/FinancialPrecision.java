package com.fintry.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FinancialPrecisionValidator.class)
public @interface FinancialPrecision {
    String message() default "Value exceeds supported financial precision";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    boolean money() default false;
}
