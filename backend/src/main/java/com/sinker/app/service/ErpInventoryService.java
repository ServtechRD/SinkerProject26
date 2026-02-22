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
     * Get inventory balance for a product at the end of a month
     * @param productCode Product code
     * @param month Month in YYYY-MM format
     * @return Inventory balance quantity
     */
    public BigDecimal getInventoryBalance(String productCode, String month) {
        log.debug("ERP stub: getInventoryBalance(productCode={}, month={})", productCode, month);

        try {
            // Stub logic: Return mock data based on product code patterns
            if (productCode == null || productCode.isEmpty()) {
                return BigDecimal.ZERO;
            }

            // Pattern-based mock data
            if (productCode.startsWith("PROD001")) {
                return new BigDecimal("250.00");
            } else if (productCode.startsWith("PROD002")) {
                return new BigDecimal("150.00");
            } else if (productCode.startsWith("PROD")) {
                // Generic product: return a value based on hash
                int hash = Math.abs(productCode.hashCode());
                return new BigDecimal(100 + (hash % 200));
            }

            // Default: return zero for unknown products
            return BigDecimal.ZERO;

        } catch (Exception e) {
            log.error("ERP stub error in getInventoryBalance: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Get sales quantity for a product within a date range
     * @param productCode Product code
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Sales quantity
     */
    public BigDecimal getSalesQuantity(String productCode, String startDate, String endDate) {
        log.debug("ERP stub: getSalesQuantity(productCode={}, startDate={}, endDate={})",
                productCode, startDate, endDate);

        try {
            // Stub logic: Return mock data based on product code patterns
            if (productCode == null || productCode.isEmpty()) {
                return BigDecimal.ZERO;
            }

            // Pattern-based mock data
            if (productCode.startsWith("PROD001")) {
                return new BigDecimal("100.00");
            } else if (productCode.startsWith("PROD002")) {
                return new BigDecimal("75.00");
            } else if (productCode.startsWith("PROD")) {
                // Generic product: return a value based on hash
                int hash = Math.abs(productCode.hashCode());
                return new BigDecimal(50 + (hash % 100));
            }

            // Default: return zero for unknown products
            return BigDecimal.ZERO;

        } catch (Exception e) {
            log.error("ERP stub error in getSalesQuantity: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }
}
