package com.sinker.app.dto.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 總體銷售預估表單版本一筆（以結束時間為第一版）
 * created_at_display 為 Asia/Taipei 當地時間字串，供前端顯示用。
 */
public class FormVersionListItemDTO {

    @JsonProperty("version_no")
    private Integer versionNo;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    /** 台灣時間顯示用，格式 yyyy-MM-dd HH:mm:ss */
    @JsonProperty("created_at_display")
    private String createdAtDisplay;
    @JsonProperty("change_reason")
    private String changeReason;

    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedAtDisplay() { return createdAtDisplay; }
    public void setCreatedAtDisplay(String createdAtDisplay) { this.createdAtDisplay = createdAtDisplay; }

    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
}
