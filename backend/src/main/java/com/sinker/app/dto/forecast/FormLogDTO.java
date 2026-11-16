package com.sinker.app.dto.forecast;

import java.time.LocalDateTime;

/** 表單異動紀錄；createdAt 為台灣時間，operatorName 系統動作為「系統」 */
public record FormLogDTO(Integer id, LocalDateTime createdAt, String operatorName,
                         String action, String channel, String detail) {
}
