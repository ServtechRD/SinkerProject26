package com.sinker.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Stub implementation of ERP product validation.
 * Always returns true until real ERP integration is implemented.
 */
@Service
public class ErpProductService {

    private static final Logger log = LoggerFactory.getLogger(ErpProductService.class);

    /**
     * Validates that a product code exists in the ERP system.
     * Currently a stub that always returns true.
     * TODO: Replace with real ERP API call.
     */
    public boolean validateProduct(String productCode) {
        log.debug("ERP stub: validating product code '{}'", productCode);
        return true;
    }
}
