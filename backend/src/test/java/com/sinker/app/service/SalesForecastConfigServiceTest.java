package com.sinker.app.service;

import com.sinker.app.dto.forecast.ConfigResponse;
import com.sinker.app.dto.forecast.CreateMonthsResponse;
import com.sinker.app.dto.forecast.UpdateConfigRequest;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.SalesForecastConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesForecastConfigServiceTest {

    @Mock
    private SalesForecastConfigRepository repository;

    private SalesForecastConfigService service;

    @BeforeEach
    void setUp() {
        service = new SalesForecastConfigService(repository);
    }

    private SalesForecastConfig createConfig(Integer id, String month,
                                              int autoCloseDay, boolean isClosed) {
        SalesForecastConfig config = new SalesForecastConfig();
        config.setId(id);
        config.setMonth(month);
        config.setAutoCloseDay(autoCloseDay);
        config.setIsClosed(isClosed);
        config.setClosedAt(isClosed ? LocalDateTime.of(2025, 1, 15, 10, 0) : null);
        config.setCreatedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        config.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        return config;
    }

    // --- Batch Create Months ---

    @Test
    void testBatchCreateMonths_Success() {
        when(repository.existsByMonth(anyString())).thenReturn(false);
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> {
            SalesForecastConfig c = inv.getArgument(0);
            c.setId(1);
            return c;
        });

        CreateMonthsResponse response = service.batchCreateMonths("202501", "202503");

        assertEquals(3, response.getCreatedCount());
        assertEquals(List.of("202501", "202502", "202503"), response.getMonths());
        verify(repository, times(3)).save(any(SalesForecastConfig.class));

        ArgumentCaptor<SalesForecastConfig> captor =
                ArgumentCaptor.forClass(SalesForecastConfig.class);
        verify(repository, times(3)).save(captor.capture());
        for (SalesForecastConfig saved : captor.getAllValues()) {
            assertEquals(10, saved.getAutoCloseDay());
            assertFalse(saved.getIsClosed());
        }
    }

    @Test
    void testBatchCreateMonths_SingleMonth() {
        when(repository.existsByMonth("202501")).thenReturn(false);
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> {
            SalesForecastConfig c = inv.getArgument(0);
            c.setId(1);
            return c;
        });

        CreateMonthsResponse response = service.batchCreateMonths("202501", "202501");

        assertEquals(1, response.getCreatedCount());
        assertEquals(List.of("202501"), response.getMonths());
    }

    @Test
    void testBatchCreateMonths_InvalidRange() {
        assertThrows(IllegalArgumentException.class,
                () -> service.batchCreateMonths("202503", "202501"));
    }

    @Test
    void testBatchCreateMonths_DuplicateHandling() {
        when(repository.existsByMonth("202501")).thenReturn(true);
        when(repository.existsByMonth("202502")).thenReturn(true);
        when(repository.existsByMonth("202503")).thenReturn(true);

        assertThrows(SalesForecastConfigService.DuplicateMonthException.class,
                () -> service.batchCreateMonths("202501", "202503"));
    }

    @Test
    void testBatchCreateMonths_PartialDuplicate() {
        when(repository.existsByMonth("202501")).thenReturn(true);
        when(repository.existsByMonth("202502")).thenReturn(false);
        when(repository.existsByMonth("202503")).thenReturn(false);
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> {
            SalesForecastConfig c = inv.getArgument(0);
            c.setId(1);
            return c;
        });

        CreateMonthsResponse response = service.batchCreateMonths("202501", "202503");

        assertEquals(2, response.getCreatedCount());
        assertEquals(List.of("202502", "202503"), response.getMonths());
    }

    @Test
    void testBatchCreateMonths_InvalidMonthFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> service.batchCreateMonths("20251", "202503"));
    }

    @Test
    void testBatchCreateMonths_CrossYear() {
        when(repository.existsByMonth(anyString())).thenReturn(false);
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> {
            SalesForecastConfig c = inv.getArgument(0);
            c.setId(1);
            return c;
        });

        CreateMonthsResponse response = service.batchCreateMonths("202511", "202602");

        assertEquals(4, response.getCreatedCount());
        assertEquals(List.of("202511", "202512", "202601", "202602"), response.getMonths());
    }

    // --- Update Config ---

    @Test
    void testUpdateConfig_ChangeClosedToTrue() {
        SalesForecastConfig config = createConfig(1, "202501", 10, false);
        when(repository.findById(1)).thenReturn(Optional.of(config));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setIsClosed(true);

        ConfigResponse response = service.updateConfig(1, request);

        assertTrue(response.getIsClosed());
        assertNotNull(response.getClosedAt());
    }

    @Test
    void testUpdateConfig_ChangeClosedToFalse() {
        SalesForecastConfig config = createConfig(1, "202501", 10, true);
        when(repository.findById(1)).thenReturn(Optional.of(config));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setIsClosed(false);

        ConfigResponse response = service.updateConfig(1, request);

        assertFalse(response.getIsClosed());
        assertNull(response.getClosedAt());
    }

    @Test
    void testUpdateConfig_ClosedUnchanged() {
        LocalDateTime originalClosedAt = LocalDateTime.of(2025, 1, 15, 10, 0);
        SalesForecastConfig config = createConfig(1, "202501", 10, true);
        config.setClosedAt(originalClosedAt);
        when(repository.findById(1)).thenReturn(Optional.of(config));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setAutoCloseDay(20);

        ConfigResponse response = service.updateConfig(1, request);

        assertEquals(20, response.getAutoCloseDay());
        assertTrue(response.getIsClosed());
        assertEquals(originalClosedAt, response.getClosedAt());
    }

    @Test
    void testUpdateConfig_AutoCloseDayTooLow() {
        SalesForecastConfig config = createConfig(1, "202501", 10, false);
        when(repository.findById(1)).thenReturn(Optional.of(config));

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setAutoCloseDay(0);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateConfig(1, request));
    }

    @Test
    void testUpdateConfig_AutoCloseDayTooHigh() {
        SalesForecastConfig config = createConfig(1, "202501", 10, false);
        when(repository.findById(1)).thenReturn(Optional.of(config));

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setAutoCloseDay(32);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateConfig(1, request));
    }

    @Test
    void testUpdateConfig_NotFound() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setAutoCloseDay(15);

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateConfig(99, request));
    }

    @Test
    void testUpdateConfig_BothFields() {
        SalesForecastConfig config = createConfig(1, "202501", 10, false);
        when(repository.findById(1)).thenReturn(Optional.of(config));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setAutoCloseDay(25);
        request.setIsClosed(true);

        ConfigResponse response = service.updateConfig(1, request);

        assertEquals(25, response.getAutoCloseDay());
        assertTrue(response.getIsClosed());
        assertNotNull(response.getClosedAt());
    }

    // --- List All ---

    @Test
    void testListAllConfigs() {
        SalesForecastConfig c1 = createConfig(1, "202503", 10, false);
        SalesForecastConfig c2 = createConfig(2, "202502", 15, true);
        SalesForecastConfig c3 = createConfig(3, "202501", 10, false);
        when(repository.findAllByOrderByMonthDesc()).thenReturn(List.of(c1, c2, c3));

        List<ConfigResponse> configs = service.listAll();

        assertEquals(3, configs.size());
        assertEquals("202503", configs.get(0).getMonth());
        assertEquals("202502", configs.get(1).getMonth());
        assertEquals("202501", configs.get(2).getMonth());
    }

    @Test
    void testListAllConfigs_Empty() {
        when(repository.findAllByOrderByMonthDesc()).thenReturn(List.of());

        List<ConfigResponse> configs = service.listAll();

        assertTrue(configs.isEmpty());
    }

    // --- Auto Close ---

    @Test
    void testAutoCloseMatchingMonths_MatchingDay() {
        SalesForecastConfig config = createConfig(1, "202501", 15, false);
        when(repository.findByIsClosedFalseAndAutoCloseDay(15)).thenReturn(List.of(config));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = service.autoCloseMatchingMonths(15);

        assertEquals(1, count);
        assertTrue(config.getIsClosed());
        assertNotNull(config.getClosedAt());
    }

    @Test
    void testAutoCloseMatchingMonths_NoMatch() {
        when(repository.findByIsClosedFalseAndAutoCloseDay(10)).thenReturn(List.of());

        int count = service.autoCloseMatchingMonths(10);

        assertEquals(0, count);
        verify(repository, never()).save(any());
    }

    @Test
    void testAutoCloseMatchingMonths_MultipleMonths() {
        SalesForecastConfig c1 = createConfig(1, "202501", 10, false);
        SalesForecastConfig c2 = createConfig(2, "202502", 10, false);
        SalesForecastConfig c3 = createConfig(3, "202503", 10, false);
        when(repository.findByIsClosedFalseAndAutoCloseDay(10))
                .thenReturn(List.of(c1, c2, c3));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = service.autoCloseMatchingMonths(10);

        assertEquals(3, count);
        assertTrue(c1.getIsClosed());
        assertTrue(c2.getIsClosed());
        assertTrue(c3.getIsClosed());
    }
}
