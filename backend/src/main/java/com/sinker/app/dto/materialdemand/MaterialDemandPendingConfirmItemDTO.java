package com.sinker.app.dto.materialdemand;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 待確認送出 ERP 清單項目（含最後觸發編輯儲存時間） */
public class MaterialDemandPendingConfirmItemDTO {

    private LocalDate weekStart;
    private String factory;
    private LocalDateTime updatedAt;

    public MaterialDemandPendingConfirmItemDTO() {}

    public MaterialDemandPendingConfirmItemDTO(LocalDate weekStart, String factory, LocalDateTime updatedAt) {
        this.weekStart = weekStart;
        this.factory = factory;
        this.updatedAt = updatedAt;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
