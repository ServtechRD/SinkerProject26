package com.sinker.app.dto.semiproduct;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SemiProductUpdateDTO {

    @JsonProperty("advanceDays")
    private Integer advanceDays;

    public SemiProductUpdateDTO() {}

    public Integer getAdvanceDays() { return advanceDays; }
    public void setAdvanceDays(Integer advanceDays) { this.advanceDays = advanceDays; }
}
