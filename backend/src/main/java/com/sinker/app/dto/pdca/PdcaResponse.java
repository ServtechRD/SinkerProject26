package com.sinker.app.dto.pdca;

import java.util.List;

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

    public static class MaterialItem {
        private String materialCode;
        private String materialName;
        private String unit;
        private String demandDate;
        private Double expectedDelivery;
        private Double demandQuantity;
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
