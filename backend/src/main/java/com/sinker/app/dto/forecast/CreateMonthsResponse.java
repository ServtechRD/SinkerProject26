package com.sinker.app.dto.forecast;

import java.util.List;

public class CreateMonthsResponse {

    private int createdCount;
    private List<String> months;

    public CreateMonthsResponse(int createdCount, List<String> months) {
        this.createdCount = createdCount;
        this.months = months;
    }

    public int getCreatedCount() { return createdCount; }
    public void setCreatedCount(int createdCount) { this.createdCount = createdCount; }

    public List<String> getMonths() { return months; }
    public void setMonths(List<String> months) { this.months = months; }
}
