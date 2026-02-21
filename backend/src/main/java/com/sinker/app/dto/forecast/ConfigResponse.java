package com.sinker.app.dto.forecast;

import com.sinker.app.entity.SalesForecastConfig;

import java.time.LocalDateTime;

public class ConfigResponse {

    private Integer id;
    private String month;
    private Integer autoCloseDay;
    private Boolean isClosed;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ConfigResponse fromEntity(SalesForecastConfig entity) {
        ConfigResponse dto = new ConfigResponse();
        dto.setId(entity.getId());
        dto.setMonth(entity.getMonth());
        dto.setAutoCloseDay(entity.getAutoCloseDay());
        dto.setIsClosed(entity.getIsClosed());
        dto.setClosedAt(entity.getClosedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public Integer getAutoCloseDay() { return autoCloseDay; }
    public void setAutoCloseDay(Integer autoCloseDay) { this.autoCloseDay = autoCloseDay; }

    public Boolean getIsClosed() { return isClosed; }
    public void setIsClosed(Boolean isClosed) { this.isClosed = isClosed; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
