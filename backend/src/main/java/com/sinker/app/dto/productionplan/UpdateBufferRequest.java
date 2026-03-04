package com.sinker.app.dto.productionplan;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class UpdateBufferRequest {

    @NotNull(message = "year is required")
    private Integer year;

    @NotNull(message = "productCode is required")
    private String productCode;

    @NotNull(message = "bufferQuantity is required")
    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal bufferQuantity;

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public BigDecimal getBufferQuantity() { return bufferQuantity; }
    public void setBufferQuantity(BigDecimal bufferQuantity) { this.bufferQuantity = bufferQuantity; }
}
