package com.sinker.app.service;

import com.sinker.app.config.IntegrationProperties;
import com.sinker.app.dto.erp.ErpProductListRequest;
import com.sinker.app.dto.erp.ErpProductPageResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class ErpProductSyncClient {

    private final IntegrationProperties integrationProperties;
    private final RestTemplate integrationRestTemplate;
    private final ErpTokenService erpTokenService;

    public ErpProductSyncClient(IntegrationProperties integrationProperties,
                                RestTemplate integrationRestTemplate,
                                ErpTokenService erpTokenService) {
        this.integrationProperties = integrationProperties;
        this.integrationRestTemplate = integrationRestTemplate;
        this.erpTokenService = erpTokenService;
    }

    public boolean isConfigured() {
        IntegrationProperties.ErpProduct cfg = integrationProperties.getErpProduct();
        return cfg.isEnabled() && StringUtils.hasText(cfg.getProductListUrl());
    }

    /** 收到 401 時自動清除 token 快取並重試一次。 */
    public ErpProductPageResponse fetchPage(ErpProductListRequest request) {
        IntegrationProperties.ErpProduct cfg = integrationProperties.getErpProduct();
        if (!cfg.isEnabled() || !StringUtils.hasText(cfg.getProductListUrl())) {
            throw new IllegalStateException("ERP product sync not configured");
        }

        try {
            return doPost(cfg.getProductListUrl(), request);
        } catch (HttpClientErrorException.Unauthorized e) {
            erpTokenService.invalidate();
            return doPost(cfg.getProductListUrl(), request);
        }
    }

    private ErpProductPageResponse doPost(String url, ErpProductListRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(erpTokenService.getToken());

        HttpEntity<ErpProductListRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<ErpProductPageResponse> response = integrationRestTemplate.exchange(
                url, HttpMethod.POST, entity, ErpProductPageResponse.class);

        return response.getBody();
    }
}
