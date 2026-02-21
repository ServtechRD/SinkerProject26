package com.sinker.app.dto.erp;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ErpOrderRequest {

    private String itm;
    private String prdNo;
    private BigDecimal qty;
    private LocalDate demandDate;

    public ErpOrderRequest() {}

    public ErpOrderRequest(String itm, String prdNo, BigDecimal qty, LocalDate demandDate) {
        this.itm = itm;
        this.prdNo = prdNo;
        this.qty = qty;
        this.demandDate = demandDate;
    }

    public String getItm() {
        return itm;
    }

    public void setItm(String itm) {
        this.itm = itm;
    }

    public String getPrdNo() {
        return prdNo;
    }

    public void setPrdNo(String prdNo) {
        this.prdNo = prdNo;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public LocalDate getDemandDate() {
        return demandDate;
    }

    public void setDemandDate(LocalDate demandDate) {
        this.demandDate = demandDate;
    }
}
