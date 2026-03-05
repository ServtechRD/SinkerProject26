package com.sinker.app.dto.forecast;

import jakarta.validation.constraints.NotBlank;

public class SaveVersionReasonRequest {

    @NotBlank(message = "change_reason is required")
    private String changeReason;

    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
}
