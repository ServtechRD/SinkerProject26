package com.sinker.app.scheduler;

import com.sinker.app.service.ReopenService;
import com.sinker.app.service.SalesForecastConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoCloseSchedulerTest {

    @Mock
    private SalesForecastConfigService service;

    @Mock
    private ReopenService reopenService;

    @InjectMocks
    private AutoCloseScheduler scheduler;

    @Test
    void runEveryMinute_invokesAutoCloseAndFinishesEachExpiredMonth() {
        when(service.autoCloseDue()).thenReturn(2);
        when(reopenService.findExpiredMonths()).thenReturn(List.of("202612", "202701"));
        when(reopenService.finishExpiredMonth(anyString())).thenReturn(1);
        scheduler.runEveryMinute();
        verify(service).autoCloseDue();
        verify(reopenService).finishExpiredMonth("202612");
        verify(reopenService).finishExpiredMonth("202701");
    }

    @Test
    void runEveryMinute_oneMonthFailureDoesNotBlockOtherMonths() {
        when(reopenService.findExpiredMonths()).thenReturn(List.of("202612", "202701"));
        when(reopenService.finishExpiredMonth("202612")).thenThrow(new RuntimeException("merge failed"));
        assertDoesNotThrow(() -> scheduler.runEveryMinute());
        verify(reopenService).finishExpiredMonth("202701");
    }

    @Test
    void runEveryMinute_autoCloseFailureDoesNotBlockReopenExpiry() {
        when(service.autoCloseDue()).thenThrow(new RuntimeException("db down"));
        assertDoesNotThrow(() -> scheduler.runEveryMinute());
        verify(reopenService).findExpiredMonths();
    }
}
