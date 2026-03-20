package com.sinker.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinker.app.dto.pdca.PdcaRequest;
import com.sinker.app.dto.pdca.PdcaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;

/**
 * 呼叫外部 PDCA recompute 與 {@link PdcaRecomputeService} 相同之 HTTP 端點，
 * 將回應 JSON 解析為 {@link PdcaResponse} 供 {@link PdcaIntegrationService} 回填 material_demand。
 * 未啟用或未設定 URL 時改為 {@link PdcaLocalStub}。
 */
@Service
public class PdcaApiClientImpl implements PdcaApiClient {

    private static final Logger log = LoggerFactory.getLogger(PdcaApiClientImpl.class);

    private final PdcaExternalHttpClient pdcaExternalHttpClient;
    private final ObjectMapper objectMapper;

    public PdcaApiClientImpl(PdcaExternalHttpClient pdcaExternalHttpClient,
                           ObjectMapper objectMapper) {
        this.pdcaExternalHttpClient = pdcaExternalHttpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public PdcaResponse calculateMaterialRequirements(PdcaRequest request,
                                                      LocalDate weekStart,
                                                      String factory) throws Exception {
        if (!pdcaExternalHttpClient.isConfigured()) {
            log.debug("PDCA HTTP disabled, using local stub: weekStart={}, factory={}", weekStart, factory);
            return PdcaLocalStub.generate(request, weekStart, factory);
        }

        log.info("PDCA HTTP recompute: weekStart={}, factory={}, scheduleItems={}",
                weekStart, factory, request.getSchedule().size());

        String body = pdcaExternalHttpClient.postRecompute(weekStart, factory);
        PdcaResponse response = objectMapper.readValue(body, PdcaResponse.class);
        if (response.getMaterials() == null) {
            response.setMaterials(Collections.emptyList());
        }
        log.info("PDCA HTTP recompute parsed {} materials", response.getMaterials().size());
        return response;
    }
}
