package com.sinker.app.dto.forecast;

public class UpdateConfigRequest {

    private String autoCloseDate;
    private Boolean isClosed;

    public String getAutoCloseDate() { return autoCloseDate; }
    public void setAutoCloseDate(String autoCloseDate) { this.autoCloseDate = autoCloseDate; }

    public Boolean getIsClosed() { return isClosed; }
    public void setIsClosed(Boolean isClosed) { this.isClosed = isClosed; }
}
