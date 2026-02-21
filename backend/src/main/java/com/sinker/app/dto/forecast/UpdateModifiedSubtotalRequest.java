package com.sinker.app.dto.forecast;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public class UpdateModifiedSubtotalRequest {

    @Digits(integer = 10, fraction = 2, message = "modifiedSubtotal must be a valid decimal with max 10 digits and 2 decimal places")
    @DecimalMax(value = "99999999.99", message = "modifiedSubtotal must not exceed 99999999.99")
    private BigDecimal modifiedSubtotal;

    public BigDecimal getModifiedSubtotal() {
        return modifiedSubtotal;
    }

    public void setModifiedSubtotal(BigDecimal modifiedSubtotal) {
        this.modifiedSubtotal = modifiedSubtotal;
    }
}
