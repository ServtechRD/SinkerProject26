package com.sinker.app.service;

import com.sinker.app.dto.pdca.PdcaRequest;
import com.sinker.app.dto.pdca.PdcaResponse;

/**
 * Interface for PDCA API client.
 * PDCA system calculates material requirements based on production schedules,
 * cross-referencing BOM and ERP inventory data.
 */
public interface PdcaApiClient {

    /**
     * Calls PDCA API to calculate material requirements from production schedule.
     *
     * @param request Production schedule data
     * @return Material requirements calculated by PDCA
     * @throws Exception if PDCA API call fails
     */
    PdcaResponse calculateMaterialRequirements(PdcaRequest request) throws Exception;
}
