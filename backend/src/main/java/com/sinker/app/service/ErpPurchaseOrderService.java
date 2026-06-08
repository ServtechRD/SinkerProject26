package com.sinker.app.service;

import com.sinker.app.config.IntegrationProperties;
import com.sinker.app.dto.erp.ErpCreatePurchaseOrderRequest;
import com.sinker.app.entity.MaterialDemand;
import com.sinker.app.exception.ExternalApiException;
import com.sinker.app.repository.MaterialDemandRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ErpPurchaseOrderService {

    private static final Logger log = LoggerFactory.getLogger(ErpPurchaseOrderService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AtomicInteger dailySequence = new AtomicInteger(1);
    private volatile String lastDate = "";

    private final IntegrationProperties integrationProperties;
    private final RestTemplate integrationRestTemplate;
    private final ErpTokenService erpTokenService;
    private final MaterialDemandRepository materialDemandRepository;

    public ErpPurchaseOrderService(IntegrationProperties integrationProperties,
                                   RestTemplate integrationRestTemplate,
                                   ErpTokenService erpTokenService,
                                   MaterialDemandRepository materialDemandRepository) {
        this.integrationProperties = integrationProperties;
        this.integrationRestTemplate = integrationRestTemplate;
        this.erpTokenService = erpTokenService;
        this.materialDemandRepository = materialDemandRepository;
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

        List<MaterialDemand> demands = materialDemandRepository
                .findByWeekStartAndFactoryOrderByMaterialCodeAsc(weekStart, factory)
                .stream()
                .filter(d -> d.getPurchaseQuantity() != null
                        && d.getPurchaseQuantity().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        if (demands.isEmpty()) {
            log.info("ERP purchase order skipped: no demands with purchase quantity > 0: weekStart={}, factory={}",
                    weekStart, factory);
            return;
        }

        String webNo = generateWebNo();

        LocalDate estDd = demands.stream()
                .map(MaterialDemand::getDemandDate)
                .min(LocalDate::compareTo)
                .orElse(weekStart);

        List<ErpCreatePurchaseOrderRequest.Detail> details = new ArrayList<>();
        for (int i = 0; i < demands.size(); i++) {
            MaterialDemand d = demands.get(i);
            details.add(new ErpCreatePurchaseOrderRequest.Detail(
                    i + 1,
                    d.getMaterialCode(),
                    d.getMaterialName(),
                    d.getFactory(),
                    d.getUnit(),
                    d.getPurchaseQuantity(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    ""
            ));
        }

        ErpCreatePurchaseOrderRequest body = new ErpCreatePurchaseOrderRequest(
                webNo,
                "0000",
                "",
                LocalDate.now().toString(),
                estDd.toString(),
                "",
                1,
                details
        );

        log.info("ERP createPurchaseOrder: webNo={}, factory={}, detailCount={}", webNo, factory, details.size());

        try {
            doPost(cfg.getPurchaseOrderUrl(), body, weekStart, factory, webNo);
        } catch (HttpClientErrorException.Unauthorized e) {
            erpTokenService.invalidate();
            doPost(cfg.getPurchaseOrderUrl(), body, weekStart, factory, webNo);
        } catch (RestClientException e) {
            log.error("ERP purchase order HTTP error: weekStart={}, factory={}", weekStart, factory, e);
            throw new ExternalApiException("ERP purchase order failed: " + e.getMessage(), e);
        }
    }

    private synchronized String generateWebNo() {
        String today = LocalDate.now().format(DATE_FMT);
        if (!today.equals(lastDate)) {
            lastDate = today;
            dailySequence.set(1);
        }
        return String.format("PO%s%04d", today, dailySequence.getAndIncrement());
    }

    private void doPost(String url, ErpCreatePurchaseOrderRequest body,
                        LocalDate weekStart, String factory, String webNo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(erpTokenService.getToken());

        HttpEntity<ErpCreatePurchaseOrderRequest> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = integrationRestTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("ERP purchase order OK: weekStart={}, factory={}, webNo={}, status={}",
                        weekStart, factory, webNo, response.getStatusCode());
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
