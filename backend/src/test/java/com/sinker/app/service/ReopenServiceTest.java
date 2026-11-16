package com.sinker.app.service;

import com.sinker.app.dto.forecast.ReopenRequest;
import com.sinker.app.entity.FormLogAction;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.entity.SalesForecastReopenWindow;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastReopenWindowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReopenServiceTest {

    // 2026-12-10（週四）08:00 台灣時間；3 個工作天上限 = 2026-12-15（週二）08:00
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-12-10T00:00:00Z"), ZoneId.of("Asia/Taipei"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 10, 8, 0);

    @Mock private SalesForecastConfigRepository configRepository;
    @Mock private SalesForecastReopenWindowRepository windowRepository;
    @Mock private FormLogService formLogService;
    @Mock private FormSummaryService formSummaryService;

    private ReopenService service;

    @BeforeEach
    void setUp() {
        service = new ReopenService(configRepository, windowRepository, formLogService, formSummaryService, CLOCK);
    }

    private static SalesForecastConfig config(boolean closed) {
        SalesForecastConfig c = new SalesForecastConfig();
        c.setId(1);
        c.setMonth("202612");
        c.setIsClosed(closed);
        return c;
    }

    private static ReopenRequest request(String expiresAt, String... channels) {
        ReopenRequest r = new ReopenRequest();
        r.setChannels(List.of(channels));
        r.setExpiresAt(expiresAt);
        r.setReason("全聯新品提案追加");
        return r;
    }

    @Test
    void open_notClosed_throws() {
        when(configRepository.findById(1)).thenReturn(Optional.of(config(false)));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.open(1, request("2026-12-12T18:00", "家樂福"), 7L));
        assertTrue(ex.getMessage().contains("尚未結束新增"));
    }

    @Test
    void open_expiresNotInFuture_throws() {
        when(configRepository.findById(1)).thenReturn(Optional.of(config(true)));
        assertThrows(IllegalArgumentException.class,
                () -> service.open(1, request("2026-12-10T08:00", "家樂福"), 7L));
    }

    @Test
    void open_beyondThreeWorkingDays_throws() {
        when(configRepository.findById(1)).thenReturn(Optional.of(config(true)));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.open(1, request("2026-12-15T08:01", "家樂福"), 7L));
        assertTrue(ex.getMessage().contains("2026/12/15 08:00"));
    }

    @Test
    void open_invalidChannel_throws() {
        when(configRepository.findById(1)).thenReturn(Optional.of(config(true)));
        assertThrows(IllegalArgumentException.class,
                () -> service.open(1, request("2026-12-12T18:00", "不存在通路"), 7L));
    }

    @Test
    void open_channelAlreadyReopened_throwsAndSavesNothing() {
        when(configRepository.findById(1)).thenReturn(Optional.of(config(true)));
        when(windowRepository.existsByMonthAndChannelAndEndedAtIsNull("202612", "家樂福")).thenReturn(false);
        when(windowRepository.existsByMonthAndChannelAndEndedAtIsNull("202612", "愛買")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.open(1, request("2026-12-12T18:00", "家樂福", "愛買"), 7L));

        assertTrue(ex.getMessage().contains("愛買"));
        verify(windowRepository, never()).save(any());
        verifyNoInteractions(formLogService);
    }

    @Test
    void open_success_createsWindowPerChannelAndLogs() {
        when(configRepository.findById(1)).thenReturn(Optional.of(config(true)));

        service.open(1, request("2026-12-15T08:00", "家樂福", "愛買"), 7L);

        ArgumentCaptor<SalesForecastReopenWindow> captor = ArgumentCaptor.forClass(SalesForecastReopenWindow.class);
        verify(windowRepository, times(2)).save(captor.capture());
        SalesForecastReopenWindow first = captor.getAllValues().get(0);
        assertEquals("202612", first.getMonth());
        assertEquals("家樂福", first.getChannel());
        assertEquals(LocalDateTime.of(2026, 12, 15, 8, 0), first.getExpiresAt());
        assertEquals(7L, first.getOpenedBy());
        assertEquals(NOW, first.getOpenedAt());
        assertNull(first.getEndedAt());
        verify(formLogService).record(eq("202612"), eq("家樂福"), eq(FormLogAction.REOPEN), eq(7L), contains("全聯新品提案追加"));
        verify(formLogService).record(eq("202612"), eq("愛買"), eq(FormLogAction.REOPEN), eq(7L), anyString());
    }
}
