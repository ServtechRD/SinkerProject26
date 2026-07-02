package com.sinker.app.service;

import com.sinker.app.config.IntegrationProperties;
import com.sinker.app.dto.erp.VendorListRequest;
import com.sinker.app.dto.erp.VendorSyncItem;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class ErpVendorSyncClient {

    private final IntegrationProperties integrationProperties;
    private final RestTemplate integrationRestTemplate;
    private final ErpTokenService erpTokenService;

    public ErpVendorSyncClient(IntegrationProperties integrationProperties,
                               RestTemplate integrationRestTemplate,
                               ErpTokenService erpTokenService) {
        this.integrationProperties = integrationProperties;
        this.integrationRestTemplate = integrationRestTemplate;
        this.erpTokenService = erpTokenService;
    }

    public boolean isConfigured() {
        IntegrationProperties.ErpVendor cfg = integrationProperties.getErpVendor();
        return cfg.isEnabled() && StringUtils.hasText(cfg.getVendorListUrl());
    }

    /** 收到 401 時自動清除 token 快取並重試一次。 */
    public List<VendorSyncItem> fetchPage(VendorListRequest request) {
        IntegrationProperties.ErpVendor cfg = integrationProperties.getErpVendor();
        if (!cfg.isEnabled() || !StringUtils.hasText(cfg.getVendorListUrl())) {
            throw new IllegalStateException("ERP vendor sync not configured");
        }

        try {
            return doPost(cfg.getVendorListUrl(), request);
        } catch (HttpClientErrorException.Unauthorized e) {
            erpTokenService.invalidate();
            return doPost(cfg.getVendorListUrl(), request);
        }
    }

    private List<VendorSyncItem> doPost(String url, VendorListRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(erpTokenService.getToken());

        HttpEntity<VendorListRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<List<VendorSyncItem>> response = integrationRestTemplate.exchange(
                url, HttpMethod.POST, entity, new ParameterizedTypeReference<List<VendorSyncItem>>() {});

        return response.getBody();
    }
}
