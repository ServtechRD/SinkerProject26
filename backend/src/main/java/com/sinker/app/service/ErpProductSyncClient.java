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
import org.springframework.web.client.RestTemplate;

@Component
public class ErpProductSyncClient {

    private final IntegrationProperties integrationProperties;
    private final RestTemplate integrationRestTemplate;

    public ErpProductSyncClient(IntegrationProperties integrationProperties,
                                RestTemplate integrationRestTemplate) {
        this.integrationProperties = integrationProperties;
        this.integrationRestTemplate = integrationRestTemplate;
    }

    public boolean isConfigured() {
        IntegrationProperties.ErpProduct cfg = integrationProperties.getErpProduct();
        return cfg.isEnabled() && StringUtils.hasText(cfg.getProductListUrl());
    }

    public ErpProductPageResponse fetchPage(ErpProductListRequest request) {
        IntegrationProperties.ErpProduct cfg = integrationProperties.getErpProduct();
        if (!cfg.isEnabled() || !StringUtils.hasText(cfg.getProductListUrl())) {
            throw new IllegalStateException("ERP product sync not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(cfg.getUsername())) {
            headers.setBasicAuth(cfg.getUsername(), cfg.getPassword() != null ? cfg.getPassword() : "");
        }

        HttpEntity<ErpProductListRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ErpProductPageResponse> response = integrationRestTemplate.exchange(
                cfg.getProductListUrl(),
                HttpMethod.POST,
                entity,
                ErpProductPageResponse.class
        );

        return response.getBody();
    }
}
