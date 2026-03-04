package com.sinker.app.dto.productionplan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One channel's monthly quantities (2-12) and total.
 */
public class ProductionFormChannelDTO {

    private String channel;
    @JsonProperty("months")
    private Map<String, BigDecimal> months = new LinkedHashMap<>(); // "2".."12"
    private BigDecimal total;

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public Map<String, BigDecimal> getMonths() { return months; }
    public void setMonths(Map<String, BigDecimal> months) { this.months = months != null ? months : new LinkedHashMap<>(); }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
}
