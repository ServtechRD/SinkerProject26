package com.sinker.app.dto.pdca;

import java.util.List;

public class PdcaRequest {

    private List<ScheduleItem> schedule;

    public PdcaRequest() {
    }

    public PdcaRequest(List<ScheduleItem> schedule) {
        this.schedule = schedule;
    }

    public List<ScheduleItem> getSchedule() {
        return schedule;
    }

    public void setSchedule(List<ScheduleItem> schedule) {
        this.schedule = schedule;
    }

    public static class ScheduleItem {
        private String productCode;
        private Double quantity;
        private String demandDate;

        public ScheduleItem() {
        }

        public ScheduleItem(String productCode, Double quantity, String demandDate) {
            this.productCode = productCode;
            this.quantity = quantity;
            this.demandDate = demandDate;
        }

        public String getProductCode() {
            return productCode;
        }

        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        public Double getQuantity() {
            return quantity;
        }

        public void setQuantity(Double quantity) {
            this.quantity = quantity;
        }

        public String getDemandDate() {
            return demandDate;
        }

        public void setDemandDate(String demandDate) {
            this.demandDate = demandDate;
        }
    }
}
