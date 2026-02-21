package com.sinker.app.dto.materialpurchase;

import com.sinker.app.entity.MaterialPurchase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class MaterialPurchaseDTO {

    private Integer id;
    private LocalDate weekStart;
    private String factory;
    private String productCode;
    private String productName;
    private BigDecimal quantity;
    private String semiProductName;
    private String semiProductCode;
    private BigDecimal kgPerBox;
    private BigDecimal basketQuantity;
    private BigDecimal boxesPerBarrel;
    private BigDecimal requiredBarrels;
    private Boolean isErpTriggered;
    private String erpOrderNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MaterialPurchaseDTO() {}

    public static MaterialPurchaseDTO fromEntity(MaterialPurchase entity) {
        MaterialPurchaseDTO dto = new MaterialPurchaseDTO();
        dto.setId(entity.getId());
        dto.setWeekStart(entity.getWeekStart());
        dto.setFactory(entity.getFactory());
        dto.setProductCode(entity.getProductCode());
        dto.setProductName(entity.getProductName());
        dto.setQuantity(entity.getQuantity());
        dto.setSemiProductName(entity.getSemiProductName());
        dto.setSemiProductCode(entity.getSemiProductCode());
        dto.setKgPerBox(entity.getKgPerBox());
        dto.setBasketQuantity(entity.getBasketQuantity());
        dto.setBoxesPerBarrel(entity.getBoxesPerBarrel());
        dto.setRequiredBarrels(entity.getRequiredBarrels());
        dto.setIsErpTriggered(entity.getIsErpTriggered());
        dto.setErpOrderNo(entity.getErpOrderNo());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(LocalDate weekStart) {
        this.weekStart = weekStart;
    }

    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getSemiProductName() {
        return semiProductName;
    }

    public void setSemiProductName(String semiProductName) {
        this.semiProductName = semiProductName;
    }

    public String getSemiProductCode() {
        return semiProductCode;
    }

    public void setSemiProductCode(String semiProductCode) {
        this.semiProductCode = semiProductCode;
    }

    public BigDecimal getKgPerBox() {
        return kgPerBox;
    }

    public void setKgPerBox(BigDecimal kgPerBox) {
        this.kgPerBox = kgPerBox;
    }

    public BigDecimal getBasketQuantity() {
        return basketQuantity;
    }

    public void setBasketQuantity(BigDecimal basketQuantity) {
        this.basketQuantity = basketQuantity;
    }

    public BigDecimal getBoxesPerBarrel() {
        return boxesPerBarrel;
    }

    public void setBoxesPerBarrel(BigDecimal boxesPerBarrel) {
        this.boxesPerBarrel = boxesPerBarrel;
    }

    public BigDecimal getRequiredBarrels() {
        return requiredBarrels;
    }

    public void setRequiredBarrels(BigDecimal requiredBarrels) {
        this.requiredBarrels = requiredBarrels;
    }

    public Boolean getIsErpTriggered() {
        return isErpTriggered;
    }

    public void setIsErpTriggered(Boolean isErpTriggered) {
        this.isErpTriggered = isErpTriggered;
    }

    public String getErpOrderNo() {
        return erpOrderNo;
    }

    public void setErpOrderNo(String erpOrderNo) {
        this.erpOrderNo = erpOrderNo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
