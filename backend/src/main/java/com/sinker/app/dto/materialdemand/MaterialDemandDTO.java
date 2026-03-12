package com.sinker.app.dto.materialdemand;

import com.sinker.app.entity.MaterialDemand;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class MaterialDemandDTO {

    private Integer id;
    private LocalDate weekStart;
    private String factory;
    private String materialCode;
    private String materialName;
    private String unit;
    private LocalDate lastPurchaseDate;
    private LocalDate demandDate;
    private BigDecimal currentStock;
    private LocalDate expectedArrivalDate;
    private BigDecimal expectedDelivery;
    private BigDecimal demandQuantity;
    private BigDecimal estimatedInventory;
    private BigDecimal purchaseQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MaterialDemandDTO() {}

    public static MaterialDemandDTO fromEntity(MaterialDemand entity) {
        MaterialDemandDTO dto = new MaterialDemandDTO();
        dto.setId(entity.getId());
        dto.setWeekStart(entity.getWeekStart());
        dto.setFactory(entity.getFactory());
        dto.setMaterialCode(entity.getMaterialCode());
        dto.setMaterialName(entity.getMaterialName());
        dto.setUnit(entity.getUnit());
        dto.setLastPurchaseDate(entity.getLastPurchaseDate());
        dto.setDemandDate(entity.getDemandDate());
        dto.setCurrentStock(entity.getCurrentStock());
        dto.setExpectedArrivalDate(entity.getExpectedArrivalDate());
        dto.setExpectedDelivery(entity.getExpectedDelivery());
        dto.setDemandQuantity(entity.getDemandQuantity());
        // 預計庫存量 = 現有庫存 + 預交量 - 需求量
        if (entity.getCurrentStock() != null) {
            BigDecimal computed = entity.getCurrentStock()
                    .add(entity.getExpectedDelivery() != null ? entity.getExpectedDelivery() : BigDecimal.ZERO)
                    .subtract(entity.getDemandQuantity() != null ? entity.getDemandQuantity() : BigDecimal.ZERO);
            dto.setEstimatedInventory(computed);
        } else {
            dto.setEstimatedInventory(entity.getEstimatedInventory());
        }
        dto.setPurchaseQuantity(entity.getPurchaseQuantity());
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

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDate getLastPurchaseDate() {
        return lastPurchaseDate;
    }

    public void setLastPurchaseDate(LocalDate lastPurchaseDate) {
        this.lastPurchaseDate = lastPurchaseDate;
    }

    public LocalDate getDemandDate() {
        return demandDate;
    }

    public void setDemandDate(LocalDate demandDate) {
        this.demandDate = demandDate;
    }

    public BigDecimal getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(BigDecimal currentStock) {
        this.currentStock = currentStock;
    }

    public LocalDate getExpectedArrivalDate() {
        return expectedArrivalDate;
    }

    public void setExpectedArrivalDate(LocalDate expectedArrivalDate) {
        this.expectedArrivalDate = expectedArrivalDate;
    }

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

    public BigDecimal getPurchaseQuantity() {
        return purchaseQuantity;
    }

    public void setPurchaseQuantity(BigDecimal purchaseQuantity) {
        this.purchaseQuantity = purchaseQuantity;
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
