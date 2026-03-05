package com.sinker.app.dto.reference;

import com.sinker.app.entity.Product;

public class ProductDTO {

    private String code;
    private String name;

    public ProductDTO() {}

    public static ProductDTO fromEntity(Product entity) {
        ProductDTO dto = new ProductDTO();
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        return dto;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
