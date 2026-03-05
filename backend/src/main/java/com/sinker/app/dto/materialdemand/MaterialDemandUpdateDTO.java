package com.sinker.app.dto.materialdemand;

import java.math.BigDecimal;

public class MaterialDemandUpdateDTO {

    private BigDecimal expectedDelivery;
    private BigDecimal demandQuantity;
    private BigDecimal estimatedInventory;

    public BigDecimal getExpectedDelivery() {
        return expectedDelivery;
    }

    public void setExpectedDelivery(BigDecimal expectedDelivery) {
        this.expectedDelivery = expectedDelivery;
    }

    public BigDecimal getDemandQuantity() {
        return demandQuantity;
    }

    public void setDemandQuantity(BigDecimal demandQuantity) {
        this.demandQuantity = demandQuantity;
    }

    public BigDecimal getEstimatedInventory() {
        return estimatedInventory;
    }

    public void setEstimatedInventory(BigDecimal estimatedInventory) {
        this.estimatedInventory = estimatedInventory;
    }
}
