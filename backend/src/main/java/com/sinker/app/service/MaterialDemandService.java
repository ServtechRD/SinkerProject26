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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MaterialDemandService {

    private static final Logger log = LoggerFactory.getLogger(MaterialDemandService.class);

    /** 待審核：主管可操作【審核】【退回】 */
    public static final int REVIEW_STATUS_PENDING = 0;
    /** 審核完成：可【送出至天心ERP】 */
    public static final int REVIEW_STATUS_APPROVED = 1;
    /** 退回：僅可編輯與匯出 */
    public static final int REVIEW_STATUS_REJECTED = 2;

    private final MaterialDemandRepository materialDemandRepository;
    private final MaterialDemandExcelParser excelParser;
    private final JdbcTemplate jdbcTemplate;
    private final PdcaRecomputeService pdcaRecomputeService;
    private final ErpPurchaseOrderService erpPurchaseOrderService;

    public MaterialDemandService(MaterialDemandRepository materialDemandRepository,
                                 MaterialDemandExcelParser excelParser,
                                 JdbcTemplate jdbcTemplate,
                                 PdcaRecomputeService pdcaRecomputeService,
                                 ErpPurchaseOrderService erpPurchaseOrderService) {
        this.materialDemandRepository = materialDemandRepository;
        this.excelParser = excelParser;
        this.jdbcTemplate = jdbcTemplate;
        this.pdcaRecomputeService = pdcaRecomputeService;
        this.erpPurchaseOrderService = erpPurchaseOrderService;
    }

    @Transactional(readOnly = true)
    public List<MaterialDemandDTO> queryMaterialDemand(LocalDate weekStart, String factory) {
        log.info("Querying material demand from DB: weekStart={}, factory={}", weekStart, factory);
        LocalDate endDate = weekStart.plusDays(6);
        return materialDemandRepository
                .findByFactoryAndDemandDateBetweenOrderByMaterialCodeAsc(factory, weekStart, endDate)
                .stream()
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
        markPendingConfirm(weekStart, factory);
        pdcaRecomputeService.recomputeAsync(weekStart, factory);
        return entities.size();
    }

    /**
     * 編輯儲存或 Excel 上傳後：標記該週+廠區為待審核（重新建立採購單流程归零）。
     */
    public void markPendingConfirm(LocalDate weekStart, String factory) {
        jdbcTemplate.update(
                "INSERT INTO material_demand_pending_confirm (week_start, factory, status) VALUES (?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP, status = ?",
                weekStart, factory, REVIEW_STATUS_PENDING, REVIEW_STATUS_PENDING);
    }

    /** 主管審核通過：待審核 → 審核完成（可送 ERP） */
    @Transactional
    public void reviewApprove(LocalDate weekStart, String factory) {
        int n = jdbcTemplate.update(
                "UPDATE material_demand_pending_confirm SET status = ?, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE week_start = ? AND factory = ? AND status = ?",
                REVIEW_STATUS_APPROVED, weekStart, factory, REVIEW_STATUS_PENDING);
        if (n == 0) {
            throw new IllegalArgumentException("無待審核紀錄或狀態已變更");
        }
        log.info("Review approve: weekStart={}, factory={}", weekStart, factory);
    }

    /** 主管退回：待審核 → 退回 */
    @Transactional
    public void reviewReject(LocalDate weekStart, String factory) {
        int n = jdbcTemplate.update(
                "UPDATE material_demand_pending_confirm SET status = ?, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE week_start = ? AND factory = ? AND status = ?",
                REVIEW_STATUS_REJECTED, weekStart, factory, REVIEW_STATUS_PENDING);
        if (n == 0) {
            throw new IllegalArgumentException("無待審核紀錄或狀態已變更");
        }
        log.info("Review reject: weekStart={}, factory={}", weekStart, factory);
    }

    /**
     * 僅審核完成（status=1）可送出；先呼叫 ERP 採購單建立 API，再刪除待確認列。
     *
     * @param vendorCode 廠商代碼，未指定時由 {@link ErpPurchaseOrderService} 使用預設值
     */
    @Transactional
    public void confirmSendErp(LocalDate weekStart, String factory, String vendorCode) {
        Integer st = getPendingStatus(weekStart, factory);
        if (st == null || st != REVIEW_STATUS_APPROVED) {
            throw new IllegalArgumentException("僅審核完成後可送出至天心ERP");
        }
        erpPurchaseOrderService.createPurchaseOrder(weekStart, factory, vendorCode);
        jdbcTemplate.update("DELETE FROM material_demand_pending_confirm WHERE week_start = ? AND factory = ?", weekStart, factory);
        log.info("Confirm send ERP: weekStart={}, factory={}", weekStart, factory);
    }

    /**
     * 依登入角色回傳待確認清單：admin／採購主管僅 status=0（待審核）；採購僅 status=1（審核完成）與 2（退回）。
     */
    public List<MaterialDemandPendingConfirmItemDTO> getPendingConfirmForRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return List.of();
        }
        if ("admin".equals(roleCode) || "procurement_supervisor".equals(roleCode)) {
            return jdbcTemplate.query(
                    "SELECT week_start, factory, status, updated_at FROM material_demand_pending_confirm "
                            + "WHERE status = ? ORDER BY updated_at DESC",
                    (rs, rowNum) -> mapPendingConfirmRow(rs),
                    REVIEW_STATUS_PENDING);
        }
        if ("procurement".equals(roleCode)) {
            return jdbcTemplate.query(
                    "SELECT week_start, factory, status, updated_at FROM material_demand_pending_confirm "
                            + "WHERE status IN (?, ?) ORDER BY updated_at DESC",
                    (rs, rowNum) -> mapPendingConfirmRow(rs),
                    REVIEW_STATUS_APPROVED,
                    REVIEW_STATUS_REJECTED);
        }
        return List.of();
    }

    /**
     * 該週+廠區之一筆待確認列（與 pending-confirm 清單項目相同 DTO）；無列則各欄為 null。
     */
    public MaterialDemandPendingConfirmItemDTO getPendingConfirmItem(LocalDate weekStart, String factory) {
        List<MaterialDemandPendingConfirmItemDTO> rows = jdbcTemplate.query(
                "SELECT week_start, factory, status, updated_at FROM material_demand_pending_confirm "
                        + "WHERE week_start = ? AND factory = ?",
                (rs, rowNum) -> mapPendingConfirmRow(rs),
                weekStart,
                factory);
        if (rows.isEmpty()) {
            return new MaterialDemandPendingConfirmItemDTO();
        }
        return rows.get(0);
    }

    /**
     * 該週+廠區若有「編輯儲存後待確認 ERP」紀錄，回傳其 updated_at（即最後一次觸發編輯儲存時間）。
     */
    public Optional<LocalDateTime> getLastEditSavedAt(LocalDate weekStart, String factory) {
        MaterialDemandPendingConfirmItemDTO item = getPendingConfirmItem(weekStart, factory);
        if (item.getUpdatedAt() == null) {
            return Optional.empty();
        }
        return Optional.of(item.getUpdatedAt());
    }

    private MaterialDemandPendingConfirmItemDTO mapPendingConfirmRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        MaterialDemandPendingConfirmItemDTO dto = new MaterialDemandPendingConfirmItemDTO();
        dto.setWeekStart(rs.getObject("week_start", LocalDate.class));
        dto.setFactory(rs.getString("factory"));
        dto.setStatus(rs.getInt("status"));
        java.sql.Timestamp ts = rs.getTimestamp("updated_at");
        dto.setUpdatedAt(ts != null ? ts.toLocalDateTime() : null);
        return dto;
    }

    private Integer getPendingStatus(LocalDate weekStart, String factory) {
        List<Integer> rows = jdbcTemplate.query(
                "SELECT status FROM material_demand_pending_confirm WHERE week_start = ? AND factory = ?",
                (rs, rowNum) -> rs.getInt("status"),
                weekStart,
                factory);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public byte[] generateTemplate(String factory) {
        return MaterialDemandTemplateService.generateTemplate();
    }
}
