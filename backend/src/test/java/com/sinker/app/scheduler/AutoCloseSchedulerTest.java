package com.sinker.app.scheduler;

import com.sinker.app.service.SalesForecastConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoCloseSchedulerTest {

    @Mock
    private SalesForecastConfigService service;

    @InjectMocks
    private AutoCloseScheduler scheduler;

    @Test
    void testAutoCloseMonths_InvokesService() {
        when(service.autoCloseMatchingMonths(anyInt())).thenReturn(2);

        scheduler.autoCloseMonths();

        verify(service).autoCloseMatchingMonths(anyInt());
    }

    @Test
    void testAutoCloseMonths_ZeroResults() {
        when(service.autoCloseMatchingMonths(anyInt())).thenReturn(0);

        scheduler.autoCloseMonths();

        verify(service).autoCloseMatchingMonths(anyInt());
    }
}
