package com.sinker.app.dto.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 表單摘要中單一通路的一格：原小計、小計、差異數、備註
 */
public class ChannelCellDTO {

    @JsonProperty("previous_qty")
    private BigDecimal previousQty;
    @JsonProperty("current_qty")
    private BigDecimal currentQty;
    @JsonProperty("diff")
    private BigDecimal diff;
    @JsonProperty("remark")
    private String remark;

    public BigDecimal getPreviousQty() { return previousQty; }
    public void setPreviousQty(BigDecimal previousQty) { this.previousQty = previousQty; }

    public BigDecimal getCurrentQty() { return currentQty; }
    public void setCurrentQty(BigDecimal currentQty) { this.currentQty = currentQty; }

    public BigDecimal getDiff() { return diff; }
    public void setDiff(BigDecimal diff) { this.diff = diff; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
