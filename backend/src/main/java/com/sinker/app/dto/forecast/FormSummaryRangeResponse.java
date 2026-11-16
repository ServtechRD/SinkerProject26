package com.sinker.app.dto.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 跨月彙整：起訖月份內各品項每月合計（12 通路加總，含禮品）＋全期合計＋各月各通路備註
 */
public class FormSummaryRangeResponse {

    @JsonProperty("months")
    private List<MonthStatusDTO> months = new ArrayList<>();
    @JsonProperty("rows")
    private List<RowDTO> rows = new ArrayList<>();

    public List<MonthStatusDTO> getMonths() { return months; }
    public void setMonths(List<MonthStatusDTO> months) { this.months = months; }

    public List<RowDTO> getRows() { return rows; }
    public void setRows(List<RowDTO> rows) { this.rows = rows; }

    public static class MonthStatusDTO {
        @JsonProperty("month")
        private String month;
        @JsonProperty("closed")
        private boolean closed;

        public MonthStatusDTO() {}

        public MonthStatusDTO(String month, boolean closed) {
            this.month = month;
            this.closed = closed;
        }

        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }

        public boolean isClosed() { return closed; }
        public void setClosed(boolean closed) { this.closed = closed; }
    }

    public static class RowDTO {
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
        /** 月份（YYYYMM）→ 當月合計，順序同 months，無資料為 0 */
        @JsonProperty("month_quantities")
        private Map<String, BigDecimal> monthQuantities = new LinkedHashMap<>();
        @JsonProperty("total")
        private BigDecimal total = BigDecimal.ZERO;
        /** 依月份、再依通路排序 */
        @JsonProperty("remarks")
        private List<ChannelRemarkDTO> remarks = new ArrayList<>();

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

        public Map<String, BigDecimal> getMonthQuantities() { return monthQuantities; }
        public void setMonthQuantities(Map<String, BigDecimal> monthQuantities) { this.monthQuantities = monthQuantities; }

        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = total; }

        public List<ChannelRemarkDTO> getRemarks() { return remarks; }
        public void setRemarks(List<ChannelRemarkDTO> remarks) { this.remarks = remarks; }
    }
}
