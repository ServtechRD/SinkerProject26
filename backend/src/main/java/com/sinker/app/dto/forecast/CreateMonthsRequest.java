package com.sinker.app.dto.forecast;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateMonthsRequest {

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Month format must be YYYYMM")
    private String startMonth;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Month format must be YYYYMM")
    private String endMonth;

    @Min(value = 1, message = "auto_close_day must be between 1 and 31")
    @Max(value = 31, message = "auto_close_day must be between 1 and 31")
    private Integer autoCloseDay = 10;

    public String getStartMonth() { return startMonth; }
    public void setStartMonth(String startMonth) { this.startMonth = startMonth; }

    public String getEndMonth() { return endMonth; }
    public void setEndMonth(String endMonth) { this.endMonth = endMonth; }

    public Integer getAutoCloseDay() { return autoCloseDay; }
    public void setAutoCloseDay(Integer autoCloseDay) { this.autoCloseDay = autoCloseDay; }
}
