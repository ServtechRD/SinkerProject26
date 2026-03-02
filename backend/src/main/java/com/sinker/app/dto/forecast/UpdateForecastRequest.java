package com.sinker.app.dto.forecast;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class UpdateForecastRequest {

    @NotNull(message = "quantity is required")
    @DecimalMin(value = "0", message = "quantity must be >= 0")
    private BigDecimal quantity;

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}
