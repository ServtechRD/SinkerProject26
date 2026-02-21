package com.sinker.app.dto.productionplan;

import com.sinker.app.validator.ValidMonthlyAllocation;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public class UpdateProductionPlanRequest {

    @NotNull(message = "monthlyAllocation is required")
    @ValidMonthlyAllocation
    private Map<String, BigDecimal> monthlyAllocation;

    @NotNull(message = "bufferQuantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "bufferQuantity must be >= 0")
    @Digits(integer = 8, fraction = 2, message = "bufferQuantity must have at most 8 integer digits and 2 decimal places")
    private BigDecimal bufferQuantity;

    @Size(max = 65535, message = "remarks must not exceed 65535 characters")
    private String remarks;

    public Map<String, BigDecimal> getMonthlyAllocation() {
        return monthlyAllocation;
    }

    public void setMonthlyAllocation(Map<String, BigDecimal> monthlyAllocation) {
        this.monthlyAllocation = monthlyAllocation;
    }

    public BigDecimal getBufferQuantity() {
        return bufferQuantity;
    }

    public void setBufferQuantity(BigDecimal bufferQuantity) {
        this.bufferQuantity = bufferQuantity;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
