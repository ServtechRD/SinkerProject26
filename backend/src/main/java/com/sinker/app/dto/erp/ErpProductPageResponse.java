package com.sinker.app.dto.erp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ErpProductPageResponse {

    @JsonProperty("Datas")
    private List<ErpProductItem> datas;

    @JsonProperty("NowPage")
    private int nowPage;

    @JsonProperty("TotalPage")
    private int totalPage;

    public List<ErpProductItem> getDatas() { return datas; }
    public void setDatas(List<ErpProductItem> datas) { this.datas = datas; }

    public int getNowPage() { return nowPage; }
    public void setNowPage(int nowPage) { this.nowPage = nowPage; }

    public int getTotalPage() { return totalPage; }
    public void setTotalPage(int totalPage) { this.totalPage = totalPage; }
}
