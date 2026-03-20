package com.sinker.app.dto.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 儲存新版本請求：修改原因（可為空）+ 各列各通路數量
 */
public class SaveFormSummaryVersionRequest {

    @JsonProperty("change_reason")
    private String changeReason;

    @NotNull(message = "rows is required")
    @JsonProperty("rows")
    private List<FormSummaryRowEditDTO> rows;

    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }

    public List<FormSummaryRowEditDTO> getRows() { return rows; }
    public void setRows(List<FormSummaryRowEditDTO> rows) { this.rows = rows; }

    /**
     * 一列編輯：productKey 識別列，channel_quantities 為各通路數量（順序同 channel_order）
     */
    public static class FormSummaryRowEditDTO {
        @JsonProperty("warehouse_location")
        private String warehouseLocation;
        @JsonProperty("category")
        private String category;
        @JsonProperty("spec")
        private String spec;
        @JsonProperty("product_name")
        private String productName;
        @JsonProperty("product_code")
        private String productCode;
        /** 該列備註（與修改原因獨立） */
        @JsonProperty("remark")
        private String remark;
        @JsonProperty("channel_quantities")
        private List<java.math.BigDecimal> channelQuantities;

        public String getWarehouseLocation() { return warehouseLocation; }
        public void setWarehouseLocation(String warehouseLocation) { this.warehouseLocation = warehouseLocation; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getSpec() { return spec; }
        public void setSpec(String spec) { this.spec = spec; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getProductCode() { return productCode; }
        public void setProductCode(String productCode) { this.productCode = productCode; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
        public List<java.math.BigDecimal> getChannelQuantities() { return channelQuantities; }
        public void setChannelQuantities(List<java.math.BigDecimal> channelQuantities) { this.channelQuantities = channelQuantities; }
    }
}
