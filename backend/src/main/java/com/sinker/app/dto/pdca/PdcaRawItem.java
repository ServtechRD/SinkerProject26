package com.sinker.app.dto.pdca;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PdcaRawItem {

    private String PrdNo;
    private String PrdNm;
    private String Ut;
    private String ExpInDate;
    private List<PdcaRawPnQty> PnQtys;

    public String getPrdNo() { return PrdNo; }
    public void setPrdNo(String PrdNo) { this.PrdNo = PrdNo; }

    public String getPrdNm() { return PrdNm; }
    public void setPrdNm(String PrdNm) { this.PrdNm = PrdNm; }

    public String getUt() { return Ut; }
    public void setUt(String Ut) { this.Ut = Ut; }

    public String getExpInDate() { return ExpInDate; }
    public void setExpInDate(String ExpInDate) { this.ExpInDate = ExpInDate; }

    public List<PdcaRawPnQty> getPnQtys() { return PnQtys; }
    public void setPnQtys(List<PdcaRawPnQty> PnQtys) { this.PnQtys = PnQtys; }
}
