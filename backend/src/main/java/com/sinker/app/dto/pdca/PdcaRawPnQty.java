package com.sinker.app.dto.pdca;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PdcaRawPnQty {

    @JsonProperty("PnDd")
    private String pnDd;

    @JsonProperty("AddQty")
    private Double addQty;

    @JsonProperty("SubQty")
    private Double subQty;

    @JsonProperty("EndQty")
    private Double endQty;

    public String getPnDd() { return pnDd; }
    public Double getAddQty() { return addQty; }
    public Double getSubQty() { return subQty; }
    public Double getEndQty() { return endQty; }
}
