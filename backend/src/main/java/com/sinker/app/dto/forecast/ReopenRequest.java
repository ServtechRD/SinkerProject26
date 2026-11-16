package com.sinker.app.dto.forecast;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 開放補登：可複選通路，共用一個補登截止時間（台灣時間）與原因 */
public class ReopenRequest {

    @NotEmpty(message = "channels is required")
    private List<String> channels;

    @NotBlank(message = "expiresAt is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?$",
            message = "expiresAt format must be YYYY-MM-DDTHH:mm")
    private String expiresAt;

    @NotBlank(message = "reason is required")
    @Size(max = 200, message = "reason must be at most 200 characters")
    private String reason;

    public List<String> getChannels() { return channels; }
    public void setChannels(List<String> channels) { this.channels = channels; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
