package com.sinker.app.service;

import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.entity.SalesForecastReopenWindow;
import com.sinker.app.repository.SalesForecastReopenWindowRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/** 業務上傳／編輯前的統一檢查：月份開放中，或該通路有未到期的補登 */
@Service
public class ForecastEditGuard {

    public enum Status { OPEN, CLOSED, REOPENED }

    private final SalesForecastReopenWindowRepository windowRepository;
    private final Clock clock;

    public ForecastEditGuard(SalesForecastReopenWindowRepository windowRepository, Clock clock) {
        this.windowRepository = windowRepository;
        this.clock = clock;
    }

    public Optional<SalesForecastReopenWindow> activeWindow(String month, String channel) {
        return windowRepository.findFirstByMonthAndChannelAndEndedAtIsNullAndExpiresAtAfter(
                month, channel, LocalDateTime.now(clock));
    }

    public Status status(SalesForecastConfig config, String channel) {
        if (!Boolean.TRUE.equals(config.getIsClosed())) {
            return Status.OPEN;
        }
        return activeWindow(config.getMonth(), channel).isPresent() ? Status.REOPENED : Status.CLOSED;
    }

    public void assertBusinessEditable(SalesForecastConfig config, String channel) {
        if (status(config, channel) == Status.CLOSED) {
            throw new AccessDeniedException("表單已截止（" + config.getMonth() + "）");
        }
    }
}
