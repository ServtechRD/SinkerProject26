package com.sinker.app.dto.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 表單摘要一列：庫位、中類名稱、貨品規格、品名、品號 + 各通路欄位
 */
public class FormSummaryRowDTO {

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
    /** 各通路列備註（與版本修改原因獨立），順序同 channel_order */
    @JsonProperty("remarks")
    private List<ChannelRemarkDTO> remarks;
    @JsonProperty("channel_cells")
    private List<ChannelCellDTO> channelCells;

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

    public List<ChannelRemarkDTO> getRemarks() { return remarks; }
    public void setRemarks(List<ChannelRemarkDTO> remarks) { this.remarks = remarks; }

    public List<ChannelCellDTO> getChannelCells() { return channelCells; }
    public void setChannelCells(List<ChannelCellDTO> channelCells) { this.channelCells = channelCells; }
}
