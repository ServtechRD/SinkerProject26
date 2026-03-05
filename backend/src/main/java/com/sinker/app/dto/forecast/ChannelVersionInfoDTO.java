package com.sinker.app.dto.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 通路及其最新版號（用於表單查詢條件顯示）
 */
public class ChannelVersionInfoDTO {

    private String channel;
    @JsonProperty("latest_version")
    private String latestVersion;

    public ChannelVersionInfoDTO() {
    }

    public ChannelVersionInfoDTO(String channel, String latestVersion) {
        this.channel = channel;
        this.latestVersion = latestVersion;
    }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getLatestVersion() { return latestVersion; }
    public void setLatestVersion(String latestVersion) { this.latestVersion = latestVersion; }
}
