package com.sinker.app.service;

import com.sinker.app.entity.SalesForecast;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.dto.forecast.UploadResponse;
import com.sinker.app.dto.reference.ProductDTO;
import com.sinker.app.exception.ExcelParseException;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastRepository;
import com.sinker.app.service.ExcelParserService.SalesForecastRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SalesForecastUploadService {

    private static final Logger log = LoggerFactory.getLogger(SalesForecastUploadService.class);

    private static final Set<String> VALID_CHANNELS = Set.of(
            "PX + 大全聯", "家樂福", "愛買", "7-11", "全家", "Ok+萊爾富",
            "好市多", "楓康", "美聯社", "康是美", "電商", "市面經銷"
    );

    private static final DateTimeFormatter VERSION_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private final SalesForecastRepository forecastRepository;
    private final SalesForecastConfigRepository configRepository;
    private final ExcelParserService excelParserService;
    private final ErpProductService erpProductService;
    private final JdbcTemplate jdbcTemplate;

    public SalesForecastUploadService(SalesForecastRepository forecastRepository,
                                      SalesForecastConfigRepository configRepository,
                                      ExcelParserService excelParserService,
                                      ErpProductService erpProductService,
                                      JdbcTemplate jdbcTemplate) {
        this.forecastRepository = forecastRepository;
        this.configRepository = configRepository;
        this.excelParserService = excelParserService;
        this.erpProductService = erpProductService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public UploadResponse upload(MultipartFile file, String month, String channel,
                                 Long userId, String roleCode) {
        long startTime = System.currentTimeMillis();
        log.info("Starting upload: user={}, month={}, channel={}", userId, month, channel);

        // 1. Validate channel
        if (!VALID_CHANNELS.contains(channel)) {
            throw new IllegalArgumentException("Invalid channel: " + channel);
        }

        // 2. Validate month format (YYYYMM)
        validateMonthFormat(month);

        // 3. Check month exists and is open
        SalesForecastConfig config = configRepository.findByMonth(month)
                .orElseThrow(() -> new ResourceNotFoundException("Month " + month + " not configured"));

        if (Boolean.TRUE.equals(config.getIsClosed())) {
            throw new AccessDeniedException("Month " + month + " is closed");
        }

        // 4. Check user owns channel (admin bypasses this check)
        if (!"admin".equals(roleCode)) {
            checkChannelOwnership(userId, channel);
        }

        // 5. Parse file (Excel or CSV)
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".csv") && !filename.toLowerCase().endsWith(".xlsx"))) {
            throw new IllegalArgumentException("Only .csv or .xlsx files are accepted");
        }
        boolean isCsv = filename.toLowerCase().endsWith(".csv");
        List<SalesForecastRow> rows = isCsv
                ? excelParserService.parseCsv(file)
                : excelParserService.parse(file);

        if (rows.isEmpty()) {
            throw new ExcelParseException(isCsv ? "CSV file has no data rows" : "Excel file has no data rows");
        }

        // 6. Validate each row: 品號必須存在於系統中
        // 若檔案品號不存在：只提示「不能上傳」，不覆蓋寫入。
        Set<String> invalidProductCodes = new LinkedHashSet<>();
        for (SalesForecastRow row : rows) {
            String rawCode = row.getProductCode();
            String trimmed = rawCode != null ? rawCode.trim() : null;
            if (trimmed == null || !StringUtils.hasText(trimmed) || !erpProductService.validateProduct(trimmed)) {
                if (StringUtils.hasText(trimmed)) invalidProductCodes.add(trimmed);
                else invalidProductCodes.add(rawCode != null ? rawCode : "（空白品號）");
            }
        }
        if (!invalidProductCodes.isEmpty()) {
            String codeList = String.join("、", invalidProductCodes);
            throw new ExcelParseException(List.of("以下品號不存在於系統中，無法上傳：" + codeList));
        }

        // 7. Generate version string
        LocalDateTime now = LocalDateTime.now();
        String version = now.format(VERSION_FORMATTER) + "(" + channel + ")";

        // Cache product master to avoid querying same code repeatedly
        Map<String, ProductDTO> productCache = new HashMap<>();

        // 8. Delete existing data for same month+channel
        forecastRepository.deleteByMonthAndChannel(month, channel);

        // 9. Insert all rows
        List<SalesForecast> entities = new ArrayList<>();
        for (SalesForecastRow row : rows) {
            String productCode = row.getProductCode() != null ? row.getProductCode().trim() : null;
            ProductDTO product = null;
            if (productCode != null) {
                product = productCache.computeIfAbsent(productCode, (k) ->
                        erpProductService.findProduct(k).orElse(null));
            }

            SalesForecast sf = new SalesForecast();
            sf.setMonth(month);
            sf.setChannel(channel);
            // If product code exists in DB, always use DB fields to prevent upload mismatch.
            if (product != null) {
                sf.setCategory(product.getCategoryName());
                sf.setSpec(product.getSpec());
                sf.setProductCode(product.getCode());
                sf.setProductName(product.getName());
                sf.setWarehouseLocation(product.getWarehouseLocation());
            } else {
                // 理論上不會發生（已在步驟 6 validate），但仍保護：品號查不到就視為不可上傳
                if (StringUtils.hasText(productCode)) invalidProductCodes.add(productCode);
                sf.setCategory(row.getCategory());
                sf.setSpec(row.getSpec());
                sf.setProductCode(row.getProductCode());
                sf.setProductName(row.getProductName());
                sf.setWarehouseLocation(row.getWarehouseLocation());
            }
            sf.setQuantity(row.getQuantity());
            sf.setVersion(version);
            sf.setIsModified(false);
            sf.setCreatedAt(now);
            sf.setUpdatedAt(now);
            entities.add(sf);
        }
        if (!invalidProductCodes.isEmpty()) {
            String codeList = String.join("、", invalidProductCodes);
            throw new ExcelParseException(List.of("以下品號不存在於系統中，無法上傳：" + codeList));
        }
        forecastRepository.saveAll(entities);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Upload complete: user={}, month={}, channel={}, rows={}, version={}, duration={}ms",
                userId, month, channel, rows.size(), version, duration);

        return new UploadResponse(rows.size(), version, now, month, channel);
    }

    private void validateMonthFormat(String month) {
        if (month == null || !month.matches("\\d{6}")) {
            throw new IllegalArgumentException("Invalid month format. Expected YYYYMM, got: " + month);
        }
    }

    private void checkChannelOwnership(Long userId, String channel) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales_channels_users WHERE user_id = ? AND channel = ?",
                Integer.class, userId, channel);
        if (count == null || count == 0) {
            throw new AccessDeniedException("No permission for channel: " + channel);
        }
    }

    public static class BadRequestException extends RuntimeException {
        private final List<String> details;

        public BadRequestException(String message) {
            super(message);
            this.details = List.of(message);
        }

        public BadRequestException(List<String> details) {
            super(String.join("; ", details));
            this.details = details;
        }

        public List<String> getDetails() { return details; }
    }
}
