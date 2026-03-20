package com.sinker.app.dto.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 表單摘要中單一通路的一格：原小計、小計、差異數、備註。
 * current_qty 為顯示用（銷售+禮品加總），current_sales_qty 為編輯/儲存用（僅銷售）。
 */
public class ChannelCellDTO {

    @JsonProperty("previous_qty")
    private BigDecimal previousQty;
    @JsonProperty("current_qty")
    private BigDecimal currentQty;
    /** 該通路銷售數量（僅銷售預估上傳），供編輯與儲存用；若為 null 則與 current_qty 同 */
    @JsonProperty("current_sales_qty")
    private BigDecimal currentSalesQty;
    @JsonProperty("diff")
    private BigDecimal diff;
    @JsonProperty("remark")
    private String remark;

    public BigDecimal getPreviousQty() { return previousQty; }
    public void setPreviousQty(BigDecimal previousQty) { this.previousQty = previousQty; }

    public BigDecimal getCurrentQty() { return currentQty; }
    public void setCurrentQty(BigDecimal currentQty) { this.currentQty = currentQty; }

    public BigDecimal getCurrentSalesQty() { return currentSalesQty; }
    public void setCurrentSalesQty(BigDecimal currentSalesQty) { this.currentSalesQty = currentSalesQty; }

    public BigDecimal getDiff() { return diff; }
    public void setDiff(BigDecimal diff) { this.diff = diff; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
