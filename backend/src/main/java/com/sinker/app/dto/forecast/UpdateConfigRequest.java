package com.sinker.app.dto.forecast;

public class UpdateConfigRequest {

    private Integer autoCloseDay;
    private Boolean isClosed;

    public Integer getAutoCloseDay() { return autoCloseDay; }
    public void setAutoCloseDay(Integer autoCloseDay) { this.autoCloseDay = autoCloseDay; }

    public Boolean getIsClosed() { return isClosed; }
    public void setIsClosed(Boolean isClosed) { this.isClosed = isClosed; }
}
