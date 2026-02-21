package com.sinker.app.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_sales_forecast")
public class InventorySalesForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 7)
    private String month;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 200)
    private String spec;

    @Column(name = "warehouse_location", nullable = false, length = 50)
    private String warehouseLocation;

    @Column(name = "sales_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal salesQuantity = BigDecimal.ZERO;

    @Column(name = "inventory_balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal inventoryBalance = BigDecimal.ZERO;

    @Column(name = "forecast_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal forecastQuantity = BigDecimal.ZERO;

    @Column(name = "production_subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal productionSubtotal = BigDecimal.ZERO;

    @Column(name = "modified_subtotal", precision = 10, scale = 2)
    private BigDecimal modifiedSubtotal;

    @Column(nullable = false, length = 100)
    private String version;

    @Column(name = "query_start_date", nullable = false, length = 10)
    private String queryStartDate;

    @Column(name = "query_end_date", nullable = false, length = 10)
    private String queryEndDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }

    public String getWarehouseLocation() { return warehouseLocation; }
    public void setWarehouseLocation(String warehouseLocation) { this.warehouseLocation = warehouseLocation; }

    public BigDecimal getSalesQuantity() { return salesQuantity; }
    public void setSalesQuantity(BigDecimal salesQuantity) { this.salesQuantity = salesQuantity; }

    public BigDecimal getInventoryBalance() { return inventoryBalance; }
    public void setInventoryBalance(BigDecimal inventoryBalance) { this.inventoryBalance = inventoryBalance; }

    public BigDecimal getForecastQuantity() { return forecastQuantity; }
    public void setForecastQuantity(BigDecimal forecastQuantity) { this.forecastQuantity = forecastQuantity; }

    public BigDecimal getProductionSubtotal() { return productionSubtotal; }
    public void setProductionSubtotal(BigDecimal productionSubtotal) { this.productionSubtotal = productionSubtotal; }

    public BigDecimal getModifiedSubtotal() { return modifiedSubtotal; }
    public void setModifiedSubtotal(BigDecimal modifiedSubtotal) { this.modifiedSubtotal = modifiedSubtotal; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getQueryStartDate() { return queryStartDate; }
    public void setQueryStartDate(String queryStartDate) { this.queryStartDate = queryStartDate; }

    public String getQueryEndDate() { return queryEndDate; }
    public void setQueryEndDate(String queryEndDate) { this.queryEndDate = queryEndDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
