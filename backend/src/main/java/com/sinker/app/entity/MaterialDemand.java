package com.sinker.app.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "material_demand")
public class MaterialDemand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(nullable = false, length = 50)
    private String factory;

    @Column(name = "material_code", nullable = false, length = 50)
    private String materialCode;

    @Column(name = "material_name", nullable = false, length = 200)
    private String materialName;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "last_purchase_date")
    private LocalDate lastPurchaseDate;

    @Column(name = "demand_date", nullable = false)
    private LocalDate demandDate;

    @Column(name = "expected_delivery", nullable = false, precision = 10, scale = 2)
    private BigDecimal expectedDelivery;

    @Column(name = "demand_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal demandQuantity;

    @Column(name = "estimated_inventory", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedInventory;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
