package com.sinker.app.service;

import com.sinker.app.dto.materialpurchase.MaterialPurchaseDTO;
import com.sinker.app.entity.MaterialPurchase;
import com.sinker.app.repository.MaterialPurchaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MaterialPurchaseService {

    private static final Logger log = LoggerFactory.getLogger(MaterialPurchaseService.class);

    private final MaterialPurchaseRepository materialPurchaseRepository;

    public MaterialPurchaseService(MaterialPurchaseRepository materialPurchaseRepository) {
        this.materialPurchaseRepository = materialPurchaseRepository;
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
}
