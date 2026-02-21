package com.sinker.app.dto.forecast;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventoryIntegrationDTO {

    private Integer id;
    private String month;
    private String productCode;
    private String productName;
    private String category;
    private String spec;
    private String warehouseLocation;
    private BigDecimal salesQuantity;
    private BigDecimal inventoryBalance;
    private BigDecimal forecastQuantity;
    private BigDecimal productionSubtotal;
    private BigDecimal modifiedSubtotal;
    private String version;
    private String queryStartDate;
    private String queryEndDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InventoryIntegrationDTO() {
        this.salesQuantity = BigDecimal.ZERO;
        this.inventoryBalance = BigDecimal.ZERO;
        this.forecastQuantity = BigDecimal.ZERO;
        this.productionSubtotal = BigDecimal.ZERO;
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
