package com.sinker.app.service;

import com.sinker.app.dto.pdca.PdcaRequest;
import com.sinker.app.dto.pdca.PdcaResponse;

import java.time.LocalDate;

/**
 * Interface for PDCA API client.
 * 實作為呼叫與 recompute 相同之外部 HTTP，解析 JSON 為物料需求；未啟用時可改走本機 stub。
 */
public interface PdcaApiClient {

    /**
     * 依週排程觸發 PDCA（外部為 POST week_start + factory；回應含 materials 陣列）。
     *
     * @param request       由週排程組出之排程明細（本機 stub 仍會使用）
     * @param weekStart     週一日期
     * @param factory       廠區
     * @return 物料需求
     * @throws Exception 若呼叫失敗
     */
    PdcaResponse calculateMaterialRequirements(PdcaRequest request, LocalDate weekStart, String factory)
            throws Exception;
}
