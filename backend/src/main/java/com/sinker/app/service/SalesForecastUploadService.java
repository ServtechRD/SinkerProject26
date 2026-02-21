package com.sinker.app.service;

import com.sinker.app.entity.SalesForecast;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.dto.forecast.UploadResponse;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class SalesForecastUploadService {

    private static final Logger log = LoggerFactory.getLogger(SalesForecastUploadService.class);

    private static final Set<String> VALID_CHANNELS = Set.of(
            "PX/大全聯", "家樂福", "7-11", "全家", "萊爾富", "OK超商",
            "美廉社", "愛買", "大潤發", "好市多", "頂好", "楓康"
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

        // 5. Parse Excel
        List<SalesForecastRow> rows = excelParserService.parse(file);

        if (rows.isEmpty()) {
            throw new ExcelParseException("Excel file has no data rows");
        }

        // 6. Validate each row against ERP
        List<String> erpErrors = new ArrayList<>();
        for (SalesForecastRow row : rows) {
            if (!erpProductService.validateProduct(row.getProductCode())) {
                erpErrors.add("Row " + row.getRowNumber() + ": Product " + row.getProductCode() + " not found in ERP");
            }
        }
        if (!erpErrors.isEmpty()) {
            throw new ExcelParseException(erpErrors);
        }

        // 7. Generate version string
        LocalDateTime now = LocalDateTime.now();
        String version = now.format(VERSION_FORMATTER) + "(" + channel + ")";

        // 8. Delete existing data for same month+channel
        forecastRepository.deleteByMonthAndChannel(month, channel);

        // 9. Insert all rows
        List<SalesForecast> entities = new ArrayList<>();
        for (SalesForecastRow row : rows) {
            SalesForecast sf = new SalesForecast();
            sf.setMonth(month);
            sf.setChannel(channel);
            sf.setCategory(row.getCategory());
            sf.setSpec(row.getSpec());
            sf.setProductCode(row.getProductCode());
            sf.setProductName(row.getProductName());
            sf.setWarehouseLocation(row.getWarehouseLocation());
            sf.setQuantity(row.getQuantity());
            sf.setVersion(version);
            sf.setIsModified(false);
            sf.setCreatedAt(now);
            sf.setUpdatedAt(now);
            entities.add(sf);
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
