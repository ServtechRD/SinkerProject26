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

    @Scheduled(cron = "0 45 7 * * ?")
    public void autoCloseMonths() {
        LocalDate currentDate = LocalDate.now();
        log.info("AutoCloseScheduler running - current date: {}", currentDate);

        int closedCount = service.autoCloseMatchingMonths(currentDate);
        log.info("AutoCloseScheduler completed - {} month(s) auto-closed", closedCount);
    }
}
