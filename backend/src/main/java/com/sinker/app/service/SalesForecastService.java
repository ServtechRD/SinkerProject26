package com.sinker.app.service;

import com.sinker.app.dto.forecast.CreateForecastRequest;
import com.sinker.app.dto.forecast.ForecastResponse;
import com.sinker.app.dto.forecast.UpdateForecastRequest;
import com.sinker.app.entity.SalesForecast;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class SalesForecastService {

    private static final Logger log = LoggerFactory.getLogger(SalesForecastService.class);
    private static final DateTimeFormatter VERSION_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private final SalesForecastRepository forecastRepository;
    private final SalesForecastConfigRepository configRepository;
    private final ErpProductService erpProductService;
    private final JdbcTemplate jdbcTemplate;

    public SalesForecastService(SalesForecastRepository forecastRepository,
                               SalesForecastConfigRepository configRepository,
                               ErpProductService erpProductService,
                               JdbcTemplate jdbcTemplate) {
        this.forecastRepository = forecastRepository;
        this.configRepository = configRepository;
        this.erpProductService = erpProductService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ForecastResponse createForecast(CreateForecastRequest request, Long userId, String roleCode) {
        log.info("Creating forecast: user={}, month={}, channel={}, productCode={}",
                userId, request.getMonth(), request.getChannel(), request.getProductCode());

        // Validate month format
        validateMonthFormat(request.getMonth());

        // Check month is open
        validateMonthOpen(request.getMonth());

        // Check channel ownership
        checkChannelOwnership(userId, request.getChannel(), roleCode);

        // Validate product via ERP
        if (!erpProductService.validateProduct(request.getProductCode())) {
            throw new IllegalArgumentException("Invalid product_code: " + request.getProductCode());
        }

        // Check for duplicate
        if (forecastRepository.findByMonthAndChannelAndProductCode(
                request.getMonth(), request.getChannel(), request.getProductCode()).isPresent()) {
            throw new DuplicateEntryException(
                    "Product " + request.getProductCode() + " already exists for month "
                            + request.getMonth() + " and channel " + request.getChannel());
        }

        // Generate version string
        LocalDateTime now = LocalDateTime.now();
        String version = now.format(VERSION_FORMATTER) + "(" + request.getChannel() + ")";

        // Create entity
        SalesForecast forecast = new SalesForecast();
        forecast.setMonth(request.getMonth());
        forecast.setChannel(request.getChannel());
        forecast.setCategory(request.getCategory());
        forecast.setSpec(request.getSpec());
        forecast.setProductCode(request.getProductCode());
        forecast.setProductName(request.getProductName());
        forecast.setWarehouseLocation(request.getWarehouseLocation());
        forecast.setQuantity(request.getQuantity());
        forecast.setVersion(version);
        forecast.setIsModified(true);
        forecast.setCreatedAt(now);
        forecast.setUpdatedAt(now);

        SalesForecast saved = forecastRepository.save(forecast);
        log.info("Created forecast: id={}, user={}, month={}, channel={}, productCode={}",
                saved.getId(), userId, request.getMonth(), request.getChannel(), request.getProductCode());

        return ForecastResponse.fromEntity(saved);
    }

    @Transactional
    public ForecastResponse updateForecast(Integer id, UpdateForecastRequest request,
                                          Long userId, String roleCode) {
        log.info("Updating forecast: id={}, user={}, quantity={}", id, userId, request.getQuantity());

        // Find existing forecast
        SalesForecast forecast = forecastRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Forecast not found with id: " + id));

        // Check month is open
        validateMonthOpen(forecast.getMonth());

        // Check channel ownership
        checkChannelOwnership(userId, forecast.getChannel(), roleCode);

        // Generate new version string
        LocalDateTime now = LocalDateTime.now();
        String version = now.format(VERSION_FORMATTER) + "(" + forecast.getChannel() + ")";

        // Update fields
        forecast.setQuantity(request.getQuantity());
        forecast.setVersion(version);
        forecast.setIsModified(true);
        forecast.setUpdatedAt(now);

        SalesForecast saved = forecastRepository.save(forecast);
        log.info("Updated forecast: id={}, user={}, newQuantity={}, newVersion={}",
                id, userId, request.getQuantity(), version);

        return ForecastResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteForecast(Integer id, Long userId, String roleCode) {
        log.info("Deleting forecast: id={}, user={}", id, userId);

        // Find existing forecast
        SalesForecast forecast = forecastRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Forecast not found with id: " + id));

        // Check month is open
        validateMonthOpen(forecast.getMonth());

        // Check channel ownership
        checkChannelOwnership(userId, forecast.getChannel(), roleCode);

        // Hard delete
        forecastRepository.delete(forecast);
        log.info("Deleted forecast: id={}, user={}, month={}, channel={}, productCode={}",
                id, userId, forecast.getMonth(), forecast.getChannel(), forecast.getProductCode());
    }

    private void validateMonthFormat(String month) {
        if (month == null || !month.matches("\\d{6}")) {
            throw new IllegalArgumentException("Invalid month format. Expected YYYYMM, got: " + month);
        }
    }

    private void validateMonthOpen(String month) {
        SalesForecastConfig config = configRepository.findByMonth(month)
                .orElseThrow(() -> new ResourceNotFoundException("Month " + month + " not configured"));

        if (Boolean.TRUE.equals(config.getIsClosed())) {
            throw new AccessDeniedException("Month " + month + " is closed");
        }
    }

    private void checkChannelOwnership(Long userId, String channel, String roleCode) {
        // Admin bypasses channel ownership check
        if ("admin".equals(roleCode)) {
            return;
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales_channels_users WHERE user_id = ? AND channel = ?",
                Integer.class, userId, channel);
        if (count == null || count == 0) {
            throw new AccessDeniedException("No permission for channel: " + channel);
        }
    }

    public static class DuplicateEntryException extends RuntimeException {
        public DuplicateEntryException(String message) {
            super(message);
        }
    }
}
