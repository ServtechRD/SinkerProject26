package com.sinker.app.scheduler;

import com.sinker.app.service.SalesForecastConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AutoCloseScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutoCloseScheduler.class);

    private final SalesForecastConfigService service;

    public AutoCloseScheduler(SalesForecastConfigService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void autoCloseMonths() {
        int currentDay = LocalDate.now().getDayOfMonth();
        log.info("AutoCloseScheduler running - current day of month: {}", currentDay);

        int closedCount = service.autoCloseMatchingMonths(currentDay);
        log.info("AutoCloseScheduler completed - {} month(s) auto-closed", closedCount);
    }
}
