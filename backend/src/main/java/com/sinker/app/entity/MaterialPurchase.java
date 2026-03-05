package com.sinker.app.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "material_purchase")
public class MaterialPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(nullable = false, length = 50)
    private String factory;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "semi_product_name", nullable = false, length = 200)
    private String semiProductName;

    @Column(name = "semi_product_code", nullable = false, length = 100)
    private String semiProductCode;

    @Column(name = "kg_per_box", nullable = false, precision = 10, scale = 2)
    private BigDecimal kgPerBox;

    @Column(name = "basket_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal basketQuantity;

    @Column(name = "boxes_per_barrel", nullable = false, precision = 10, scale = 2)
    private BigDecimal boxesPerBarrel;

    @Column(name = "required_barrels", nullable = false, precision = 10, scale = 2)
    private BigDecimal requiredBarrels;

    @Column(name = "is_erp_triggered", nullable = false)
    private Boolean isErpTriggered;

    @Column(name = "erp_order_no", length = 100)
    private String erpOrderNo;

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
