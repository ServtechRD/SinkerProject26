package com.sinker.app.scheduler;

import com.sinker.app.service.ReopenService;
import com.sinker.app.service.SalesForecastConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void runEveryMinute_invokesAutoCloseAndReopenExpiry() {
        when(service.autoCloseDue()).thenReturn(2);
        when(reopenService.processExpired()).thenReturn(1);
        scheduler.runEveryMinute();
        verify(service).autoCloseDue();
        verify(reopenService).processExpired();
    }

    @Test
    void runEveryMinute_autoCloseFailureDoesNotBlockReopenExpiry() {
        when(service.autoCloseDue()).thenThrow(new RuntimeException("db down"));
        assertDoesNotThrow(() -> scheduler.runEveryMinute());
        verify(reopenService).processExpired();
    }
}
