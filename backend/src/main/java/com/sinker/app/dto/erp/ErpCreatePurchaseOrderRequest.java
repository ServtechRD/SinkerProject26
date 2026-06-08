package com.sinker.app.dto.erp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public class ErpCreatePurchaseOrderRequest {

    @JsonProperty("WebNo")
    private String webNo;

    @JsonProperty("Dep")
    private String dep;

    @JsonProperty("CusNo")
    private String cusNo;

    @JsonProperty("Dd")
    private String dd;

    @JsonProperty("EstDd")
    private String estDd;

    @JsonProperty("PoNo")
    private String poNo;

    @JsonProperty("TaxId")
    private Integer taxId;

    @JsonProperty("Details")
    private List<Detail> details;

    public ErpCreatePurchaseOrderRequest() {}

    public ErpCreatePurchaseOrderRequest(String webNo, String dep, String cusNo,
                                         String dd, String estDd, String poNo,
                                         Integer taxId, List<Detail> details) {
        this.webNo = webNo;
        this.dep = dep;
        this.cusNo = cusNo;
        this.dd = dd;
        this.estDd = estDd;
        this.poNo = poNo;
        this.taxId = taxId;
        this.details = details;
    }

    public String getWebNo() { return webNo; }
    public void setWebNo(String webNo) { this.webNo = webNo; }

    public String getDep() { return dep; }
    public void setDep(String dep) { this.dep = dep; }

    public String getCusNo() { return cusNo; }
    public void setCusNo(String cusNo) { this.cusNo = cusNo; }

    public String getDd() { return dd; }
    public void setDd(String dd) { this.dd = dd; }

    public String getEstDd() { return estDd; }
    public void setEstDd(String estDd) { this.estDd = estDd; }

    public String getPoNo() { return poNo; }
    public void setPoNo(String poNo) { this.poNo = poNo; }

    public Integer getTaxId() { return taxId; }
    public void setTaxId(Integer taxId) { this.taxId = taxId; }

    public List<Detail> getDetails() { return details; }
    public void setDetails(List<Detail> details) { this.details = details; }

    public static class Detail {

        @JsonProperty("Itm")
        private Integer itm;

        @JsonProperty("PrdNo")
        private String prdNo;

        @JsonProperty("PrdName")
        private String prdName;

        @JsonProperty("Wh")
        private String wh;

        @JsonProperty("Unit")
        private String unit;

        @JsonProperty("Qty")
        private BigDecimal qty;

        @JsonProperty("Amt")
        private BigDecimal amt;

        @JsonProperty("Amtn")
        private BigDecimal amtn;

        @JsonProperty("Tax")
        private BigDecimal tax;

        @JsonProperty("Rem")
        private String rem;

        public Detail() {}

        public Detail(Integer itm, String prdNo, String prdName, String wh,
                      String unit, BigDecimal qty, BigDecimal amt, BigDecimal amtn,
                      BigDecimal tax, String rem) {
            this.itm = itm;
            this.prdNo = prdNo;
            this.prdName = prdName;
            this.wh = wh;
            this.unit = unit;
            this.qty = qty;
            this.amt = amt;
            this.amtn = amtn;
            this.tax = tax;
            this.rem = rem;
        }

        public Integer getItm() { return itm; }
        public void setItm(Integer itm) { this.itm = itm; }

        public String getPrdNo() { return prdNo; }
        public void setPrdNo(String prdNo) { this.prdNo = prdNo; }

        public String getPrdName() { return prdName; }
        public void setPrdName(String prdName) { this.prdName = prdName; }

        public String getWh() { return wh; }
        public void setWh(String wh) { this.wh = wh; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }

        public BigDecimal getQty() { return qty; }
        public void setQty(BigDecimal qty) { this.qty = qty; }

        public BigDecimal getAmt() { return amt; }
        public void setAmt(BigDecimal amt) { this.amt = amt; }

        public BigDecimal getAmtn() { return amtn; }
        public void setAmtn(BigDecimal amtn) { this.amtn = amtn; }

        public BigDecimal getTax() { return tax; }
        public void setTax(BigDecimal tax) { this.tax = tax; }

        public String getRem() { return rem; }
        public void setRem(String rem) { this.rem = rem; }
    }
}
