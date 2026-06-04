package com.sinker.app.service;

import com.sinker.app.config.IntegrationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ErpTokenService {

    private static final Logger log = LoggerFactory.getLogger(ErpTokenService.class);

    private final AtomicReference<String> cachedToken = new AtomicReference<>();
    private final RestTemplate integrationRestTemplate;
    private final IntegrationProperties integrationProperties;

    public ErpTokenService(RestTemplate integrationRestTemplate,
                           IntegrationProperties integrationProperties) {
        this.integrationRestTemplate = integrationRestTemplate;
        this.integrationProperties = integrationProperties;
    }

    public String getToken() {
        String token = cachedToken.get();
        if (token != null) {
            return token;
        }
        return fetchToken();
    }

    public void invalidate() {
        cachedToken.set(null);
        log.info("ERP token cache invalidated");
    }

    private synchronized String fetchToken() {
        String token = cachedToken.get();
        if (token != null) {
            return token;
        }

        IntegrationProperties.Auth cfg = integrationProperties.getAuth();
        if (!StringUtils.hasText(cfg.getTokenUrl())) {
            throw new IllegalStateException("app.integrations.auth.token-url is not configured");
        }

        Map<String, String> body = Map.of(
                "Account", cfg.getAccount(),
                "Salasana", cfg.getSalasana()
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = integrationRestTemplate.exchange(
                cfg.getTokenUrl(),
                HttpMethod.POST,
                entity,
                String.class
        );

        String newToken = response.getBody();
        if (!StringUtils.hasText(newToken)) {
            throw new IllegalStateException("ERP token response is empty");
        }
        // 若外部回傳 JSON 字串（含引號）則去除
        newToken = newToken.trim().replaceAll("^\"|\"$", "");

        cachedToken.set(newToken);
        log.info("ERP token fetched successfully");
        return newToken;
    }
}
