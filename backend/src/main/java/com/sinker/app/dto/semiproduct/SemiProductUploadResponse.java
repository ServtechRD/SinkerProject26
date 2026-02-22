package com.sinker.app.dto.semiproduct;

public class SemiProductUploadResponse {

    private String message;
    private int count;

    public SemiProductUploadResponse() {}

    public SemiProductUploadResponse(String message, int count) {
        this.message = message;
        this.count = count;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
