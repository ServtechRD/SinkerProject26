package com.sinker.app.dto.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sinker.app.entity.SalesForecast;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ForecastResponse {

    private Integer id;
    private String month;
    private String channel;
    private String category;
    private String spec;
    @JsonProperty("product_code")
    private String productCode;
    @JsonProperty("product_name")
    private String productName;
    @JsonProperty("warehouse_location")
    private String warehouseLocation;
    private BigDecimal quantity;
    private String version;
    @JsonProperty("is_modified")
    private Boolean isModified;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public static ForecastResponse fromEntity(SalesForecast entity) {
        ForecastResponse response = new ForecastResponse();
        response.setId(entity.getId());
        response.setMonth(entity.getMonth());
        response.setChannel(entity.getChannel());
        response.setCategory(entity.getCategory());
        response.setSpec(entity.getSpec());
        response.setProductCode(entity.getProductCode());
        response.setProductName(entity.getProductName());
        response.setWarehouseLocation(entity.getWarehouseLocation());
        response.setQuantity(entity.getQuantity());
        response.setVersion(entity.getVersion());
        response.setIsModified(entity.getIsModified());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getWarehouseLocation() { return warehouseLocation; }
    public void setWarehouseLocation(String warehouseLocation) { this.warehouseLocation = warehouseLocation; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public Boolean getIsModified() { return isModified; }
    public void setIsModified(Boolean isModified) { this.isModified = isModified; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
