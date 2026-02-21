package com.sinker.app.service;

import com.sinker.app.dto.forecast.ConfigResponse;
import com.sinker.app.dto.forecast.CreateMonthsResponse;
import com.sinker.app.dto.forecast.UpdateConfigRequest;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.SalesForecastConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class SalesForecastConfigService {

    private static final Logger log = LoggerFactory.getLogger(SalesForecastConfigService.class);
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    private final SalesForecastConfigRepository repository;

    public SalesForecastConfigService(SalesForecastConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CreateMonthsResponse batchCreateMonths(String startMonth, String endMonth) {
        YearMonth start = parseMonth(startMonth);
        YearMonth end = parseMonth(endMonth);

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start_month must not be after end_month");
        }

        List<String> createdMonths = new ArrayList<>();
        List<String> skippedMonths = new ArrayList<>();
        YearMonth current = start;

        while (!current.isAfter(end)) {
            String monthStr = current.format(MONTH_FORMAT);
            if (repository.existsByMonth(monthStr)) {
                skippedMonths.add(monthStr);
                log.info("Month {} already exists, skipping", monthStr);
            } else {
                SalesForecastConfig config = new SalesForecastConfig();
                config.setMonth(monthStr);
                config.setAutoCloseDay(10);
                config.setIsClosed(false);
                config.setCreatedAt(LocalDateTime.now());
                config.setUpdatedAt(LocalDateTime.now());
                try {
                    repository.save(config);
                    createdMonths.add(monthStr);
                    log.info("Created forecast config for month {}", monthStr);
                } catch (DataIntegrityViolationException e) {
                    skippedMonths.add(monthStr);
                    log.warn("Duplicate month {} detected during save, skipping", monthStr);
                }
            }
            current = current.plusMonths(1);
        }

        if (createdMonths.isEmpty() && !skippedMonths.isEmpty()) {
            throw new DuplicateMonthException("All months already exist: " + skippedMonths);
        }

        return new CreateMonthsResponse(createdMonths.size(), createdMonths);
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

        if (request.getAutoCloseDay() != null) {
            if (request.getAutoCloseDay() < 1 || request.getAutoCloseDay() > 31) {
                throw new IllegalArgumentException(
                        "auto_close_day must be between 1 and 31");
            }
            config.setAutoCloseDay(request.getAutoCloseDay());
        }

        if (request.getIsClosed() != null) {
            boolean wasClosed = Boolean.TRUE.equals(config.getIsClosed());
            boolean nowClosed = request.getIsClosed();

            if (!wasClosed && nowClosed) {
                config.setIsClosed(true);
                config.setClosedAt(LocalDateTime.now());
            } else if (wasClosed && !nowClosed) {
                config.setIsClosed(false);
                config.setClosedAt(null);
            }
        }

        config.setUpdatedAt(LocalDateTime.now());
        SalesForecastConfig saved = repository.save(config);
        return ConfigResponse.fromEntity(saved);
    }

    @Transactional
    public int autoCloseMatchingMonths(int currentDay) {
        List<SalesForecastConfig> configs =
                repository.findByIsClosedFalseAndAutoCloseDay(currentDay);

        LocalDateTime now = LocalDateTime.now();
        for (SalesForecastConfig config : configs) {
            config.setIsClosed(true);
            config.setClosedAt(now);
            config.setUpdatedAt(now);
            repository.save(config);
            log.info("Auto-closed month {} (auto_close_day={})", config.getMonth(), currentDay);
        }

        return configs.size();
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
