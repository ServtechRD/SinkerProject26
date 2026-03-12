package com.sinker.app.dto.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 銷售預估量表單查詢結果：各通路最新版號 + 彙總列；可選版本號與該版備註
 */
public class FormSummaryResponse {

    @JsonProperty("channel_versions")
    private List<ChannelVersionInfoDTO> channelVersions;
    @JsonProperty("channel_order")
    private List<String> channelOrder;
    @JsonProperty("rows")
    private List<FormSummaryRowDTO> rows;
    @JsonProperty("version_no")
    private Integer versionNo;
    @JsonProperty("version_remark")
    private String versionRemark;

    public List<ChannelVersionInfoDTO> getChannelVersions() { return channelVersions; }
    public void setChannelVersions(List<ChannelVersionInfoDTO> channelVersions) { this.channelVersions = channelVersions; }

    public List<String> getChannelOrder() { return channelOrder; }
    public void setChannelOrder(List<String> channelOrder) { this.channelOrder = channelOrder; }

    public List<FormSummaryRowDTO> getRows() { return rows; }
    public void setRows(List<FormSummaryRowDTO> rows) { this.rows = rows; }

    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }

    public String getVersionRemark() { return versionRemark; }
    public void setVersionRemark(String versionRemark) { this.versionRemark = versionRemark; }
}
