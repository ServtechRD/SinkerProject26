package com.sinker.app.dto.reference;

import com.sinker.app.entity.Product;

public class ProductDTO {

    private String code;
    private String name;
    private String categoryName;
    private String spec;
    private String warehouseLocation;

    public ProductDTO() {}

    public static ProductDTO fromEntity(Product entity) {
        ProductDTO dto = new ProductDTO();
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setCategoryName(entity.getCategoryName());
        dto.setSpec(entity.getSpec());
        dto.setWarehouseLocation(entity.getWarehouseLocation());
        return dto;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }

    public String getWarehouseLocation() { return warehouseLocation; }
    public void setWarehouseLocation(String warehouseLocation) { this.warehouseLocation = warehouseLocation; }
}
