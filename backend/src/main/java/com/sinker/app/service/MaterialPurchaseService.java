package com.sinker.app.service;

import com.sinker.app.dto.erp.ErpOrderRequest;
import com.sinker.app.dto.erp.ErpOrderResponse;
import com.sinker.app.dto.materialpurchase.MaterialPurchaseDTO;
import com.sinker.app.dto.materialpurchase.MaterialPurchaseUpdateDTO;
import com.sinker.app.entity.MaterialPurchase;
import com.sinker.app.exception.AlreadyTriggeredErpException;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.MaterialPurchaseRepository;
import com.sinker.app.util.MaterialPurchaseExcelParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MaterialPurchaseService {

    private static final Logger log = LoggerFactory.getLogger(MaterialPurchaseService.class);

    private final MaterialPurchaseRepository materialPurchaseRepository;
    private final ErpPurchaseService erpPurchaseService;
    private final MaterialPurchaseExcelParser excelParser;

    public MaterialPurchaseService(MaterialPurchaseRepository materialPurchaseRepository,
                                   ErpPurchaseService erpPurchaseService,
                                   MaterialPurchaseExcelParser excelParser) {
        this.materialPurchaseRepository = materialPurchaseRepository;
        this.erpPurchaseService = erpPurchaseService;
        this.excelParser = excelParser;
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

    @Transactional
    public MaterialPurchaseDTO update(Integer id, MaterialPurchaseUpdateDTO dto) {
        MaterialPurchase entity = materialPurchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material purchase not found with id: " + id));
        if (dto.getKgPerBox() != null) {
            if (dto.getKgPerBox().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("公斤/箱 must be >= 0");
            }
            entity.setKgPerBox(dto.getKgPerBox());
        }
        if (dto.getBasketQuantity() != null) {
            if (dto.getBasketQuantity().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("籃數 must be >= 0");
            }
            entity.setBasketQuantity(dto.getBasketQuantity());
        }
        if (dto.getBoxesPerBarrel() != null) {
            if (dto.getBoxesPerBarrel().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("箱/桶 must be >= 0");
            }
            entity.setBoxesPerBarrel(dto.getBoxesPerBarrel());
        }
        if (dto.getRequiredBarrels() != null) {
            if (dto.getRequiredBarrels().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("所需桶數 must be >= 0");
            }
            entity.setRequiredBarrels(dto.getRequiredBarrels());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        MaterialPurchase saved = materialPurchaseRepository.save(entity);
        log.info("Updated material purchase id={}", id);
        return MaterialPurchaseDTO.fromEntity(saved);
    }

    @Transactional
    public int upload(MultipartFile file, LocalDate weekStart, String factory) {
        log.info("Upload material purchase: weekStart={}, factory={}", weekStart, factory);
        if (factory == null || factory.trim().isEmpty()) {
            throw new IllegalArgumentException("factory is required");
        }
        List<MaterialPurchaseExcelParser.MaterialPurchaseRow> rows = excelParser.parse(file);
        materialPurchaseRepository.deleteByWeekStartAndFactory(weekStart, factory);
        LocalDateTime now = LocalDateTime.now();
        List<MaterialPurchase> entities = rows.stream().map(row -> {
            MaterialPurchase p = new MaterialPurchase();
            p.setWeekStart(weekStart);
            p.setFactory(factory);
            p.setProductCode(row.getProductCode());
            p.setProductName(row.getProductName());
            p.setQuantity(row.getQuantity());
            p.setSemiProductName(row.getSemiProductName());
            p.setSemiProductCode(row.getSemiProductCode());
            p.setKgPerBox(row.getKgPerBox());
            p.setBasketQuantity(row.getBasketQuantity());
            p.setBoxesPerBarrel(row.getBoxesPerBarrel());
            p.setRequiredBarrels(row.getRequiredBarrels());
            p.setIsErpTriggered(false);
            p.setErpOrderNo(null);
            p.setCreatedAt(now);
            p.setUpdatedAt(now);
            return p;
        }).collect(Collectors.toList());
        materialPurchaseRepository.saveAll(entities);
        log.info("Saved {} material purchase records", entities.size());
        return entities.size();
    }

    public byte[] generateTemplate(String factory) {
        return MaterialPurchaseTemplateService.generateTemplate();
    }
}
