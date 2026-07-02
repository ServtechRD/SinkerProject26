package com.sinker.app.dto.erp;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VendorListRequest {

    @JsonProperty("IsOnlyUpdate")
    private boolean isOnlyUpdate;

    @JsonProperty("NowPage")
    private int nowPage = 1;

    @JsonProperty("PageSize")
    private int pageSize = 1000;

    public boolean isOnlyUpdate() { return isOnlyUpdate; }
    public void setIsOnlyUpdate(boolean isOnlyUpdate) { this.isOnlyUpdate = isOnlyUpdate; }

    public int getNowPage() { return nowPage; }
    public void setNowPage(int nowPage) { this.nowPage = nowPage; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
