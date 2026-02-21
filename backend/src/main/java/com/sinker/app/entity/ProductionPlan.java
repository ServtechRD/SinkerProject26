package com.sinker.app.entity;

import com.sinker.app.converter.MonthlyAllocationConverter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "production_plan")
public class ProductionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer year;

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

    @Column(nullable = false, length = 50)
    private String channel;

    @Convert(converter = MonthlyAllocationConverter.class)
    @Column(name = "monthly_allocation", nullable = false, columnDefinition = "JSON")
    private Map<String, BigDecimal> monthlyAllocation = new HashMap<>();

    @Column(name = "buffer_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal bufferQuantity = BigDecimal.ZERO;

    @Column(name = "total_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalQuantity = BigDecimal.ZERO;

    @Column(name = "original_forecast", nullable = false, precision = 10, scale = 2)
    private BigDecimal originalForecast = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal difference = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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
        this.monthlyAllocation = monthlyAllocation != null ? monthlyAllocation : new HashMap<>();
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
