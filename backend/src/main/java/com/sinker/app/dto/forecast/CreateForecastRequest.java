package com.sinker.app.dto.forecast;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateForecastRequest {

    @NotBlank(message = "month is required")
    private String month;

    @NotBlank(message = "channel is required")
    private String channel;

    private String category;

    private String spec;

    @NotBlank(message = "product_code is required")
    private String productCode;

    private String productName;

    private String warehouseLocation;

    @NotNull(message = "quantity is required")
    @DecimalMin(value = "0.01", message = "quantity must be positive")
    private BigDecimal quantity;

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

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

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}
