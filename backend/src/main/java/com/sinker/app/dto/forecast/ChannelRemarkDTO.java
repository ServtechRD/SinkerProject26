package com.sinker.app.dto.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 單一通路的列備註：標示來源通路與月份（YYYYMM），供彙總表顯示 [通路/M月] 備註
 */
public class ChannelRemarkDTO {

    @JsonProperty("channel")
    private String channel;
    @JsonProperty("month")
    private String month;
    @JsonProperty("text")
    private String text;

    public ChannelRemarkDTO() {}

    public ChannelRemarkDTO(String channel, String month, String text) {
        this.channel = channel;
        this.month = month;
        this.text = text;
    }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
