package com.sinker.app.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public class MonthlyAllocationValidator implements ConstraintValidator<ValidMonthlyAllocation, Map<String, BigDecimal>> {

    private static final Set<String> VALID_MONTHS = Set.of("2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
    private static final BigDecimal MAX_VALUE = new BigDecimal("9999999999.99");

    @Override
    public void initialize(ValidMonthlyAllocation constraintAnnotation) {
    }

    @Override
    public boolean isValid(Map<String, BigDecimal> value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // @NotNull handles null check
        }

        for (Map.Entry<String, BigDecimal> entry : value.entrySet()) {
            String month = entry.getKey();
            BigDecimal amount = entry.getValue();

            // Validate month key
            if (!VALID_MONTHS.contains(month)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "monthlyAllocation contains invalid month key: " + month + ". Valid keys are 2-12"
                ).addConstraintViolation();
                return false;
            }

            // Validate amount
            if (amount == null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "monthlyAllocation value for month " + month + " cannot be null"
                ).addConstraintViolation();
                return false;
            }

            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "monthlyAllocation value for month " + month + " must be >= 0"
                ).addConstraintViolation();
                return false;
            }

            if (amount.compareTo(MAX_VALUE) > 0) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "monthlyAllocation value for month " + month + " exceeds maximum of 9999999999.99"
                ).addConstraintViolation();
                return false;
            }

            // Validate decimal scale
            if (amount.scale() > 2) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "monthlyAllocation value for month " + month + " must have at most 2 decimal places"
                ).addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}
