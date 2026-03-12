package com.sinker.app.service;

import com.sinker.app.entity.GiftSalesForecast;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.dto.forecast.UploadResponse;
import com.sinker.app.exception.ExcelParseException;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.GiftSalesForecastRepository;
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
public class GiftSalesForecastUploadService {

    private static final Logger log = LoggerFactory.getLogger(GiftSalesForecastUploadService.class);

    private static final Set<String> VALID_CHANNELS = Set.of(
            "PX + 大全聯", "家樂福", "愛買", "7-11", "全家", "Ok+萊爾富",
            "好市多", "楓康", "美聯社", "康是美", "電商", "市面經銷"
    );

    private static final DateTimeFormatter VERSION_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private final GiftSalesForecastRepository forecastRepository;
    private final SalesForecastConfigRepository configRepository;
    private final ExcelParserService excelParserService;
    private final ErpProductService erpProductService;
    private final JdbcTemplate jdbcTemplate;

    public GiftSalesForecastUploadService(GiftSalesForecastRepository forecastRepository,
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
        log.info("Starting gift forecast upload: user={}, month={}, channel={}", userId, month, channel);

        if (!VALID_CHANNELS.contains(channel)) {
            throw new IllegalArgumentException("Invalid channel: " + channel);
        }
        validateMonthFormat(month);

        SalesForecastConfig config = configRepository.findByMonth(month)
                .orElseThrow(() -> new ResourceNotFoundException("Month " + month + " not configured"));
        if (Boolean.TRUE.equals(config.getIsClosed())) {
            throw new AccessDeniedException("Month " + month + " is closed");
        }
        if (!"admin".equals(roleCode)) {
            checkChannelOwnership(userId, channel);
        }

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

        List<String> erpErrors = new ArrayList<>();
        for (SalesForecastRow row : rows) {
            if (!erpProductService.validateProduct(row.getProductCode())) {
                erpErrors.add("Row " + row.getRowNumber() + ": Product " + row.getProductCode() + " not found in ERP");
            }
        }
        if (!erpErrors.isEmpty()) {
            throw new ExcelParseException(erpErrors);
        }

        LocalDateTime now = LocalDateTime.now();
        String version = now.format(VERSION_FORMATTER) + "(" + channel + ")";

        forecastRepository.deleteByMonthAndChannel(month, channel);

        List<GiftSalesForecast> entities = new ArrayList<>();
        for (SalesForecastRow row : rows) {
            GiftSalesForecast g = new GiftSalesForecast();
            g.setMonth(month);
            g.setChannel(channel);
            g.setCategory(row.getCategory());
            g.setSpec(row.getSpec());
            g.setProductCode(row.getProductCode());
            g.setProductName(row.getProductName());
            g.setWarehouseLocation(row.getWarehouseLocation());
            g.setQuantity(row.getQuantity());
            g.setVersion(version);
            g.setIsModified(false);
            g.setCreatedAt(now);
            g.setUpdatedAt(now);
            entities.add(g);
        }
        forecastRepository.saveAll(entities);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Gift upload complete: user={}, month={}, channel={}, rows={}, version={}, duration={}ms",
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
}
