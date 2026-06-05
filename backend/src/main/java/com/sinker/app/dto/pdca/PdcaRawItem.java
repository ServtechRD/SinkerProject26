package com.sinker.app.dto.pdca;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PdcaRawItem {

    @JsonProperty("PrdNo")
    private String prdNo;

    @JsonProperty("PrdNm")
    private String prdNm;

    @JsonProperty("Ut")
    private String ut;

    @JsonProperty("LastInDate")
    private String lastInDate;

    @JsonProperty("StkQty")
    private Double stkQty;

    @JsonProperty("ExpInDate")
    private String expInDate;

    @JsonProperty("PnQtys")
    private List<PdcaRawPnQty> pnQtys;

    public String getPrdNo() { return prdNo; }
    public String getPrdNm() { return prdNm; }
    public String getUt() { return ut; }
    public String getLastInDate() { return lastInDate; }
    public Double getStkQty() { return stkQty; }
    public String getExpInDate() { return expInDate; }
    public List<PdcaRawPnQty> getPnQtys() { return pnQtys; }
}
