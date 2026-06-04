package com.sinker.app.dto.pdca;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PdcaRawPnQty {

    private String PnDd;
    private Double AddQty;
    private Double SubQty;
    private Double EndQty;

    public String getPnDd() { return PnDd; }
    public void setPnDd(String PnDd) { this.PnDd = PnDd; }

    public Double getAddQty() { return AddQty; }
    public void setAddQty(Double AddQty) { this.AddQty = AddQty; }

    public Double getSubQty() { return SubQty; }
    public void setSubQty(Double SubQty) { this.SubQty = SubQty; }

    public Double getEndQty() { return EndQty; }
    public void setEndQty(Double EndQty) { this.EndQty = EndQty; }
}
