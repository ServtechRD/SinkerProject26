package com.sinker.app.service;

import com.sinker.app.dto.forecast.ConfigResponse;
import com.sinker.app.dto.forecast.EditStatusResponse;
import com.sinker.app.dto.forecast.FormDetailResponse;
import com.sinker.app.dto.forecast.FormLogDTO;
import com.sinker.app.entity.FormLogAction;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.entity.SalesForecastReopenWindow;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.GiftSalesForecastRepository;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastFormLogRepository;
import com.sinker.app.repository.SalesForecastRepository;
import com.sinker.app.util.WorkingDays;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 表單明細頁、異動紀錄、業務端編輯狀態的唯讀查詢 */
@Service
public class ForecastFormDetailService {

    private final SalesForecastConfigRepository configRepository;
    private final SalesForecastRepository forecastRepository;
    private final GiftSalesForecastRepository giftForecastRepository;
    private final SalesForecastFormLogRepository formLogRepository;
    private final FormLogService formLogService;
    private final FormSummaryService formSummaryService;
    private final ForecastEditGuard editGuard;
    private final Clock clock;

    public ForecastFormDetailService(SalesForecastConfigRepository configRepository,
                                     SalesForecastRepository forecastRepository,
                                     GiftSalesForecastRepository giftForecastRepository,
                                     SalesForecastFormLogRepository formLogRepository,
                                     FormLogService formLogService,
                                     FormSummaryService formSummaryService,
                                     ForecastEditGuard editGuard,
                                     Clock clock) {
        this.configRepository = configRepository;
        this.forecastRepository = forecastRepository;
        this.giftForecastRepository = giftForecastRepository;
        this.formLogRepository = formLogRepository;
        this.formLogService = formLogService;
        this.formSummaryService = formSummaryService;
        this.editGuard = editGuard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public FormDetailResponse getFormDetail(Integer id) {
        SalesForecastConfig config = findConfig(id);
        String month = config.getMonth();
        boolean closed = Boolean.TRUE.equals(config.getIsClosed());
        Map<String, LocalDateTime> salesAt = toChannelMap(forecastRepository.findLastUploadAtByChannel(month));
        Map<String, LocalDateTime> giftAt = toChannelMap(giftForecastRepository.findLastUploadAtByChannel(month));

        // ponytail: 通路清單沿用 CHANNEL_ORDER，R3 通路主檔上線後改讀主檔
        List<FormDetailResponse.ChannelStatus> channels = FormSummaryService.CHANNEL_ORDER.stream()
                .map(ch -> new FormDetailResponse.ChannelStatus(
                        ch,
                        salesAt.get(ch),
                        giftAt.get(ch),
                        editGuard.activeWindow(month, ch)
                                .map(w -> new FormDetailResponse.ActiveWindow(w.getId(), w.getExpiresAt(), w.getReason()))
                                .orElse(null),
                        closed ? formSummaryService.countCorrections(month, ch) : 0))
                .toList();

        String closeType = formLogRepository
                .findFirstByMonthAndActionInOrderByCreatedAtDescIdDesc(month,
                        List.of(FormLogAction.AUTO_CLOSE, FormLogAction.MANUAL_CLOSE))
                .map(l -> l.getAction() == FormLogAction.MANUAL_CLOSE ? "MANUAL" : "AUTO")
                .orElse(null);

        return new FormDetailResponse(ConfigResponse.fromEntity(config), closeType,
                WorkingDays.addWorkingDays(LocalDateTime.now(clock), ReopenService.MAX_WORKING_DAYS), channels);
    }

    @Transactional(readOnly = true)
    public List<FormLogDTO> getLogs(Integer id) {
        return formLogService.list(findConfig(id).getMonth());
    }

    @Transactional(readOnly = true)
    public EditStatusResponse getEditStatus(String month, String channel) {
        SalesForecastConfig config = configRepository.findByMonth(month)
                .orElseThrow(() -> new ResourceNotFoundException("Month " + month + " not configured"));
        ForecastEditGuard.Status status = editGuard.status(config, channel);
        if (status == ForecastEditGuard.Status.OPEN) {
            return new EditStatusResponse(status.name(), config.getAutoCloseAt(), null, null, null);
        }
        Optional<SalesForecastReopenWindow> window = editGuard.activeWindow(month, channel);
        Map<String, BigDecimal> planner = formSummaryService.latestPlannerQuantities(month, channel);
        return new EditStatusResponse(status.name(), config.getAutoCloseAt(),
                window.map(SalesForecastReopenWindow::getExpiresAt).orElse(null),
                window.map(SalesForecastReopenWindow::getReason).orElse(null),
                planner);
    }

    private SalesForecastConfig findConfig(Integer id) {
        return configRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Forecast config not found with id: " + id));
    }

    private static Map<String, LocalDateTime> toChannelMap(List<Object[]> rows) {
        Map<String, LocalDateTime> map = new HashMap<>();
        for (Object[] r : rows) {
            map.put((String) r[0], (LocalDateTime) r[1]);
        }
        return map;
    }
}
