package com.sinker.app.dto.erp;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VendorSyncItem {

    @JsonProperty("CusNo")
    private String cusNo;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("SysDate")
    private String sysDate;

    @JsonProperty("ModifyDate")
    private String modifyDate;

    public String getCusNo() { return cusNo; }
    public void setCusNo(String cusNo) { this.cusNo = cusNo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSysDate() { return sysDate; }
    public void setSysDate(String sysDate) { this.sysDate = sysDate; }

    public String getModifyDate() { return modifyDate; }
    public void setModifyDate(String modifyDate) { this.modifyDate = modifyDate; }
}
