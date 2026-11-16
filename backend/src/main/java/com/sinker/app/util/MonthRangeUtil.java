package com.sinker.app.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 月份區間（YYYYMM）工具：驗證起訖並展開月份清單，供生產表單與跨月彙整共用
 */
public final class MonthRangeUtil {

    public static final int MAX_MONTH_RANGE = 4;

    private MonthRangeUtil() {}

    public static void validateMonthRange(String startMonth, String endMonth) {
        if (startMonth == null || startMonth.length() != 6 || !startMonth.matches("\\d{6}")) {
            throw new IllegalArgumentException("start_month must be YYYYMM");
        }
        if (endMonth == null || endMonth.length() != 6 || !endMonth.matches("\\d{6}")) {
            throw new IllegalArgumentException("end_month must be YYYYMM");
        }
        if (startMonth.compareTo(endMonth) > 0) {
            throw new IllegalArgumentException("start_month must not be after end_month");
        }
        if (listMonthsInRange(startMonth, endMonth).size() > MAX_MONTH_RANGE) {
            throw new IllegalArgumentException("查詢區間最多 4 個月");
        }
    }

    public static List<String> listMonthsInRange(String startMonth, String endMonth) {
        List<String> out = new ArrayList<>();
        String m = startMonth;
        while (m.compareTo(endMonth) <= 0) {
            out.add(m);
            int y = Integer.parseInt(m.substring(0, 4));
            int mon = Integer.parseInt(m.substring(4, 6));
            mon++;
            if (mon > 12) { mon = 1; y++; }
            m = String.format("%d%02d", y, mon);
        }
        return out;
    }
}
