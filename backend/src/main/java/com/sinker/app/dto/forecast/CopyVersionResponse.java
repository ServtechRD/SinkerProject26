package com.sinker.app.dto.forecast;

public class CopyVersionResponse {

    private String version;

    public CopyVersionResponse() {
    }

    public CopyVersionResponse(String version) {
        this.version = version;
    }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
}
