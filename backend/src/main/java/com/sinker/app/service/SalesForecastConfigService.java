package com.sinker.app.service;

import com.sinker.app.dto.forecast.ConfigResponse;
import com.sinker.app.dto.forecast.UpdateConfigRequest;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.service.FormSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SalesForecastConfigService {

    private static final Logger log = LoggerFactory.getLogger(SalesForecastConfigService.class);
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    private final SalesForecastConfigRepository repository;
    private final FormSummaryService formSummaryService;

    public SalesForecastConfigService(SalesForecastConfigRepository repository,
                                      FormSummaryService formSummaryService) {
        this.repository = repository;
        this.formSummaryService = formSummaryService;
    }

    @Transactional
    public ConfigResponse createMonth(String monthStr, LocalDate autoCloseDate) {
        YearMonth ym = parseMonth(monthStr);
        String formattedMonth = ym.format(MONTH_FORMAT);

        if (repository.existsByMonth(formattedMonth)) {
            throw new DuplicateMonthException("Month already exists: " + formattedMonth);
        }

        SalesForecastConfig config = new SalesForecastConfig();
        config.setMonth(formattedMonth);
        config.setAutoCloseDate(autoCloseDate);
        config.setIsClosed(false);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        try {
            SalesForecastConfig saved = repository.save(config);
            log.info("Created forecast config for month {}", formattedMonth);
            return ConfigResponse.fromEntity(saved);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateMonthException("Month already exists: " + formattedMonth);
        }
    }

    @Transactional(readOnly = true)
    public List<ConfigResponse> listAll() {
        return repository.findAllByOrderByMonthDesc().stream()
                .map(ConfigResponse::fromEntity)
                .toList();
    }

    @Transactional
    public ConfigResponse updateConfig(Integer id, UpdateConfigRequest request) {
        SalesForecastConfig config = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Forecast config not found with id: " + id));

        if (request.getAutoCloseDate() != null) {
            try {
                LocalDate date = LocalDate.parse(request.getAutoCloseDate());
                config.setAutoCloseDate(date);
            } catch (Exception e) {
                throw new IllegalArgumentException("autoCloseDate format must be YYYY-MM-DD");
            }
        }

        if (request.getIsClosed() != null) {
            boolean wasClosed = Boolean.TRUE.equals(config.getIsClosed());
            boolean nowClosed = request.getIsClosed();

            if (!wasClosed && nowClosed) {
                config.setIsClosed(true);
                config.setClosedAt(LocalDateTime.now());
            } else if (wasClosed && !nowClosed) {
                throw new IllegalArgumentException("已結束新增的月份無法重新開放");
            }
        }

        config.setUpdatedAt(LocalDateTime.now());
        SalesForecastConfig saved = repository.save(config);

        if (Boolean.TRUE.equals(saved.getIsClosed())) {
            try {
                formSummaryService.createFormVersion1Snapshot(saved.getMonth(), saved);
            } catch (Exception ex) {
                log.warn("Failed to create form version 1 snapshot for month {}: {}", saved.getMonth(), ex.getMessage());
            }
        }
        return ConfigResponse.fromEntity(saved);
    }

    @Transactional
    public int autoCloseMatchingMonths(LocalDate currentDate) {
        List<SalesForecastConfig> configs =
                repository.findByIsClosedFalseAndAutoCloseDate(currentDate);

        LocalDateTime now = LocalDateTime.now();
        int closedCount = 0;
        for (SalesForecastConfig config : configs) {
            config.setIsClosed(true);
            config.setClosedAt(now);
            config.setUpdatedAt(now);
            SalesForecastConfig saved = repository.save(config);
            closedCount++;
            log.info("Auto-closed month {} (auto_close_date={})", saved.getMonth(), currentDate);
            try {
                formSummaryService.createFormVersion1Snapshot(saved.getMonth(), saved);
            } catch (Exception ex) {
                log.warn("Failed to create form version 1 snapshot for month {}: {}", saved.getMonth(), ex.getMessage());
            }
        }

        return closedCount;
    }

    private YearMonth parseMonth(String monthStr) {
        try {
            int year = Integer.parseInt(monthStr.substring(0, 4));
            int month = Integer.parseInt(monthStr.substring(4, 6));
            return YearMonth.of(year, month);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid month format: " + monthStr
                    + ". Expected YYYYMM");
        }
    }

    public static class DuplicateMonthException extends RuntimeException {
        public DuplicateMonthException(String message) {
            super(message);
        }
    }
}
