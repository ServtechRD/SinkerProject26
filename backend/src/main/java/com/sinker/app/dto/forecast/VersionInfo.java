package com.sinker.app.dto.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class VersionInfo {

    private String version;
    @JsonProperty("item_count")
    private Integer itemCount;
    private LocalDateTime timestamp;

    public VersionInfo() {
    }

    public VersionInfo(String version, Integer itemCount, LocalDateTime timestamp) {
        this.version = version;
        this.itemCount = itemCount;
        this.timestamp = timestamp;
    }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
