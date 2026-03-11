package com.sinker.app.dto.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 總體銷售預估表單版本一筆（以結束時間為第一版）
 */
public class FormVersionListItemDTO {

    @JsonProperty("version_no")
    private Integer versionNo;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("change_reason")
    private String changeReason;

    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
}
