package com.sinker.app.dto.forecast;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateMonthsRequest {

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Month format must be YYYYMM")
    private String startMonth;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Month format must be YYYYMM")
    private String endMonth;

    public String getStartMonth() { return startMonth; }
    public void setStartMonth(String startMonth) { this.startMonth = startMonth; }

    public String getEndMonth() { return endMonth; }
    public void setEndMonth(String endMonth) { this.endMonth = endMonth; }
}
