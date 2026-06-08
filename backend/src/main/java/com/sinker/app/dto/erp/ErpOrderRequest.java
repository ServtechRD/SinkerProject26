package com.sinker.app.dto.erp;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ErpOrderRequest {

    private Integer itm;
    private String prdNo;
    private String prdName;
    private String wh;
    private BigDecimal qty;
    private LocalDate demandDate;

    public ErpOrderRequest() {}

    public ErpOrderRequest(Integer itm, String prdNo, String prdName, String wh,
                           BigDecimal qty, LocalDate demandDate) {
        this.itm = itm;
        this.prdNo = prdNo;
        this.prdName = prdName;
        this.wh = wh;
        this.qty = qty;
        this.demandDate = demandDate;
    }

    public Integer getItm() { return itm; }
    public void setItm(Integer itm) { this.itm = itm; }

    public String getPrdNo() { return prdNo; }
    public void setPrdNo(String prdNo) { this.prdNo = prdNo; }

    public String getPrdName() { return prdName; }
    public void setPrdName(String prdName) { this.prdName = prdName; }

    public String getWh() { return wh; }
    public void setWh(String wh) { this.wh = wh; }

    public BigDecimal getQty() { return qty; }
    public void setQty(BigDecimal qty) { this.qty = qty; }

    public LocalDate getDemandDate() { return demandDate; }
    public void setDemandDate(LocalDate demandDate) { this.demandDate = demandDate; }
}
