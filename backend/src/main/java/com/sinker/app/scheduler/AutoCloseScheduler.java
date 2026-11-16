package com.sinker.app.scheduler;

import com.sinker.app.service.ReopenService;
import com.sinker.app.service.SalesForecastConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AutoCloseScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutoCloseScheduler.class);

    private final SalesForecastConfigService service;

    private final ReopenService reopenService;

    public AutoCloseScheduler(SalesForecastConfigService service, ReopenService reopenService) {
        this.service = service;
        this.reopenService = reopenService;
    }

    /** 每分鐘：自動截止到期的月份、結束到期的補登 */
    @Scheduled(cron = "0 * * * * ?")
    public void runEveryMinute() {
        try {
            int closed = service.autoCloseDue();
            if (closed > 0) {
                log.info("AutoCloseScheduler - {} month(s) auto-closed", closed);
            }
        } catch (Exception e) {
            log.error("AutoCloseScheduler - 自動截止失敗", e);
        }
        List<String> months;
        try {
            months = reopenService.findExpiredMonths();
        } catch (Exception e) {
            log.error("AutoCloseScheduler - 查詢到期補登失敗", e);
            return;
        }
        for (String month : months) {
            try {
                int ended = reopenService.finishExpiredMonth(month);
                log.info("AutoCloseScheduler - month {} 結束 {} 筆補登", month, ended);
            } catch (Exception e) {
                // 單一月份失敗不影響其他月份；下一分鐘重試
                log.error("AutoCloseScheduler - 月份 {} 補登到期處理失敗", month, e);
            }
        }
    }
}
