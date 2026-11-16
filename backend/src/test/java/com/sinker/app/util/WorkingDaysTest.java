package com.sinker.app.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkingDaysTest {

    @Test
    void monday_plus3_isThursday() {
        assertEquals(LocalDateTime.of(2026, 12, 10, 10, 0),
                WorkingDays.addWorkingDays(LocalDateTime.of(2026, 12, 7, 10, 0), 3));
    }

    @Test
    void friday_plus3_skipsWeekend() {
        assertEquals(LocalDateTime.of(2026, 12, 16, 10, 0),
                WorkingDays.addWorkingDays(LocalDateTime.of(2026, 12, 11, 10, 0), 3));
    }

    @Test
    void saturday_plus3_isWednesday() {
        assertEquals(LocalDateTime.of(2026, 12, 16, 10, 0),
                WorkingDays.addWorkingDays(LocalDateTime.of(2026, 12, 12, 10, 0), 3));
    }

    @Test
    void sunday_plus3_isWednesday() {
        assertEquals(LocalDateTime.of(2026, 12, 16, 10, 0),
                WorkingDays.addWorkingDays(LocalDateTime.of(2026, 12, 13, 10, 0), 3));
    }
}
