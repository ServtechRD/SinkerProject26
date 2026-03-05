package com.sinker.app.dto.schedule;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sinker.app.entity.WeeklySchedule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class WeeklyScheduleDTO {

    private Integer id;

    @JsonProperty("weekStart")
    private LocalDate weekStart;

    private String factory;

    @JsonProperty("demandDate")
    private LocalDate demandDate;

    @JsonProperty("productCode")
    private String productCode;

    @JsonProperty("productName")
    private String productName;

    @JsonProperty("warehouseLocation")
    private String warehouseLocation;

    private BigDecimal quantity;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    public WeeklyScheduleDTO() {}

    public static WeeklyScheduleDTO fromEntity(WeeklySchedule entity) {
        WeeklyScheduleDTO dto = new WeeklyScheduleDTO();
        dto.setId(entity.getId());
        dto.setWeekStart(entity.getWeekStart());
        dto.setFactory(entity.getFactory());
        dto.setDemandDate(entity.getDemandDate());
        dto.setProductCode(entity.getProductCode());
        dto.setProductName(entity.getProductName());
        dto.setWarehouseLocation(entity.getWarehouseLocation());
        dto.setQuantity(entity.getQuantity());
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

    public LocalDate getDemandDate() {
        return demandDate;
    }

    public void setDemandDate(LocalDate demandDate) {
        this.demandDate = demandDate;
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

    public String getWarehouseLocation() {
        return warehouseLocation;
    }

    public void setWarehouseLocation(String warehouseLocation) {
        this.warehouseLocation = warehouseLocation;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
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
