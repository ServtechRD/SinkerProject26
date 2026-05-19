package com.sinker.app.dto.erp;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErpProductListRequest {

    @JsonProperty("IsOnlyUpdate")
    private boolean isOnlyUpdate;

    @JsonProperty("PrdNo")
    private String prdNo = "";

    @JsonProperty("Name")
    private String name = "";

    @JsonProperty("Idx1")
    private String idx1 = "";

    @JsonProperty("Knd")
    private String knd = "";

    @JsonProperty("NowPage")
    private int nowPage = 1;

    @JsonProperty("PageSize")
    private int pageSize = 1000;

    public boolean isOnlyUpdate() { return isOnlyUpdate; }
    public void setIsOnlyUpdate(boolean isOnlyUpdate) { this.isOnlyUpdate = isOnlyUpdate; }

    public String getPrdNo() { return prdNo; }
    public void setPrdNo(String prdNo) { this.prdNo = prdNo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIdx1() { return idx1; }
    public void setIdx1(String idx1) { this.idx1 = idx1; }

    public String getKnd() { return knd; }
    public void setKnd(String knd) { this.knd = knd; }

    public int getNowPage() { return nowPage; }
    public void setNowPage(int nowPage) { this.nowPage = nowPage; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
