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
        @JsonAlias({"material_code"})
        private String materialCode;
        @JsonAlias({"material_name"})
        private String materialName;
        private String unit;
        @JsonAlias({"demand_date"})
        private String demandDate;
        @JsonAlias({"expected_delivery"})
        private Double expectedDelivery;
        @JsonAlias({"demand_quantity"})
        private Double demandQuantity;
        @JsonAlias({"estimated_inventory"})
        private Double estimatedInventory;

        public MaterialItem() {
        }

        public MaterialItem(String materialCode, String materialName, String unit,
                           String demandDate, Double expectedDelivery,
                           Double demandQuantity, Double estimatedInventory) {
            this.materialCode = materialCode;
            this.materialName = materialName;
            this.unit = unit;
            this.demandDate = demandDate;
            this.expectedDelivery = expectedDelivery;
            this.demandQuantity = demandQuantity;
            this.estimatedInventory = estimatedInventory;
        }

        public String getMaterialCode() {
            return materialCode;
        }

        public void setMaterialCode(String materialCode) {
            this.materialCode = materialCode;
        }

        public String getMaterialName() {
            return materialName;
        }

        public void setMaterialName(String materialName) {
            this.materialName = materialName;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getDemandDate() {
            return demandDate;
        }

        public void setDemandDate(String demandDate) {
            this.demandDate = demandDate;
        }

        public Double getExpectedDelivery() {
            return expectedDelivery;
        }

        public void setExpectedDelivery(Double expectedDelivery) {
            this.expectedDelivery = expectedDelivery;
        }

        public Double getDemandQuantity() {
            return demandQuantity;
        }

        public void setDemandQuantity(Double demandQuantity) {
            this.demandQuantity = demandQuantity;
        }

        public Double getEstimatedInventory() {
            return estimatedInventory;
        }

        public void setEstimatedInventory(Double estimatedInventory) {
            this.estimatedInventory = estimatedInventory;
        }
    }
}
