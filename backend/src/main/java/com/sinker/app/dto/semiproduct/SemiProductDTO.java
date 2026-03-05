package com.sinker.app.dto.semiproduct;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sinker.app.entity.SemiProductAdvancePurchase;

import java.time.LocalDateTime;

public class SemiProductDTO {

    private Integer id;

    @JsonProperty("productCode")
    private String productCode;

    @JsonProperty("productName")
    private String productName;

    @JsonProperty("advanceDays")
    private Integer advanceDays;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    public SemiProductDTO() {}

    public static SemiProductDTO fromEntity(SemiProductAdvancePurchase entity) {
        SemiProductDTO dto = new SemiProductDTO();
        dto.setId(entity.getId());
        dto.setProductCode(entity.getProductCode());
        dto.setProductName(entity.getProductName());
        dto.setAdvanceDays(entity.getAdvanceDays());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getAdvanceDays() { return advanceDays; }
    public void setAdvanceDays(Integer advanceDays) { this.advanceDays = advanceDays; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
