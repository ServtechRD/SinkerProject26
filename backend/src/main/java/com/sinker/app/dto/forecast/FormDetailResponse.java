package com.sinker.app.dto.forecast;

import java.time.LocalDateTime;
import java.util.List;

/** 表單明細：月份狀態、補登上限、各通路上傳／補登／生管修正狀態 */
public record FormDetailResponse(ConfigResponse config, String closeType, LocalDateTime reopenMaxExpiresAt,
                                 List<ChannelStatus> channels) {

    public record ChannelStatus(String channel, LocalDateTime salesLastUploadAt, LocalDateTime giftLastUploadAt,
                                ActiveWindow activeWindow, int correctionCount) {
    }

    public record ActiveWindow(Integer id, LocalDateTime expiresAt, String reason) {
    }
}
