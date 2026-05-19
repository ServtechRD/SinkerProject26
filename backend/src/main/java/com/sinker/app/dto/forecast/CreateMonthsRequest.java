package com.sinker.app.dto.forecast;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateMonthsRequest {

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Month format must be YYYYMM")
    private String month;

    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "autoCloseDate format must be YYYY-MM-DD")
    private String autoCloseDate;

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public String getAutoCloseDate() { return autoCloseDate; }
    public void setAutoCloseDate(String autoCloseDate) { this.autoCloseDate = autoCloseDate; }
}
