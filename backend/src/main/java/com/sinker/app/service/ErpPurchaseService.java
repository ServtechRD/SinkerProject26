package com.sinker.app.service;

import com.sinker.app.dto.erp.ErpOrderRequest;
import com.sinker.app.dto.erp.ErpOrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ERP Purchase Service Stub
 * This is a temporary stub implementation for ERP API integration.
 * Replace with actual ERP API client when ERP system details are available.
 */
@Service
public class ErpPurchaseService {

    private static final Logger log = LoggerFactory.getLogger(ErpPurchaseService.class);
    private static final AtomicInteger orderSequence = new AtomicInteger(1);

    /**
     * Stub method to create an ERP purchase order
     * @param request ERP order request containing item details
     * @return ERP order response with generated order number
     * @throws RuntimeException if ERP service fails (simulated)
     */
    public ErpOrderResponse createOrder(ErpOrderRequest request) {
        log.info("ERP API call - createOrder: itm={}, prdNo={}, qty={}, demandDate={}",
                request.getItm(), request.getPrdNo(), request.getQty(), request.getDemandDate());

        // Simulate network delay (200-500ms)
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("ERP service interrupted", e);
        }

        // Generate mock order number: ERP-YYYY-NNNN
        String year = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String orderNo = String.format("ERP-%s-%04d", year, orderSequence.getAndIncrement());

        ErpOrderResponse response = new ErpOrderResponse(orderNo, "SUCCESS");

        log.info("ERP API response - orderNo={}, status={}", response.getOrderNo(), response.getStatus());

        return response;
    }
}
