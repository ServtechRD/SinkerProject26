package com.sinker.app.scheduler;

import com.sinker.app.dto.erp.ErpProductSyncParam;
import com.sinker.app.service.ErpProductSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProductSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProductSyncScheduler.class);

    private final ErpProductSyncService service;

    public ProductSyncScheduler(ErpProductSyncService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 45 7 * * ?")
    public void scheduledProductSync() {
        if (service.isRunning()) {
            log.warn("ProductSyncScheduler skipped - sync already running");
            return;
        }
        log.info("ProductSyncScheduler triggering ERP product sync");
        service.syncProductsAsync(new ErpProductSyncParam());
    }
}
