package com.sinker.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * ERP Inventory Service Stub
 * Provides mock data for inventory balance and sales quantity
 * In production, this would connect to actual ERP system
 */
@Service
public class ErpInventoryService {

    private static final Logger log = LoggerFactory.getLogger(ErpInventoryService.class);

    /**
     * Get inventory balance for a product at the end of a month.
     * Placeholder: returns 100 until ERP API is integrated.
     */
    public BigDecimal getInventoryBalance(String productCode, String month) {
        log.debug("ERP stub: getInventoryBalance(productCode={}, month={})", productCode, month);
        if (productCode == null || productCode.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal("100");
    }

    /**
     * Get sales quantity for a product within a date range.
     * Placeholder: returns 50 until ERP API is integrated.
     */
    public BigDecimal getSalesQuantity(String productCode, String startDate, String endDate) {
        log.debug("ERP stub: getSalesQuantity(productCode={}, startDate={}, endDate={})",
                productCode, startDate, endDate);
        if (productCode == null || productCode.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal("50");
    }
}
