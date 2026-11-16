package com.sinker.app.service;

import com.sinker.app.dto.forecast.EditStatusResponse;
import com.sinker.app.dto.forecast.FormDetailResponse;
import com.sinker.app.entity.FormLogAction;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.entity.SalesForecastFormLog;
import com.sinker.app.entity.SalesForecastReopenWindow;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.GiftSalesForecastRepository;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastFormLogRepository;
import com.sinker.app.repository.SalesForecastRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ForecastFormDetailServiceTest {

    // 2026-12-10（週四）08:00 台灣時間
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-12-10T00:00:00Z"), ZoneId.of("Asia/Taipei"));

    @Mock private SalesForecastConfigRepository configRepository;
    @Mock private SalesForecastRepository forecastRepository;
    @Mock private GiftSalesForecastRepository giftForecastRepository;
    @Mock private SalesForecastFormLogRepository formLogRepository;
    @Mock private FormLogService formLogService;
    @Mock private FormSummaryService formSummaryService;
    @Mock private ForecastEditGuard editGuard;

    private ForecastFormDetailService service;

    @BeforeEach
    void setUp() {
        service = new ForecastFormDetailService(configRepository, forecastRepository, giftForecastRepository,
                formLogRepository, formLogService, formSummaryService, editGuard, CLOCK);
    }

    private static SalesForecastConfig config(boolean closed) {
        SalesForecastConfig c = new SalesForecastConfig();
        c.setId(7);
        c.setMonth("202612");
        c.setAutoCloseAt(LocalDateTime.of(2026, 12, 10, 0, 0));
        c.setIsClosed(closed);
        return c;
    }

    private static SalesForecastReopenWindow window() {
        SalesForecastReopenWindow w = new SalesForecastReopenWindow();
        w.setId(3);
        w.setExpiresAt(LocalDateTime.of(2026, 12, 12, 18, 0));
        w.setReason("全聯新品提案追加");
        return w;
    }

    @Test
    void getFormDetail_listsChannelStatus() {
        when(configRepository.findById(7)).thenReturn(Optional.of(config(true)));
        LocalDateTime t1 = LocalDateTime.of(2026, 12, 8, 9, 40);
        LocalDateTime t2 = LocalDateTime.of(2026, 12, 9, 3, 2);
        when(forecastRepository.findLastUploadAtByChannel("202612")).thenReturn(List.<Object[]>of(new Object[]{"家樂福", t1}));
        when(giftForecastRepository.findLastUploadAtByChannel("202612")).thenReturn(List.<Object[]>of(new Object[]{"愛買", t2}));
        when(editGuard.activeWindow(anyString(), anyString())).thenReturn(Optional.empty());
        when(editGuard.activeWindow("202612", "家樂福")).thenReturn(Optional.of(window()));
        when(formSummaryService.countCorrections(anyString(), anyString())).thenReturn(0);
        when(formSummaryService.countCorrections("202612", "家樂福")).thenReturn(2);
        SalesForecastFormLog closeLog = new SalesForecastFormLog();
        closeLog.setAction(FormLogAction.MANUAL_CLOSE);
        when(formLogRepository.findFirstByMonthAndActionInOrderByCreatedAtDescIdDesc(eq("202612"), anyCollection()))
                .thenReturn(Optional.of(closeLog));

        FormDetailResponse d = service.getFormDetail(7);

        assertEquals("MANUAL", d.closeType());
        assertEquals(LocalDateTime.of(2026, 12, 15, 8, 0), d.reopenMaxExpiresAt());
        assertEquals(FormSummaryService.CHANNEL_ORDER.size(), d.channels().size());
        FormDetailResponse.ChannelStatus carrefour = d.channels().get(FormSummaryService.CHANNEL_ORDER.indexOf("家樂福"));
        assertEquals(t1, carrefour.salesLastUploadAt());
        assertNull(carrefour.giftLastUploadAt());
        assertEquals(3, carrefour.activeWindow().id());
        assertEquals(2, carrefour.correctionCount());
        FormDetailResponse.ChannelStatus aimai = d.channels().get(FormSummaryService.CHANNEL_ORDER.indexOf("愛買"));
        assertEquals(t2, aimai.giftLastUploadAt());
        assertNull(aimai.activeWindow());
    }

    @Test
    void getFormDetail_notFound() {
        when(configRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getFormDetail(99));
    }

    @Test
    void getEditStatus_closed_returnsPlannerQuantities() {
        SalesForecastConfig c = config(true);
        when(configRepository.findByMonth("202612")).thenReturn(Optional.of(c));
        when(editGuard.status(c, "家樂福")).thenReturn(ForecastEditGuard.Status.CLOSED);
        when(editGuard.activeWindow("202612", "家樂福")).thenReturn(Optional.empty());
        when(formSummaryService.latestPlannerQuantities("202612", "家樂福")).thenReturn(Map.of("P1", new BigDecimal("150")));

        EditStatusResponse r = service.getEditStatus("202612", "家樂福");

        assertEquals("CLOSED", r.status());
        assertEquals(c.getAutoCloseAt(), r.autoCloseAt());
        assertNull(r.reopenExpiresAt());
        assertEquals(0, new BigDecimal("150").compareTo(r.plannerQuantities().get("P1")));
    }

    @Test
    void getEditStatus_reopened_includesWindow() {
        SalesForecastConfig c = config(true);
        when(configRepository.findByMonth("202612")).thenReturn(Optional.of(c));
        when(editGuard.status(c, "家樂福")).thenReturn(ForecastEditGuard.Status.REOPENED);
        when(editGuard.activeWindow("202612", "家樂福")).thenReturn(Optional.of(window()));

        EditStatusResponse r = service.getEditStatus("202612", "家樂福");

        assertEquals("REOPENED", r.status());
        assertEquals(LocalDateTime.of(2026, 12, 12, 18, 0), r.reopenExpiresAt());
        assertEquals("全聯新品提案追加", r.reopenReason());
    }

    @Test
    void getEditStatus_open_noPlannerQuantities() {
        SalesForecastConfig c = config(false);
        when(configRepository.findByMonth("202612")).thenReturn(Optional.of(c));
        when(editGuard.status(c, "家樂福")).thenReturn(ForecastEditGuard.Status.OPEN);

        EditStatusResponse r = service.getEditStatus("202612", "家樂福");

        assertEquals("OPEN", r.status());
        assertNull(r.plannerQuantities());
        verifyNoInteractions(formSummaryService);
    }
}
