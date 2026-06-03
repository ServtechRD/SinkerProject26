package com.sinker.app.dto.erp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class ErpProductItem {

    @JsonProperty("PrdNo")
    private String prdNo;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Snm")
    private String snm;

    @JsonProperty("Idx1")
    private String idx1;

    @JsonProperty("IdxName")
    private String idxName;

    @JsonProperty("Ut")
    private String ut;

    @JsonProperty("Ut1")
    private String ut1;

    @JsonProperty("Spc")
    private String spc;

    @JsonProperty("Wh")
    private String wh;

    @JsonProperty("Knd")
    private String knd;

    @JsonProperty("DfuUt")
    private String dfuUt;

    @JsonProperty("SpcTax")
    private BigDecimal spcTax;

    @JsonProperty("Pk2Ut")
    private String pk2Ut;

    @JsonProperty("Pk2Qty")
    private BigDecimal pk2Qty;

    @JsonProperty("Pk3Ut")
    private String pk3Ut;

    @JsonProperty("Pk3Qty")
    private BigDecimal pk3Qty;

    @JsonProperty("Upr")
    private BigDecimal upr;

    @JsonProperty("Rem")
    private String rem;

    @JsonProperty("NouseDd")
    private String nouseDd;

    @JsonProperty("BarCode")
    private String barCode;

    @JsonProperty("NameEng")
    private String nameEng;

    @JsonProperty("QtyLow")
    private BigDecimal qtyLow;

    @JsonProperty("ValidDays")
    private Integer validDays;

    @JsonProperty("QtyMin1")
    private BigDecimal qtyMin1;

    @JsonProperty("QtyMax")
    private BigDecimal qtyMax;

    @JsonProperty("ChkDate")
    private String chkDate;

    @JsonProperty("ModifyDate")
    private String modifyDate;

    public String getPrdNo() { return prdNo; }
    public void setPrdNo(String prdNo) { this.prdNo = prdNo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSnm() { return snm; }
    public void setSnm(String snm) { this.snm = snm; }

    public String getIdx1() { return idx1; }
    public void setIdx1(String idx1) { this.idx1 = idx1; }

    public String getIdxName() { return idxName; }
    public void setIdxName(String idxName) { this.idxName = idxName; }

    public String getUt() { return ut; }
    public void setUt(String ut) { this.ut = ut; }

    public String getUt1() { return ut1; }
    public void setUt1(String ut1) { this.ut1 = ut1; }

    public String getSpc() { return spc; }
    public void setSpc(String spc) { this.spc = spc; }

    public String getWh() { return wh; }
    public void setWh(String wh) { this.wh = wh; }

    public String getKnd() { return knd; }
    public void setKnd(String knd) { this.knd = knd; }

    public String getDfuUt() { return dfuUt; }
    public void setDfuUt(String dfuUt) { this.dfuUt = dfuUt; }

    public BigDecimal getSpcTax() { return spcTax; }
    public void setSpcTax(BigDecimal spcTax) { this.spcTax = spcTax; }

    public String getPk2Ut() { return pk2Ut; }
    public void setPk2Ut(String pk2Ut) { this.pk2Ut = pk2Ut; }

    public BigDecimal getPk2Qty() { return pk2Qty; }
    public void setPk2Qty(BigDecimal pk2Qty) { this.pk2Qty = pk2Qty; }

    public String getPk3Ut() { return pk3Ut; }
    public void setPk3Ut(String pk3Ut) { this.pk3Ut = pk3Ut; }

    public BigDecimal getPk3Qty() { return pk3Qty; }
    public void setPk3Qty(BigDecimal pk3Qty) { this.pk3Qty = pk3Qty; }

    public BigDecimal getUpr() { return upr; }
    public void setUpr(BigDecimal upr) { this.upr = upr; }

    public String getRem() { return rem; }
    public void setRem(String rem) { this.rem = rem; }

    public String getNouseDd() { return nouseDd; }
    public void setNouseDd(String nouseDd) { this.nouseDd = nouseDd; }

    public String getBarCode() { return barCode; }
    public void setBarCode(String barCode) { this.barCode = barCode; }

    public String getNameEng() { return nameEng; }
    public void setNameEng(String nameEng) { this.nameEng = nameEng; }

    public BigDecimal getQtyLow() { return qtyLow; }
    public void setQtyLow(BigDecimal qtyLow) { this.qtyLow = qtyLow; }

    public Integer getValidDays() { return validDays; }
    public void setValidDays(Integer validDays) { this.validDays = validDays; }

    public BigDecimal getQtyMin1() { return qtyMin1; }
    public void setQtyMin1(BigDecimal qtyMin1) { this.qtyMin1 = qtyMin1; }

    public BigDecimal getQtyMax() { return qtyMax; }
    public void setQtyMax(BigDecimal qtyMax) { this.qtyMax = qtyMax; }

    public String getChkDate() { return chkDate; }
    public void setChkDate(String chkDate) { this.chkDate = chkDate; }

    public String getModifyDate() { return modifyDate; }
    public void setModifyDate(String modifyDate) { this.modifyDate = modifyDate; }
}
