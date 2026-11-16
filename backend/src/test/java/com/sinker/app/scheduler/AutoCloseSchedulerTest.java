package com.sinker.app.scheduler;

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

    @InjectMocks
    private AutoCloseScheduler scheduler;

    @Test
    void runEveryMinute_invokesAutoClose() {
        when(service.autoCloseDue()).thenReturn(2);
        scheduler.runEveryMinute();
        verify(service).autoCloseDue();
    }

    @Test
    void runEveryMinute_swallowsException() {
        when(service.autoCloseDue()).thenThrow(new RuntimeException("db down"));
        assertDoesNotThrow(() -> scheduler.runEveryMinute());
    }
}
