package com.sinker.app.dto.erp;

public class VendorSyncParam {

    private Boolean isOnlyUpdate = false;
    private Integer pageSize = 1000;

    public Boolean getIsOnlyUpdate() { return isOnlyUpdate; }
    public void setIsOnlyUpdate(Boolean isOnlyUpdate) { this.isOnlyUpdate = isOnlyUpdate; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
