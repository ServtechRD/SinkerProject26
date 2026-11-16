package com.sinker.app.util;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

public final class WorkingDays {

    private WorkingDays() {
    }

    /**
     * 從 start 起算第 days 個工作天的同一時刻（只跳過週六日）。
     * ponytail: 不含國定假日，需要時再加假日表
     */
    public static LocalDateTime addWorkingDays(LocalDateTime start, int days) {
        LocalDateTime t = start;
        int added = 0;
        while (added < days) {
            t = t.plusDays(1);
            DayOfWeek d = t.getDayOfWeek();
            if (d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return t;
    }
}
