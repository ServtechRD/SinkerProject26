package com.sinker.app.service;

import com.sinker.app.dto.materialdemand.MaterialDemandDTO;
import com.sinker.app.dto.materialdemand.MaterialDemandUpdateDTO;
import com.sinker.app.entity.MaterialDemand;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.MaterialDemandRepository;
import com.sinker.app.util.MaterialDemandExcelParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MaterialDemandService {

    private static final Logger log = LoggerFactory.getLogger(MaterialDemandService.class);

    private final MaterialDemandRepository materialDemandRepository;
    private final MaterialDemandExcelParser excelParser;
    private final JdbcTemplate jdbcTemplate;

    public MaterialDemandService(MaterialDemandRepository materialDemandRepository,
                                 MaterialDemandExcelParser excelParser,
                                 JdbcTemplate jdbcTemplate) {
        this.materialDemandRepository = materialDemandRepository;
        this.excelParser = excelParser;
        this.jdbcTemplate = jdbcTemplate;
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
        if (dto.getPurchaseQuantity() != null) {
            if (dto.getPurchaseQuantity().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("purchaseQuantity must be >= 0");
            }
            entity.setPurchaseQuantity(dto.getPurchaseQuantity());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        MaterialDemand saved = materialDemandRepository.save(entity);
        markPendingConfirm(saved.getWeekStart(), saved.getFactory());
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
            d.setCurrentStock(row.getCurrentStock());
            d.setExpectedArrivalDate(row.getExpectedArrivalDate());
            d.setExpectedDelivery(row.getExpectedDelivery());
            d.setDemandQuantity(row.getDemandQuantity());
            d.setEstimatedInventory(row.getEstimatedInventory());
            d.setPurchaseQuantity(row.getPurchaseQuantity());
            d.setCreatedAt(now);
            d.setUpdatedAt(now);
            return d;
        }).collect(Collectors.toList());
        materialDemandRepository.saveAll(entities);
        log.info("Saved {} material demand records", entities.size());
        return entities.size();
    }

    /** 編輯儲存後標記該週+廠區有待確認送出 ERP */
    public void markPendingConfirm(LocalDate weekStart, String factory) {
        jdbcTemplate.update(
                "INSERT INTO material_demand_pending_confirm (week_start, factory) VALUES (?, ?) ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP",
                weekStart, factory);
    }

    /** 本週資料確認無誤送出至天心 ERP：清除待確認記錄（dummy API） */
    @Transactional
    public void confirmSendErp(LocalDate weekStart, String factory) {
        jdbcTemplate.update("DELETE FROM material_demand_pending_confirm WHERE week_start = ? AND factory = ?", weekStart, factory);
        log.info("Confirm send ERP: weekStart={}, factory={}", weekStart, factory);
    }

    /** 取得所有待確認送出 ERP 的 (week_start, factory) 清單，供採購主管提示用 */
    public List<Map<String, Object>> getPendingConfirm() {
        return jdbcTemplate.queryForList("SELECT week_start AS weekStart, factory FROM material_demand_pending_confirm ORDER BY updated_at DESC");
    }

    public byte[] generateTemplate(String factory) {
        return MaterialDemandTemplateService.generateTemplate();
    }
}
