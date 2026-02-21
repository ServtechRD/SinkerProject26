package com.sinker.app.dto.schedule;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateScheduleRequest {

    @JsonProperty("demandDate")
    private LocalDate demandDate;

    private BigDecimal quantity;

    public UpdateScheduleRequest() {}

    public LocalDate getDemandDate() {
        return demandDate;
    }

    public void setDemandDate(LocalDate demandDate) {
        this.demandDate = demandDate;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
