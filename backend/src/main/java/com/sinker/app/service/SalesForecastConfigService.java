package com.sinker.app.service;

import com.sinker.app.dto.forecast.ConfigResponse;
import com.sinker.app.dto.forecast.UpdateConfigRequest;
import com.sinker.app.entity.FormLogAction;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastReopenWindowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SalesForecastConfigService {

    private static final Logger log = LoggerFactory.getLogger(SalesForecastConfigService.class);
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private final SalesForecastConfigRepository repository;
    private final FormSummaryService formSummaryService;
    private final FormLogService formLogService;
    private final SalesForecastReopenWindowRepository windowRepository;
    private final Clock clock;

    public SalesForecastConfigService(SalesForecastConfigRepository repository,
                                      FormSummaryService formSummaryService,
                                      FormLogService formLogService,
                                      SalesForecastReopenWindowRepository windowRepository,
                                      Clock clock) {
        this.repository = repository;
        this.formSummaryService = formSummaryService;
        this.formLogService = formLogService;
        this.windowRepository = windowRepository;
        this.clock = clock;
    }

    @Transactional
    public ConfigResponse createMonth(String monthStr, LocalDateTime autoCloseAt) {
        YearMonth ym = parseMonth(monthStr);
        String formattedMonth = ym.format(MONTH_FORMAT);

        if (repository.existsByMonth(formattedMonth)) {
            throw new DuplicateMonthException("Month already exists: " + formattedMonth);
        }

        SalesForecastConfig config = new SalesForecastConfig();
        config.setMonth(formattedMonth);
        config.setAutoCloseAt(autoCloseAt);
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
                .map(c -> {
                    ConfigResponse r = ConfigResponse.fromEntity(c);
                    r.setActiveReopenCount(windowRepository.countByMonthAndEndedAtIsNull(c.getMonth()));
                    return r;
                })
                .toList();
    }

    @Transactional
    public ConfigResponse updateConfig(Integer id, UpdateConfigRequest request, Long userId) {
        SalesForecastConfig config = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Forecast config not found with id: " + id));

        if (request.getAutoCloseAt() != null) {
            config.setAutoCloseAt(parseDateTime(request.getAutoCloseAt()));
        }

        boolean newlyClosed = false;
        if (request.getIsClosed() != null) {
            boolean wasClosed = Boolean.TRUE.equals(config.getIsClosed());
            boolean nowClosed = request.getIsClosed();
            if (!wasClosed && nowClosed) {
                config.setIsClosed(true);
                config.setClosedAt(LocalDateTime.now());
                newlyClosed = true;
            } else if (wasClosed && !nowClosed) {
                throw new IllegalArgumentException("已結束新增的月份無法重新開放");
            }
        }

        config.setUpdatedAt(LocalDateTime.now());
        SalesForecastConfig saved = repository.save(config);

        if (newlyClosed) {
            formLogService.record(saved.getMonth(), null, FormLogAction.MANUAL_CLOSE, userId, "生管手動結束新增設定");
        }
        if (Boolean.TRUE.equals(saved.getIsClosed())) {
            try {
                formSummaryService.createFormVersion1Snapshot(saved.getMonth(), saved);
            } catch (Exception ex) {
                log.warn("Failed to create form version 1 snapshot for month {}: {}", saved.getMonth(), ex.getMessage());
            }
        }
        return ConfigResponse.fromEntity(saved);
    }

    /** 排程每分鐘呼叫：關閉截止時間（台灣時間）已到的月份 */
    @Transactional
    public int autoCloseDue() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<SalesForecastConfig> due = repository.findByIsClosedFalseAndAutoCloseAtLessThanEqual(now);
        int closedCount = 0;
        for (SalesForecastConfig config : due) {
            config.setIsClosed(true);
            // closed_at 沿用既有寫法（前端以 UTC 轉台灣時間顯示）
            config.setClosedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            SalesForecastConfig saved = repository.save(config);
            closedCount++;
            formLogService.record(saved.getMonth(), null, FormLogAction.AUTO_CLOSE, null,
                    "到達截止時間 " + saved.getAutoCloseAt().format(DISPLAY));
            log.info("Auto-closed month {} (auto_close_at={})", saved.getMonth(), saved.getAutoCloseAt());
            try {
                formSummaryService.createFormVersion1Snapshot(saved.getMonth(), saved);
            } catch (Exception ex) {
                log.warn("Failed to create form version 1 snapshot for month {}: {}", saved.getMonth(), ex.getMessage());
            }
        }
        return closedCount;
    }

    public static LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("autoCloseAt format must be YYYY-MM-DDTHH:mm");
        }
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
