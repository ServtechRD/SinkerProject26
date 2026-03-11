package com.sinker.app.dto.productionplan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 生產表單依月份區間查詢結果：月份列表、通路順序、庫存整合版本列表、各列資料。
 */
public class ProductionFormRangeResponse {

    @JsonProperty("month_keys")
    private List<String> monthKeys;
    @JsonProperty("channel_order")
    private List<String> channelOrder;
    @JsonProperty("versions")
    private List<String> versions;
    @JsonProperty("rows")
    private List<ProductionFormRowDTO> rows;

    public List<String> getMonthKeys() { return monthKeys; }
    public void setMonthKeys(List<String> monthKeys) { this.monthKeys = monthKeys; }
    public List<String> getChannelOrder() { return channelOrder; }
    public void setChannelOrder(List<String> channelOrder) { this.channelOrder = channelOrder; }
    public List<String> getVersions() { return versions; }
    public void setVersions(List<String> versions) { this.versions = versions; }
    public List<ProductionFormRowDTO> getRows() { return rows; }
    public void setRows(List<ProductionFormRowDTO> rows) { this.rows = rows; }
}
