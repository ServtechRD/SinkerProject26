package com.sinker.app.dto.pdca;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PdcaResponse {

    private List<MaterialItem> materials;

    public PdcaResponse() {
    }

    public PdcaResponse(List<MaterialItem> materials) {
        this.materials = materials;
    }

    public List<MaterialItem> getMaterials() {
        return materials;
    }

    public void setMaterials(List<MaterialItem> materials) {
        this.materials = materials;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MaterialItem {
        @JsonAlias({"PrdNo", "material_code"})
        private String materialCode;

        @JsonAlias({"PrdNm", "material_name"})
        private String materialName;

        @JsonAlias({"Ut", "unit"})
        private String unit;

        @JsonAlias({"PnDd", "demand_date"})
        private String demandDate;

        @JsonAlias({"AddQty", "expected_delivery"})
        private Double expectedDelivery;

        @JsonAlias({"SubQty", "demand_quantity"})
        private Double demandQuantity;

        @JsonAlias({"EndQty", "estimated_inventory"})
        private Double estimatedInventory;

        @JsonAlias({"StkQty", "current_stock"})
        private Double currentStock;

        @JsonAlias({"LastInDate", "last_purchase_date"})
        private String lastInDate;

        @JsonAlias({"ExpInDate", "expected_arrival_date"})
        private String expInDate;

        @JsonAlias({"safety_stock"})
        private Double safetyStock;

        @JsonAlias({"standard_lead_time"})
        private Integer standardLeadTime;

        public MaterialItem() {
        }

        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

        public String getMaterialName() { return materialName; }
        public void setMaterialName(String materialName) { this.materialName = materialName; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }

        public String getDemandDate() { return demandDate; }
        public void setDemandDate(String demandDate) { this.demandDate = demandDate; }

        public Double getExpectedDelivery() { return expectedDelivery; }
        public void setExpectedDelivery(Double expectedDelivery) { this.expectedDelivery = expectedDelivery; }

        public Double getDemandQuantity() { return demandQuantity; }
        public void setDemandQuantity(Double demandQuantity) { this.demandQuantity = demandQuantity; }

        public Double getEstimatedInventory() { return estimatedInventory; }
        public void setEstimatedInventory(Double estimatedInventory) { this.estimatedInventory = estimatedInventory; }

        public Double getCurrentStock() { return currentStock; }
        public void setCurrentStock(Double currentStock) { this.currentStock = currentStock; }

        public String getLastInDate() { return lastInDate; }
        public void setLastInDate(String lastInDate) { this.lastInDate = lastInDate; }

        public String getExpInDate() { return expInDate; }
        public void setExpInDate(String expInDate) { this.expInDate = expInDate; }

        public Double getSafetyStock() { return safetyStock; }
        public void setSafetyStock(Double safetyStock) { this.safetyStock = safetyStock; }

        public Integer getStandardLeadTime() { return standardLeadTime; }
        public void setStandardLeadTime(Integer standardLeadTime) { this.standardLeadTime = standardLeadTime; }
    }
}
