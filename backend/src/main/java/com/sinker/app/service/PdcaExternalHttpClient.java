package com.sinker.app.service;

import com.sinker.app.config.IntegrationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;

/**
 * 呼叫設定檔中的 PDCA recompute URL（POST JSON：week_start、factory），與 {@link PdcaRecomputeService} 共用。
 */
@Component
public class PdcaExternalHttpClient {

    private final IntegrationProperties integrationProperties;
    private final RestTemplate integrationRestTemplate;

    public PdcaExternalHttpClient(IntegrationProperties integrationProperties,
                                  RestTemplate integrationRestTemplate) {
        this.integrationProperties = integrationProperties;
        this.integrationRestTemplate = integrationRestTemplate;
    }

    public boolean isConfigured() {
        IntegrationProperties.Pdca cfg = integrationProperties.getPdca();
        return cfg.isEnabled() && StringUtils.hasText(cfg.getRecomputeUrl());
    }

    /**
     * 呼叫外部 PDCA recompute，回傳 response body（若無 body 則回傳 "{}"）。
     *
     * @throws IllegalStateException 未啟用或未設定 URL
     * @throws RestClientException   HTTP 失敗
     */
    public String postRecompute(LocalDate weekStart, String factory) {
        IntegrationProperties.Pdca cfg = integrationProperties.getPdca();
        if (!cfg.isEnabled() || !StringUtils.hasText(cfg.getRecomputeUrl())) {
            throw new IllegalStateException("PDCA HTTP not configured");
        }
        if (!StringUtils.hasText(factory)) {
            throw new IllegalArgumentException("factory is required");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(cfg.getUsername())) {
            headers.setBasicAuth(cfg.getUsername(), cfg.getPassword() != null ? cfg.getPassword() : "");
        }

        Map<String, Object> body = Map.of(
                "week_start", weekStart.toString(),
                "factory", factory
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = integrationRestTemplate.exchange(
                cfg.getRecomputeUrl(),
                HttpMethod.POST,
                entity,
                String.class
        );

        String respBody = response.getBody();
        return respBody != null ? respBody : "{}";
    }
}
