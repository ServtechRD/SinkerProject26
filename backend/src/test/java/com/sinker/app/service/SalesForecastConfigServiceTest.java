package com.sinker.app.service;

import com.sinker.app.dto.forecast.ConfigResponse;
import com.sinker.app.dto.forecast.UpdateConfigRequest;
import com.sinker.app.entity.FormLogAction;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastReopenWindowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class SalesForecastConfigServiceTest {

    // 2026-12-10 08:00 台灣時間
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-12-10T00:00:00Z"), ZoneId.of("Asia/Taipei"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 10, 8, 0);

    @Mock private SalesForecastConfigRepository repository;
    @Mock private FormSummaryService formSummaryService;
    @Mock private FormLogService formLogService;
    @Mock private SalesForecastReopenWindowRepository windowRepository;

    private SalesForecastConfigService service;

    @BeforeEach
    void setUp() {
        service = new SalesForecastConfigService(repository, formSummaryService, formLogService, windowRepository, CLOCK);
    }

    private SalesForecastConfig config(Integer id, String month, LocalDateTime autoCloseAt, boolean closed) {
        SalesForecastConfig c = new SalesForecastConfig();
        c.setId(id);
        c.setMonth(month);
        c.setAutoCloseAt(autoCloseAt);
        c.setIsClosed(closed);
        c.setClosedAt(closed ? LocalDateTime.of(2026, 12, 9, 10, 0) : null);
        return c;
    }

    @Test
    void createMonth_setsAutoCloseAt() {
        when(repository.existsByMonth("202612")).thenReturn(false);
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfigResponse r = service.createMonth("202612", LocalDateTime.of(2026, 12, 10, 18, 30));

        assertEquals("202612", r.getMonth());
        assertEquals(LocalDateTime.of(2026, 12, 10, 18, 30), r.getAutoCloseAt());
        assertFalse(r.getIsClosed());
    }

    @Test
    void createMonth_duplicate() {
        when(repository.existsByMonth("202612")).thenReturn(true);
        assertThrows(SalesForecastConfigService.DuplicateMonthException.class,
                () -> service.createMonth("202612", NOW));
    }

    @Test
    void createMonth_invalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> service.createMonth("20261", NOW));
    }

    @Test
    void listAll_includesActiveReopenCount() {
        when(repository.findAllByOrderByMonthDesc()).thenReturn(List.of(config(1, "202612", NOW, true)));
        when(windowRepository.countByMonthAndEndedAtIsNull("202612")).thenReturn(2L);

        List<ConfigResponse> list = service.listAll();

        assertEquals(1, list.size());
        assertEquals(2L, list.get(0).getActiveReopenCount());
    }

    @Test
    void updateConfig_manualClose_logsAndSnapshots() {
        SalesForecastConfig c = config(1, "202612", NOW, false);
        when(repository.findById(1)).thenReturn(Optional.of(c));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateConfigRequest req = new UpdateConfigRequest();
        req.setIsClosed(true);

        ConfigResponse r = service.updateConfig(1, req, 7L);

        assertTrue(r.getIsClosed());
        assertNotNull(r.getClosedAt());
        verify(formSummaryService).createFormVersion1Snapshot(eq("202612"), any());
        verify(formLogService).record(eq("202612"), isNull(), eq(FormLogAction.MANUAL_CLOSE), eq(7L), anyString());
    }

    @Test
    void updateConfig_cannotReopen() {
        when(repository.findById(1)).thenReturn(Optional.of(config(1, "202612", NOW, true)));
        UpdateConfigRequest req = new UpdateConfigRequest();
        req.setIsClosed(false);
        assertThrows(IllegalArgumentException.class, () -> service.updateConfig(1, req, 7L));
    }

    @Test
    void updateConfig_closedUnchanged_doesNotLog() {
        when(repository.findById(1)).thenReturn(Optional.of(config(1, "202612", NOW, true)));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateConfigRequest req = new UpdateConfigRequest();
        req.setIsClosed(true);

        service.updateConfig(1, req, 7L);

        verify(formLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void updateConfig_validAutoCloseAt() {
        when(repository.findById(1)).thenReturn(Optional.of(config(1, "202612", NOW, false)));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateConfigRequest req = new UpdateConfigRequest();
        req.setAutoCloseAt("2026-12-11T18:00");

        ConfigResponse r = service.updateConfig(1, req, 7L);

        assertEquals(LocalDateTime.of(2026, 12, 11, 18, 0), r.getAutoCloseAt());
    }

    @Test
    void updateConfig_invalidAutoCloseAt() {
        when(repository.findById(1)).thenReturn(Optional.of(config(1, "202612", NOW, false)));
        UpdateConfigRequest req = new UpdateConfigRequest();
        req.setAutoCloseAt("2026/12/11");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateConfig(1, req, 7L));
        assertTrue(ex.getMessage().contains("autoCloseAt"));
    }

    @Test
    void updateConfig_notFound() {
        when(repository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.updateConfig(99, new UpdateConfigRequest(), 7L));
    }

    @Test
    void autoCloseDue_closesDueMonthsWithLog() {
        SalesForecastConfig a = config(1, "202612", NOW.minusMinutes(1), false);
        SalesForecastConfig b = config(2, "202701", NOW, false);
        when(repository.findByIsClosedFalseAndAutoCloseAtLessThanEqual(NOW)).thenReturn(List.of(a, b));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = service.autoCloseDue();

        assertEquals(2, count);
        assertTrue(a.getIsClosed());
        assertTrue(b.getIsClosed());
        verify(formSummaryService, times(2)).createFormVersion1Snapshot(anyString(), any());
        verify(formLogService).record(eq("202612"), isNull(), eq(FormLogAction.AUTO_CLOSE), isNull(), anyString());
        verify(formLogService).record(eq("202701"), isNull(), eq(FormLogAction.AUTO_CLOSE), isNull(), anyString());
    }

    @Test
    void autoCloseDue_none() {
        when(repository.findByIsClosedFalseAndAutoCloseAtLessThanEqual(NOW)).thenReturn(List.of());
        assertEquals(0, service.autoCloseDue());
        verify(repository, never()).save(any());
    }
}
