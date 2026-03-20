package com.sinker.app.service;

import com.sinker.app.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;

/**
 * 物料需求／手動觸發時呼叫外部 PDCA recompute（與 {@link PdcaApiClientImpl} 共用 {@link PdcaExternalHttpClient}）。
 * 週排程上傳後之回填改由 {@link PdcaIntegrationService} 經 {@link PdcaApiClientImpl} 完成，不再於此重複呼叫。
 */
@Service
public class PdcaRecomputeService {

    private static final Logger log = LoggerFactory.getLogger(PdcaRecomputeService.class);

    private final PdcaExternalHttpClient pdcaExternalHttpClient;

    public PdcaRecomputeService(PdcaExternalHttpClient pdcaExternalHttpClient) {
        this.pdcaExternalHttpClient = pdcaExternalHttpClient;
    }

    /**
     * 非同步重算（物料需求上傳／編輯儲存後）；錯誤只記 log。
     */
    @Async
    public void recomputeAsync(LocalDate weekStart, String factory) {
        if (!pdcaExternalHttpClient.isConfigured()) {
            log.debug("PDCA recompute skipped (not configured): weekStart={}, factory={}", weekStart, factory);
            return;
        }
        try {
            pdcaExternalHttpClient.postRecompute(weekStart, factory);
            log.info("PDCA recompute OK (async): weekStart={}, factory={}", weekStart, factory);
        } catch (RestClientException | IllegalArgumentException | IllegalStateException e) {
            log.error("PDCA recompute async failed: weekStart={}, factory={}, error={}",
                    weekStart, factory, e.getMessage(), e);
        }
    }

    /**
     * 同步重算（手動 API）；啟用且設定完整時失敗會拋出 {@link ExternalApiException}。
     */
    public void recomputeSync(LocalDate weekStart, String factory) {
        if (!pdcaExternalHttpClient.isConfigured()) {
            log.debug("PDCA recompute skipped (not configured): weekStart={}, factory={}", weekStart, factory);
            return;
        }
        try {
            pdcaExternalHttpClient.postRecompute(weekStart, factory);
            log.info("PDCA recompute OK (sync): weekStart={}, factory={}", weekStart, factory);
        } catch (RestClientException e) {
            throw new ExternalApiException("PDCA recompute HTTP error: " + e.getMessage(), e);
        }
    }
}
