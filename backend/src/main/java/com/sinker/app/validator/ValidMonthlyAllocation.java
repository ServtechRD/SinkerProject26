package com.sinker.app.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MonthlyAllocationValidator.class)
@Documented
public @interface ValidMonthlyAllocation {
    String message() default "Invalid monthly allocation";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
