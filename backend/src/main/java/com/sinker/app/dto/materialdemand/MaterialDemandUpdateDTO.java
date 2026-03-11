package com.sinker.app.dto.materialdemand;

import java.math.BigDecimal;

public class MaterialDemandUpdateDTO {

    private BigDecimal purchaseQuantity;

    public BigDecimal getPurchaseQuantity() {
        return purchaseQuantity;
    }

    public void setPurchaseQuantity(BigDecimal purchaseQuantity) {
        this.purchaseQuantity = purchaseQuantity;
    }
}
