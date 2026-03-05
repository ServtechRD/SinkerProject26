package com.sinker.app.dto.productionplan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One row of the production form: product + per-channel months + aggregate + buffer + totals.
 */
public class ProductionFormRowDTO {

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

    @JsonProperty("channel_data")
    private List<ProductionFormChannelDTO> channelData = new ArrayList<>();

    @JsonProperty("aggregate_months")
    private Map<String, BigDecimal> aggregateMonths = new LinkedHashMap<>(); // "2".."12"
    @JsonProperty("buffer_quantity")
    private BigDecimal bufferQuantity = BigDecimal.ZERO;
    @JsonProperty("aggregate_total")
    private BigDecimal aggregateTotal = BigDecimal.ZERO;
    @JsonProperty("original_forecast")
    private BigDecimal originalForecast = BigDecimal.ZERO;
    private BigDecimal difference = BigDecimal.ZERO;
    private String remarks;

    @JsonProperty("production_form_id")
    private Integer productionFormId; // for buffer update

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
    public List<ProductionFormChannelDTO> getChannelData() { return channelData; }
    public void setChannelData(List<ProductionFormChannelDTO> channelData) { this.channelData = channelData != null ? channelData : new ArrayList<>(); }
    public Map<String, BigDecimal> getAggregateMonths() { return aggregateMonths; }
    public void setAggregateMonths(Map<String, BigDecimal> aggregateMonths) { this.aggregateMonths = aggregateMonths != null ? aggregateMonths : new LinkedHashMap<>(); }
    public BigDecimal getBufferQuantity() { return bufferQuantity; }
    public void setBufferQuantity(BigDecimal bufferQuantity) { this.bufferQuantity = bufferQuantity; }
    public BigDecimal getAggregateTotal() { return aggregateTotal; }
    public void setAggregateTotal(BigDecimal aggregateTotal) { this.aggregateTotal = aggregateTotal; }
    public BigDecimal getOriginalForecast() { return originalForecast; }
    public void setOriginalForecast(BigDecimal originalForecast) { this.originalForecast = originalForecast; }
    public BigDecimal getDifference() { return difference; }
    public void setDifference(BigDecimal difference) { this.difference = difference; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public Integer getProductionFormId() { return productionFormId; }
    public void setProductionFormId(Integer productionFormId) { this.productionFormId = productionFormId; }
}
