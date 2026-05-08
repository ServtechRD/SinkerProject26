package com.sinker.app.dto.materialdemand;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 待確認送出 ERP 清單項目（含最後觸發編輯儲存時間） */
public class MaterialDemandPendingConfirmItemDTO {

    private LocalDate weekStart;
    private String factory;
    /** 0 待審核、1 審核完成、2 退回 */
    private Integer status;
    private LocalDateTime updatedAt;

    public MaterialDemandPendingConfirmItemDTO() {}

    public MaterialDemandPendingConfirmItemDTO(LocalDate weekStart, String factory, LocalDateTime updatedAt) {
        this.weekStart = weekStart;
        this.factory = factory;
        this.updatedAt = updatedAt;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
