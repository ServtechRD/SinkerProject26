package com.sinker.app.service;

import com.sinker.app.config.IntegrationProperties;
import com.sinker.app.dto.pdca.PdcaRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 呼叫設定檔中的 PDCA recompute URL（POST JSON：PnDd、Wh），與 {@link PdcaRecomputeService} 共用。
 */
@Component
public class PdcaExternalHttpClient {

    private static final Logger log = LoggerFactory.getLogger(PdcaExternalHttpClient.class);

    private final IntegrationProperties integrationProperties;
    private final RestTemplate integrationRestTemplate;
    private final ErpTokenService erpTokenService;

    public PdcaExternalHttpClient(IntegrationProperties integrationProperties,
                                  RestTemplate integrationRestTemplate,
                                  ErpTokenService erpTokenService) {
        this.integrationProperties = integrationProperties;
        this.integrationRestTemplate = integrationRestTemplate;
        this.erpTokenService = erpTokenService;
    }

    private static final Map<String, String> FACTORY_CODE_MAP = Map.of(
            "一廠", "05",
            "二廠", "02",
            "三廠", "03"
    );

    public boolean isConfigured() {
        IntegrationProperties.Pdca cfg = integrationProperties.getPdca();
        return cfg.isEnabled() && StringUtils.hasText(cfg.getRecomputeUrl());
    }

    /**
     * 呼叫外部 PDCA recompute，回傳 response body（若無 body 則回傳 "{}"）。
     * 收到 401 時自動清除 token 快取並重試一次。
     *
     * @throws IllegalStateException    未啟用或未設定 URL
     * @throws IllegalArgumentException factory 無對應代號
     * @throws RestClientException      HTTP 失敗
     */
    public String postRecompute(LocalDate weekStart, String factory) {
        return postRecompute(weekStart, factory, null);
    }

    /**
     * 同上，額外帶上週排程明細（品號 PrdNo、數量 Qty），供 PDCA 依實際上傳/編輯內容重算。
     */
    public String postRecompute(LocalDate weekStart, String factory, List<PdcaRequest.ScheduleItem> schedule) {
        IntegrationProperties.Pdca cfg = integrationProperties.getPdca();
        if (!cfg.isEnabled() || !StringUtils.hasText(cfg.getRecomputeUrl())) {
            throw new IllegalStateException("PDCA HTTP not configured");
        }
        if (!StringUtils.hasText(factory)) {
            throw new IllegalArgumentException("factory is required");
        }

        String wh = FACTORY_CODE_MAP.get(factory);
        if (wh == null) {
            throw new IllegalArgumentException("未知廠別: " + factory);
        }

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("PnDd", weekStart.toString());
        body.put("Wh", wh);
        if (!CollectionUtils.isEmpty(schedule)) {
            body.put("Details", schedule.stream()
                    .map(item -> Map.of(
                            "PrdNo", item.getProductCode(),
                            "Qty", item.getQuantity()
                    ))
                    .collect(Collectors.toList()));
        }

        log.info("呼叫外部 PDCA API: url={}, body={}", cfg.getRecomputeUrl(), body);

        try {
            return doPost(cfg.getRecomputeUrl(), body);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("PDCA API 回傳 401，清除 token 後重試: url={}, body={}", cfg.getRecomputeUrl(), body);
            erpTokenService.invalidate();
            return doPost(cfg.getRecomputeUrl(), body);
        }
    }

    private String doPost(String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(erpTokenService.getToken());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = integrationRestTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

        String respBody = response.getBody();
        return respBody != null ? respBody : "{}";
    }
}
