package com.sinker.app.dto.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * One row in version diff: same product, different quantity between current and previous version.
 */
public class VersionDiffItemDTO {

    private String category;
    private String spec;
    @JsonProperty("product_code")
    private String productCode;
    @JsonProperty("product_name")
    private String productName;
    @JsonProperty("warehouse_location")
    private String warehouseLocation;
    @JsonProperty("current_quantity")
    private BigDecimal currentQuantity;
    @JsonProperty("previous_quantity")
    private BigDecimal previousQuantity;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getWarehouseLocation() { return warehouseLocation; }
    public void setWarehouseLocation(String warehouseLocation) { this.warehouseLocation = warehouseLocation; }

    public BigDecimal getCurrentQuantity() { return currentQuantity; }
    public void setCurrentQuantity(BigDecimal currentQuantity) { this.currentQuantity = currentQuantity; }

    public BigDecimal getPreviousQuantity() { return previousQuantity; }
    public void setPreviousQuantity(BigDecimal previousQuantity) { this.previousQuantity = previousQuantity; }
}
