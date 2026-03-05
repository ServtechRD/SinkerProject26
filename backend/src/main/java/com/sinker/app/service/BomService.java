package com.sinker.app.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * BOM (Bill of Materials) Service Stub
 *
 * This is a temporary stub service that returns hardcoded BOM data.
 * It will be replaced with actual BOM data integration in the future.
 */
@Service
public class BomService {

    /**
     * Get kg per box for a product.
     * Returns hardcoded values for known products, default 1.00 for unknown products.
     *
     * @param productCode the product code
     * @return kg per box
     */
    public BigDecimal getKgPerBox(String productCode) {
        return switch (productCode) {
            case "P001" -> new BigDecimal("5.50");
            case "P002" -> new BigDecimal("3.00");
            case "P003" -> new BigDecimal("4.20");
            default -> new BigDecimal("1.00");
        };
    }

    /**
     * Get boxes per barrel for a product.
     * Returns hardcoded values for known products, default 10.00 for unknown products.
     *
     * @param productCode the product code
     * @return boxes per barrel
     */
    public BigDecimal getBoxesPerBarrel(String productCode) {
        return switch (productCode) {
            case "P001" -> new BigDecimal("20.00");
            case "P002" -> new BigDecimal("15.00");
            case "P003" -> new BigDecimal("25.00");
            default -> new BigDecimal("10.00");
        };
    }
}
