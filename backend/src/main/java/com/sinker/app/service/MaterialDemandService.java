package com.sinker.app.service;

import com.sinker.app.dto.materialdemand.MaterialDemandDTO;
import com.sinker.app.dto.materialdemand.MaterialDemandPendingConfirmItemDTO;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MaterialDemandService {

    private static final Logger log = LoggerFactory.getLogger(MaterialDemandService.class);

    private final MaterialDemandRepository materialDemandRepository;
    private final MaterialDemandExcelParser excelParser;
    private final JdbcTemplate jdbcTemplate;
    private final PdcaRecomputeService pdcaRecomputeService;
    private final ErpPurchaseOrderService erpPurchaseOrderService;
    private final PdcaIntegrationService pdcaIntegrationService;

    public MaterialDemandService(MaterialDemandRepository materialDemandRepository,
                                 MaterialDemandExcelParser excelParser,
                                 JdbcTemplate jdbcTemplate,
                                 PdcaRecomputeService pdcaRecomputeService,
                                 ErpPurchaseOrderService erpPurchaseOrderService,
                                 PdcaIntegrationService pdcaIntegrationService) {
        this.materialDemandRepository = materialDemandRepository;
        this.excelParser = excelParser;
        this.jdbcTemplate = jdbcTemplate;
        this.pdcaRecomputeService = pdcaRecomputeService;
        this.erpPurchaseOrderService = erpPurchaseOrderService;
        this.pdcaIntegrationService = pdcaIntegrationService;
    }

    /**
     * 查詢：呼叫 PDCA（外部 URL 或 URL 未設定時固定假資料），並覆寫 DB 後回傳。
     */
    @Transactional
    public List<MaterialDemandDTO> queryMaterialDemand(LocalDate weekStart, String factory) {
        log.info("Querying material demand via PDCA: weekStart={}, factory={}", weekStart, factory);
        return pdcaIntegrationService.syncMaterialDemandFromPdca(weekStart, factory);
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
        pdcaRecomputeService.recomputeAsync(saved.getWeekStart(), saved.getFactory());
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
        pdcaRecomputeService.recomputeAsync(weekStart, factory);
        return entities.size();
    }

    /** 編輯儲存後標記該週+廠區有待確認送出 ERP */
    public void markPendingConfirm(LocalDate weekStart, String factory) {
        jdbcTemplate.update(
                "INSERT INTO material_demand_pending_confirm (week_start, factory) VALUES (?, ?) ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP",
                weekStart, factory);
    }

    /** 本週資料確認無誤送出至天心 ERP：先呼叫外部 ERP 採購單建立 API，再清除待確認記錄 */
    @Transactional
    public void confirmSendErp(LocalDate weekStart, String factory) {
        erpPurchaseOrderService.createPurchaseOrder(weekStart, factory);
        jdbcTemplate.update("DELETE FROM material_demand_pending_confirm WHERE week_start = ? AND factory = ?", weekStart, factory);
        log.info("Confirm send ERP: weekStart={}, factory={}", weekStart, factory);
    }

    /** 取得所有待確認送出 ERP 的 (week_start, factory) 清單，供採購主管提示用（含最後編輯儲存時間） */
    public List<MaterialDemandPendingConfirmItemDTO> getPendingConfirm() {
        return jdbcTemplate.query(
                "SELECT week_start, factory, updated_at FROM material_demand_pending_confirm ORDER BY updated_at DESC",
                (rs, rowNum) -> {
                    MaterialDemandPendingConfirmItemDTO dto = new MaterialDemandPendingConfirmItemDTO();
                    dto.setWeekStart(rs.getObject("week_start", LocalDate.class));
                    dto.setFactory(rs.getString("factory"));
                    java.sql.Timestamp ts = rs.getTimestamp("updated_at");
                    dto.setUpdatedAt(ts != null ? ts.toLocalDateTime() : null);
                    return dto;
                });
    }

    /**
     * 該週+廠區若有「編輯儲存後待確認 ERP」紀錄，回傳其 updated_at（即最後一次觸發編輯儲存時間）。
     */
    public Optional<LocalDateTime> getLastEditSavedAt(LocalDate weekStart, String factory) {
        List<LocalDateTime> rows = jdbcTemplate.query(
                "SELECT updated_at FROM material_demand_pending_confirm WHERE week_start = ? AND factory = ?",
                (rs, rowNum) -> {
                    java.sql.Timestamp ts = rs.getTimestamp(1);
                    return ts != null ? ts.toLocalDateTime() : null;
                },
                weekStart,
                factory);
        if (rows.isEmpty() || rows.get(0) == null) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public byte[] generateTemplate(String factory) {
        return MaterialDemandTemplateService.generateTemplate();
    }
}
