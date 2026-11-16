package com.sinker.app.dto.forecast;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateMonthsRequest {

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Month format must be YYYYMM")
    private String month;

    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?$",
            message = "autoCloseAt format must be YYYY-MM-DDTHH:mm")
    private String autoCloseAt;

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public String getAutoCloseAt() { return autoCloseAt; }
    public void setAutoCloseAt(String autoCloseAt) { this.autoCloseAt = autoCloseAt; }
}
