package com.sinker.app.service;

import com.sinker.app.dto.forecast.ReopenRequest;
import com.sinker.app.entity.FormLogAction;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.entity.SalesForecastReopenWindow;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastReopenWindowRepository;
import com.sinker.app.util.WorkingDays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 開放補登與補登結束 */
@Service
public class ReopenService {

    private static final Logger log = LoggerFactory.getLogger(ReopenService.class);
    public static final int MAX_WORKING_DAYS = 3;
    static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private final SalesForecastConfigRepository configRepository;
    private final SalesForecastReopenWindowRepository windowRepository;
    private final FormLogService formLogService;
    private final FormSummaryService formSummaryService;
    private final Clock clock;

    public ReopenService(SalesForecastConfigRepository configRepository,
                         SalesForecastReopenWindowRepository windowRepository,
                         FormLogService formLogService,
                         FormSummaryService formSummaryService,
                         Clock clock) {
        this.configRepository = configRepository;
        this.windowRepository = windowRepository;
        this.formLogService = formLogService;
        this.formSummaryService = formSummaryService;
        this.clock = clock;
    }

    /** 開放補登：任一通路不符規則則整批不建立 */
    @Transactional
    public void open(Integer configId, ReopenRequest request, Long userId) {
        SalesForecastConfig config = configRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("Forecast config not found with id: " + configId));
        if (!Boolean.TRUE.equals(config.getIsClosed())) {
            throw new IllegalArgumentException("表單尚未結束新增，不需開放補登");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = SalesForecastConfigService.parseDateTime(request.getExpiresAt());
        LocalDateTime max = WorkingDays.addWorkingDays(now, MAX_WORKING_DAYS);
        if (!expiresAt.isAfter(now)) {
            throw new IllegalArgumentException("補登截止時間必須晚於現在");
        }
        if (expiresAt.isAfter(max)) {
            throw new IllegalArgumentException("補登期限不可超過 " + max.format(DISPLAY));
        }

        List<String> channels = List.copyOf(new LinkedHashSet<>(request.getChannels()));
        List<String> invalid = channels.stream().filter(ch -> !FormSummaryService.CHANNEL_ORDER.contains(ch)).toList();
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException("無效的通路：" + String.join("、", invalid));
        }
        String month = config.getMonth();
        List<String> busy = channels.stream()
                .filter(ch -> windowRepository.existsByMonthAndChannelAndEndedAtIsNull(month, ch)).toList();
        if (!busy.isEmpty()) {
            throw new IllegalArgumentException(String.join("、", busy) + " 已在補登中，請先結束原本的補登");
        }

        // 舊版程式可能讓歷史月份沒有 form_v* 列，開放後結束時合併會讀到空而使未補登通路數量歸零
        if (!formSummaryService.hasReopenBase(month)) {
            throw new IllegalArgumentException("此月份沒有生管表單版本資料，無法開放補登，請洽系統管理員");
        }

        String reason = request.getReason().trim();
        for (String channel : channels) {
            SalesForecastReopenWindow w = new SalesForecastReopenWindow();
            w.setMonth(month);
            w.setChannel(channel);
            w.setExpiresAt(expiresAt);
            w.setReason(reason);
            w.setOpenedBy(userId);
            w.setOpenedAt(now);
            windowRepository.save(w);
            formLogService.record(month, channel, FormLogAction.REOPEN, userId,
                    "期限 " + expiresAt.format(DISPLAY) + "；原因：" + reason);
        }
        log.info("Reopened month {} channels {} until {}", month, channels, expiresAt);
    }

    /** 生管提前結束單一通路的補登 */
    @Transactional
    public void end(Integer configId, Integer windowId, Long userId) {
        SalesForecastConfig config = configRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("Forecast config not found with id: " + configId));
        SalesForecastReopenWindow w = windowRepository.findById(windowId)
                .filter(x -> x.getMonth().equals(config.getMonth()))
                .orElseThrow(() -> new ResourceNotFoundException("Reopen window not found with id: " + windowId));
        if (w.getEndedAt() != null) {
            throw new IllegalArgumentException("此補登已結束");
        }
        finishWindows(config.getMonth(), List.of(w), userId);
    }

    /** 已到期但尚未結束補登的月份（不重複，保持出現順序） */
    @Transactional(readOnly = true)
    public List<String> findExpiredMonths() {
        return windowRepository.findByEndedAtIsNullAndExpiresAtLessThanEqual(LocalDateTime.now(clock)).stream()
                .map(SalesForecastReopenWindow::getMonth).distinct().toList();
    }

    /** 結束單一月份已到期的補登（同月份合併成一個新版本）；每個月份各自一個交易，由排程逐月呼叫 */
    @Transactional
    public int finishExpiredMonth(String month) {
        List<SalesForecastReopenWindow> windows = windowRepository
                .findByMonthAndEndedAtIsNullAndExpiresAtLessThanEqual(month, LocalDateTime.now(clock));
        if (windows.isEmpty()) {
            return 0;
        }
        finishWindows(month, windows, null);
        return windows.size();
    }

    private void finishWindows(String month, List<SalesForecastReopenWindow> windows, Long userId) {
        Set<String> channels = windows.stream().map(SalesForecastReopenWindow::getChannel)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        String reasons = windows.stream().map(SalesForecastReopenWindow::getReason).distinct()
                .collect(Collectors.joining("；"));
        int versionNo = formSummaryService.mergeReopenedChannels(month, channels,
                "補登：" + String.join("、", channels) + "／" + reasons);
        String label = "form_v" + versionNo;
        LocalDateTime now = LocalDateTime.now(clock);
        for (SalesForecastReopenWindow w : windows) {
            w.setEndedAt(now);
            w.setEndedBy(userId);
            w.setMergedVersion(label);
            windowRepository.save(w);
            formLogService.record(month, w.getChannel(), FormLogAction.REOPEN_END, userId,
                    (userId == null ? "到期自動鎖回" : "生管提前結束") + "，產生版本 " + label);
        }
    }
}
