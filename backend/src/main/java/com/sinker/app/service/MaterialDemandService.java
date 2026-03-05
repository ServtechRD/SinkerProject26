package com.sinker.app.service;

import com.sinker.app.dto.materialdemand.MaterialDemandDTO;
import com.sinker.app.dto.materialdemand.MaterialDemandUpdateDTO;
import com.sinker.app.entity.MaterialDemand;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.MaterialDemandRepository;
import com.sinker.app.util.MaterialDemandExcelParser;
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
public class MaterialDemandService {

    private static final Logger log = LoggerFactory.getLogger(MaterialDemandService.class);

    private final MaterialDemandRepository materialDemandRepository;
    private final MaterialDemandExcelParser excelParser;

    public MaterialDemandService(MaterialDemandRepository materialDemandRepository,
                                 MaterialDemandExcelParser excelParser) {
        this.materialDemandRepository = materialDemandRepository;
        this.excelParser = excelParser;
    }

    @Transactional(readOnly = true)
    public List<MaterialDemandDTO> queryMaterialDemand(LocalDate weekStart, String factory) {
        log.info("Querying material demand for weekStart: {}, factory: {}", weekStart, factory);

        List<MaterialDemand> demands = materialDemandRepository
                .findByWeekStartAndFactoryOrderByMaterialCodeAsc(weekStart, factory);

        return demands.stream()
                .map(MaterialDemandDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public MaterialDemandDTO update(Integer id, MaterialDemandUpdateDTO dto) {
        MaterialDemand entity = materialDemandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material demand not found with id: " + id));
        if (dto.getExpectedDelivery() != null) {
            if (dto.getExpectedDelivery().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("expectedDelivery must be >= 0");
            }
            entity.setExpectedDelivery(dto.getExpectedDelivery());
        }
        if (dto.getDemandQuantity() != null) {
            if (dto.getDemandQuantity().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("demandQuantity must be >= 0");
            }
            entity.setDemandQuantity(dto.getDemandQuantity());
        }
        if (dto.getEstimatedInventory() != null) {
            if (dto.getEstimatedInventory().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("estimatedInventory must be >= 0");
            }
            entity.setEstimatedInventory(dto.getEstimatedInventory());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        MaterialDemand saved = materialDemandRepository.save(entity);
        log.info("Updated material demand id={}", id);
        return MaterialDemandDTO.fromEntity(saved);
    }

    @Transactional
    public int upload(MultipartFile file, LocalDate weekStart, String factory) {
        log.info("Upload material demand: weekStart={}, factory={}", weekStart, factory);
        if (factory == null || factory.trim().isEmpty()) {
            throw new IllegalArgumentException("factory is required");
        }
        List<MaterialDemandExcelParser.MaterialDemandRow> rows = excelParser.parse(file);
        materialDemandRepository.deleteByWeekStartAndFactory(weekStart, factory);
        LocalDateTime now = LocalDateTime.now();
        List<MaterialDemand> entities = rows.stream().map(row -> {
            MaterialDemand d = new MaterialDemand();
            d.setWeekStart(weekStart);
            d.setFactory(factory);
            d.setMaterialCode(row.getMaterialCode());
            d.setMaterialName(row.getMaterialName());
            d.setUnit(row.getUnit());
            d.setLastPurchaseDate(row.getLastPurchaseDate());
            d.setDemandDate(row.getDemandDate());
            d.setExpectedDelivery(row.getExpectedDelivery());
            d.setDemandQuantity(row.getDemandQuantity());
            d.setEstimatedInventory(row.getEstimatedInventory());
            d.setCreatedAt(now);
            d.setUpdatedAt(now);
            return d;
        }).collect(Collectors.toList());
        materialDemandRepository.saveAll(entities);
        log.info("Saved {} material demand records", entities.size());
        return entities.size();
    }

    public byte[] generateTemplate(String factory) {
        return MaterialDemandTemplateService.generateTemplate();
    }
}
