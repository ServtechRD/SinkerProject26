package com.sinker.app.dto.forecast;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/** 業務上傳頁的編輯狀態；plannerQuantities 為最新生管版本中該通路各品號數量（含禮品），未結束時為 null */
public record EditStatusResponse(String status, LocalDateTime autoCloseAt, LocalDateTime reopenExpiresAt,
                                 String reopenReason, Map<String, BigDecimal> plannerQuantities) {
}
