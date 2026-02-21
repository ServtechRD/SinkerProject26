package com.sinker.app.dto.schedule;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class UploadScheduleResponse {

    private String message;

    @JsonProperty("recordsInserted")
    private int recordsInserted;

    @JsonProperty("weekStart")
    private LocalDate weekStart;

    private String factory;

    public UploadScheduleResponse() {}

    public UploadScheduleResponse(String message, int recordsInserted, LocalDate weekStart, String factory) {
        this.message = message;
        this.recordsInserted = recordsInserted;
        this.weekStart = weekStart;
        this.factory = factory;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getRecordsInserted() {
        return recordsInserted;
    }

    public void setRecordsInserted(int recordsInserted) {
        this.recordsInserted = recordsInserted;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(LocalDate weekStart) {
        this.weekStart = weekStart;
    }

    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }
}
