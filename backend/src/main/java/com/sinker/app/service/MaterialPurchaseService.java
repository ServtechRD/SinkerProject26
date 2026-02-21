package com.sinker.app.service;

import com.sinker.app.dto.erp.ErpOrderRequest;
import com.sinker.app.dto.erp.ErpOrderResponse;
import com.sinker.app.dto.materialpurchase.MaterialPurchaseDTO;
import com.sinker.app.entity.MaterialPurchase;
import com.sinker.app.exception.AlreadyTriggeredErpException;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.MaterialPurchaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MaterialPurchaseService {

    private static final Logger log = LoggerFactory.getLogger(MaterialPurchaseService.class);

    private final MaterialPurchaseRepository materialPurchaseRepository;
    private final ErpPurchaseService erpPurchaseService;

    public MaterialPurchaseService(MaterialPurchaseRepository materialPurchaseRepository,
                                   ErpPurchaseService erpPurchaseService) {
        this.materialPurchaseRepository = materialPurchaseRepository;
        this.erpPurchaseService = erpPurchaseService;
    }

    @Transactional(readOnly = true)
    public List<MaterialPurchaseDTO> queryMaterialPurchase(LocalDate weekStart, String factory) {
        log.info("Querying material purchase for weekStart: {}, factory: {}", weekStart, factory);

        List<MaterialPurchase> purchases = materialPurchaseRepository
                .findByWeekStartAndFactoryOrderByProductCodeAsc(weekStart, factory);

        return purchases.stream()
                .map(MaterialPurchaseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public MaterialPurchaseDTO triggerErp(Integer id) {
        log.info("Triggering ERP order for material purchase ID: {}", id);

        // Load MaterialPurchase by ID
        MaterialPurchase purchase = materialPurchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Material purchase with ID " + id + " not found"));

        // Check if already triggered
        if (Boolean.TRUE.equals(purchase.getIsErpTriggered())) {
            String message = String.format(
                    "ERP order already triggered for this material purchase. Order number: %s",
                    purchase.getErpOrderNo());
            throw new AlreadyTriggeredErpException(message);
        }

        // Calculate demand date (weekStart + 10 days)
        LocalDate demandDate = purchase.getWeekStart().plusDays(10);

        // Prepare ERP request
        ErpOrderRequest erpRequest = new ErpOrderRequest(
                purchase.getProductCode(),
                purchase.getSemiProductCode(),
                purchase.getQuantity(),
                demandDate
        );

        // Call ERP service
        ErpOrderResponse erpResponse = erpPurchaseService.createOrder(erpRequest);

        // Update entity with ERP response
        purchase.setIsErpTriggered(true);
        purchase.setErpOrderNo(erpResponse.getOrderNo());
        purchase.setUpdatedAt(LocalDateTime.now());

        // Save and return
        MaterialPurchase savedPurchase = materialPurchaseRepository.save(purchase);

        log.info("ERP order triggered successfully - ID: {}, OrderNo: {}",
                id, savedPurchase.getErpOrderNo());

        return MaterialPurchaseDTO.fromEntity(savedPurchase);
    }
}
