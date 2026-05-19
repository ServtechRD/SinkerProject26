package com.sinker.app.dto.erp;

public class ErpProductSyncParam {

    private Boolean isOnlyUpdate = false;
    private String prdNo = "";
    private String name = "";
    private String idx1 = "";
    private String knd = "";
    private Integer pageSize = 1000;

    public Boolean getIsOnlyUpdate() { return isOnlyUpdate; }
    public void setIsOnlyUpdate(Boolean isOnlyUpdate) { this.isOnlyUpdate = isOnlyUpdate; }

    public String getPrdNo() { return prdNo; }
    public void setPrdNo(String prdNo) { this.prdNo = prdNo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIdx1() { return idx1; }
    public void setIdx1(String idx1) { this.idx1 = idx1; }

    public String getKnd() { return knd; }
    public void setKnd(String knd) { this.knd = knd; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
