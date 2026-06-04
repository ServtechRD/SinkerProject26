package com.sinker.app.service;

import com.sinker.app.config.IntegrationProperties;
import com.sinker.app.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;

/**
 * 呼叫外部 ERP「採購單建立」API（URL／帳密由 {@link IntegrationProperties} 注入）。
 */
@Service
public class ErpPurchaseOrderService {

    private static final Logger log = LoggerFactory.getLogger(ErpPurchaseOrderService.class);

    private final IntegrationProperties integrationProperties;
    private final RestTemplate integrationRestTemplate;
    private final ErpTokenService erpTokenService;

    public ErpPurchaseOrderService(IntegrationProperties integrationProperties,
                                   RestTemplate integrationRestTemplate,
                                   ErpTokenService erpTokenService) {
        this.integrationProperties = integrationProperties;
        this.integrationRestTemplate = integrationRestTemplate;
        this.erpTokenService = erpTokenService;
    }

    /**
     * 建立採購單。未啟用或未設定 URL 時略過（僅 log），供開發環境使用。
     * 啟用且呼叫失敗時拋出 {@link ExternalApiException}。
     * 收到 401 時自動清除 token 快取並重試一次。
     */
    public void createPurchaseOrder(LocalDate weekStart, String factory) {
        IntegrationProperties.Erp cfg = integrationProperties.getErp();
        if (!cfg.isEnabled() || !StringUtils.hasText(cfg.getPurchaseOrderUrl())) {
            log.info("ERP purchase order skipped (disabled or empty URL): weekStart={}, factory={}", weekStart, factory);
            return;
        }
        if (factory == null || factory.isBlank()) {
            throw new IllegalArgumentException("factory is required");
        }

        Map<String, Object> body = Map.of(
                "week_start", weekStart.toString(),
                "factory", factory
        );

        try {
            doPost(cfg.getPurchaseOrderUrl(), body, weekStart, factory);
        } catch (HttpClientErrorException.Unauthorized e) {
            erpTokenService.invalidate();
            doPost(cfg.getPurchaseOrderUrl(), body, weekStart, factory);
        } catch (RestClientException e) {
            log.error("ERP purchase order HTTP error: weekStart={}, factory={}", weekStart, factory, e);
            throw new ExternalApiException("ERP purchase order failed: " + e.getMessage(), e);
        }
    }

    private void doPost(String url, Map<String, Object> body, LocalDate weekStart, String factory) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(erpTokenService.getToken());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = integrationRestTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("ERP purchase order OK: weekStart={}, factory={}, status={}",
                        weekStart, factory, response.getStatusCode());
            } else {
                throw new ExternalApiException("ERP purchase order returned " + response.getStatusCode());
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            throw e;
        } catch (RestClientException e) {
            log.error("ERP purchase order HTTP error: weekStart={}, factory={}", weekStart, factory, e);
            throw new ExternalApiException("ERP purchase order failed: " + e.getMessage(), e);
        }
    }
}
