package com.sinker.app.dto.materialpurchase;

import java.math.BigDecimal;

public class MaterialPurchaseUpdateDTO {

    private BigDecimal kgPerBox;
    private BigDecimal basketQuantity;
    private BigDecimal boxesPerBarrel;
    private BigDecimal requiredBarrels;

    public BigDecimal getKgPerBox() { return kgPerBox; }
    public void setKgPerBox(BigDecimal kgPerBox) { this.kgPerBox = kgPerBox; }

    public BigDecimal getBasketQuantity() { return basketQuantity; }
    public void setBasketQuantity(BigDecimal basketQuantity) { this.basketQuantity = basketQuantity; }

    public BigDecimal getBoxesPerBarrel() { return boxesPerBarrel; }
    public void setBoxesPerBarrel(BigDecimal boxesPerBarrel) { this.boxesPerBarrel = boxesPerBarrel; }

    public BigDecimal getRequiredBarrels() { return requiredBarrels; }
    public void setRequiredBarrels(BigDecimal requiredBarrels) { this.requiredBarrels = requiredBarrels; }
}
