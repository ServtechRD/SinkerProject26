package com.sinker.app.service;

import com.sinker.app.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Validates product codes against the system product master (product table).
 * Used by sales forecast and gift forecast upload to ensure 品號 exists before upload.
 */
@Service
public class ErpProductService {

    private static final Logger log = LoggerFactory.getLogger(ErpProductService.class);

    private final ProductRepository productRepository;

    public ErpProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Validates that a product code exists in the system (product table).
     * Returns false if code is null, blank, or not found.
     */
    public boolean validateProduct(String productCode) {
        if (!StringUtils.hasText(productCode)) {
            return false;
        }
        boolean exists = productRepository.findByCode(productCode.trim()).isPresent();
        if (!exists) {
            log.debug("Product code not found in system: '{}'", productCode);
        }
        return exists;
    }
}
