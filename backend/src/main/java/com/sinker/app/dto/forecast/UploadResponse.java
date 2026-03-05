package com.sinker.app.dto.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class UploadResponse {

    @JsonProperty("rows_processed")
    private int rowsProcessed;

    private String version;

    @JsonProperty("upload_timestamp")
    private LocalDateTime uploadTimestamp;

    private String month;

    private String channel;

    public UploadResponse() {}

    public UploadResponse(int rowsProcessed, String version, LocalDateTime uploadTimestamp,
                          String month, String channel) {
        this.rowsProcessed = rowsProcessed;
        this.version = version;
        this.uploadTimestamp = uploadTimestamp;
        this.month = month;
        this.channel = channel;
    }

    public int getRowsProcessed() { return rowsProcessed; }
    public void setRowsProcessed(int rowsProcessed) { this.rowsProcessed = rowsProcessed; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public LocalDateTime getUploadTimestamp() { return uploadTimestamp; }
    public void setUploadTimestamp(LocalDateTime uploadTimestamp) { this.uploadTimestamp = uploadTimestamp; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
}
