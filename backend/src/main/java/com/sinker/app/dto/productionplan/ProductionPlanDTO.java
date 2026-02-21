package com.sinker.app.dto.productionplan;

import com.sinker.app.entity.ProductionPlan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class ProductionPlanDTO {

    private Integer id;
    private Integer year;
    private String productCode;
    private String productName;
    private String category;
    private String spec;
    private String warehouseLocation;
    private String channel;
    private Map<String, BigDecimal> monthlyAllocation;
    private BigDecimal bufferQuantity;
    private BigDecimal totalQuantity;
    private BigDecimal originalForecast;
    private BigDecimal difference;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductionPlanDTO() {}

    public static ProductionPlanDTO fromEntity(ProductionPlan entity) {
        ProductionPlanDTO dto = new ProductionPlanDTO();
        dto.setId(entity.getId());
        dto.setYear(entity.getYear());
        dto.setProductCode(entity.getProductCode());
        dto.setProductName(entity.getProductName());
        dto.setCategory(entity.getCategory());
        dto.setSpec(entity.getSpec());
        dto.setWarehouseLocation(entity.getWarehouseLocation());
        dto.setChannel(entity.getChannel());
        dto.setMonthlyAllocation(entity.getMonthlyAllocation());
        dto.setBufferQuantity(entity.getBufferQuantity());
        dto.setTotalQuantity(entity.getTotalQuantity());
        dto.setOriginalForecast(entity.getOriginalForecast());
        dto.setDifference(entity.getDifference());
        dto.setRemarks(entity.getRemarks());
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

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getWarehouseLocation() {
        return warehouseLocation;
    }

    public void setWarehouseLocation(String warehouseLocation) {
        this.warehouseLocation = warehouseLocation;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Map<String, BigDecimal> getMonthlyAllocation() {
        return monthlyAllocation;
    }

    public void setMonthlyAllocation(Map<String, BigDecimal> monthlyAllocation) {
        this.monthlyAllocation = monthlyAllocation;
    }

    public BigDecimal getBufferQuantity() {
        return bufferQuantity;
    }

    public void setBufferQuantity(BigDecimal bufferQuantity) {
        this.bufferQuantity = bufferQuantity;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getOriginalForecast() {
        return originalForecast;
    }

    public void setOriginalForecast(BigDecimal originalForecast) {
        this.originalForecast = originalForecast;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public void setDifference(BigDecimal difference) {
        this.difference = difference;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
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
