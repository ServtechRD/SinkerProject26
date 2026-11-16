package com.sinker.app.dto.forecast;

public class UpdateConfigRequest {

    private String autoCloseAt;
    private Boolean isClosed;

    public String getAutoCloseAt() { return autoCloseAt; }
    public void setAutoCloseAt(String autoCloseAt) { this.autoCloseAt = autoCloseAt; }

    public Boolean getIsClosed() { return isClosed; }
    public void setIsClosed(Boolean isClosed) { this.isClosed = isClosed; }
}
