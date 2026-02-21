package com.sinker.app.service;

import com.sinker.app.dto.semiproduct.SemiProductDTO;
import com.sinker.app.dto.semiproduct.SemiProductUpdateDTO;
import com.sinker.app.dto.semiproduct.SemiProductUploadResponse;
import com.sinker.app.entity.SemiProductAdvancePurchase;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.SemiProductAdvancePurchaseRepository;
import com.sinker.app.util.SemiProductExcelParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SemiProductService {

    private static final Logger log = LoggerFactory.getLogger(SemiProductService.class);

    private final SemiProductAdvancePurchaseRepository repository;
    private final SemiProductExcelParser excelParser;

    public SemiProductService(SemiProductAdvancePurchaseRepository repository,
                              SemiProductExcelParser excelParser) {
        this.repository = repository;
        this.excelParser = excelParser;
    }

    @Transactional
    public SemiProductUploadResponse upload(MultipartFile file) {
        log.info("Starting semi-product upload, filename={}", file.getOriginalFilename());

        // Parse Excel file
        List<SemiProductExcelParser.SemiProductRow> rows = excelParser.parse(file);
        log.info("Parsed {} rows from Excel", rows.size());

        // Perform TRUNCATE + bulk insert in single transaction
        repository.truncateTable();
        log.info("Truncated semi_product_advance_purchase table");

        List<SemiProductAdvancePurchase> entities = new ArrayList<>();
        for (SemiProductExcelParser.SemiProductRow row : rows) {
            SemiProductAdvancePurchase entity = new SemiProductAdvancePurchase();
            entity.setProductCode(row.getProductCode());
            entity.setProductName(row.getProductName());
            entity.setAdvanceDays(row.getAdvanceDays());
            entities.add(entity);
        }

        repository.saveAll(entities);
        log.info("Inserted {} semi-product records", entities.size());

        return new SemiProductUploadResponse("Upload successful", entities.size());
    }

    public List<SemiProductDTO> findAll() {
        return repository.findAll().stream()
                .map(SemiProductDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public SemiProductDTO update(Integer id, SemiProductUpdateDTO updateDTO) {
        if (updateDTO.getAdvanceDays() == null) {
            throw new IllegalArgumentException("advanceDays is required");
        }
        if (updateDTO.getAdvanceDays() <= 0) {
            throw new IllegalArgumentException("advanceDays must be positive");
        }

        SemiProductAdvancePurchase entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Semi-product not found with id: " + id));

        entity.setAdvanceDays(updateDTO.getAdvanceDays());
        SemiProductAdvancePurchase saved = repository.save(entity);

        log.info("Updated semi-product id={}, advanceDays={}", id, updateDTO.getAdvanceDays());

        return SemiProductDTO.fromEntity(saved);
    }

    public byte[] generateTemplate() {
        return SemiProductTemplateService.generateTemplate();
    }
}
