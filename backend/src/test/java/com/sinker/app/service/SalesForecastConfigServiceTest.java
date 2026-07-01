package com.sinker.app.service;

import com.sinker.app.dto.forecast.ConfigResponse;
import com.sinker.app.dto.forecast.UpdateConfigRequest;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.SalesForecastConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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

    @Mock
    private FormSummaryService formSummaryService;

    private SalesForecastConfigService service;

    @BeforeEach
    void setUp() {
        service = new SalesForecastConfigService(repository, formSummaryService);
    }

    private SalesForecastConfig createConfig(Integer id, String month,
                                              LocalDate autoCloseDate, boolean isClosed) {
        SalesForecastConfig config = new SalesForecastConfig();
        config.setId(id);
        config.setMonth(month);
        config.setAutoCloseDate(autoCloseDate);
        config.setIsClosed(isClosed);
        config.setClosedAt(isClosed ? LocalDateTime.of(2025, 1, 15, 10, 0) : null);
        config.setCreatedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        config.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        return config;
    }

    // --- Create Month ---

    @Test
    void testCreateMonth_Success() {
        when(repository.existsByMonth("202501")).thenReturn(false);
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> {
            SalesForecastConfig c = inv.getArgument(0);
            c.setId(1);
            return c;
        });

        LocalDate autoCloseDate = LocalDate.of(2025, 2, 10);
        ConfigResponse response = service.createMonth("202501", autoCloseDate);

        assertEquals("202501", response.getMonth());
        assertEquals(autoCloseDate, response.getAutoCloseDate());
        assertFalse(response.getIsClosed());
    }

    @Test
    void testCreateMonth_DuplicateMonth() {
        when(repository.existsByMonth("202501")).thenReturn(true);

        assertThrows(SalesForecastConfigService.DuplicateMonthException.class,
                () -> service.createMonth("202501", LocalDate.of(2025, 2, 10)));
        verify(repository, never()).save(any());
    }

    @Test
    void testCreateMonth_InvalidMonthFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createMonth("20251", LocalDate.of(2025, 2, 10)));
    }

    // --- Update Config ---

    @Test
    void testUpdateConfig_ChangeClosedToTrue() {
        SalesForecastConfig config = createConfig(1, "202501", LocalDate.of(2025, 1, 10), false);
        when(repository.findById(1)).thenReturn(Optional.of(config));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setIsClosed(true);

        ConfigResponse response = service.updateConfig(1, request);

        assertTrue(response.getIsClosed());
        assertNotNull(response.getClosedAt());
    }

    @Test
    void testUpdateConfig_CannotReopenWhenClosed() {
        SalesForecastConfig config = createConfig(1, "202501", LocalDate.of(2025, 1, 10), true);
        when(repository.findById(1)).thenReturn(Optional.of(config));

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setIsClosed(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateConfig(1, request));
        assertTrue(ex.getMessage().contains("無法重新開放"));
        verify(repository, never()).save(any());
    }

    @Test
    void testUpdateConfig_ClosedUnchanged() {
        LocalDateTime originalClosedAt = LocalDateTime.of(2025, 1, 15, 10, 0);
        SalesForecastConfig config = createConfig(1, "202501", LocalDate.of(2025, 1, 10), true);
        config.setClosedAt(originalClosedAt);
        when(repository.findById(1)).thenReturn(Optional.of(config));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setAutoCloseDate("2025-01-20");

        ConfigResponse response = service.updateConfig(1, request);

        assertEquals(LocalDate.of(2025, 1, 20), response.getAutoCloseDate());
        assertTrue(response.getIsClosed());
        assertEquals(originalClosedAt, response.getClosedAt());
    }

    @Test
    void testUpdateConfig_ValidAutoCloseDate() {
        SalesForecastConfig config = createConfig(1, "202501", LocalDate.of(2025, 1, 10), false);
        when(repository.findById(1)).thenReturn(Optional.of(config));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setAutoCloseDate("2025-02-15");

        ConfigResponse response = service.updateConfig(1, request);

        assertEquals(LocalDate.of(2025, 2, 15), response.getAutoCloseDate());
    }

    @Test
    void testUpdateConfig_InvalidAutoCloseDateFormat() {
        SalesForecastConfig config = createConfig(1, "202501", LocalDate.of(2025, 1, 10), false);
        when(repository.findById(1)).thenReturn(Optional.of(config));

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setAutoCloseDate("2025/02/15");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateConfig(1, request));
        assertEquals("autoCloseDate format must be YYYY-MM-DD", ex.getMessage());
    }

    @Test
    void testUpdateConfig_NotFound() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setAutoCloseDate("2025-01-15");

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateConfig(99, request));
    }

    @Test
    void testUpdateConfig_BothFields() {
        SalesForecastConfig config = createConfig(1, "202501", LocalDate.of(2025, 1, 10), false);
        when(repository.findById(1)).thenReturn(Optional.of(config));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setAutoCloseDate("2025-01-25");
        request.setIsClosed(true);

        ConfigResponse response = service.updateConfig(1, request);

        assertEquals(LocalDate.of(2025, 1, 25), response.getAutoCloseDate());
        assertTrue(response.getIsClosed());
        assertNotNull(response.getClosedAt());
    }

    // --- List All ---

    @Test
    void testListAllConfigs() {
        SalesForecastConfig c1 = createConfig(1, "202503", LocalDate.of(2025, 3, 10), false);
        SalesForecastConfig c2 = createConfig(2, "202502", LocalDate.of(2025, 2, 15), true);
        SalesForecastConfig c3 = createConfig(3, "202501", LocalDate.of(2025, 1, 10), false);
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
    void testAutoCloseMatchingMonths_Matches() {
        LocalDate closeDate = LocalDate.of(2025, 1, 15);
        SalesForecastConfig config = createConfig(1, "202501", closeDate, false);
        when(repository.findByIsClosedFalseAndAutoCloseDate(closeDate)).thenReturn(List.of(config));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = service.autoCloseMatchingMonths(closeDate);

        assertEquals(1, count);
        assertTrue(config.getIsClosed());
        assertNotNull(config.getClosedAt());
    }

    @Test
    void testAutoCloseMatchingMonths_NoMatch() {
        LocalDate closeDate = LocalDate.of(2025, 1, 10);
        when(repository.findByIsClosedFalseAndAutoCloseDate(closeDate)).thenReturn(List.of());

        int count = service.autoCloseMatchingMonths(closeDate);

        assertEquals(0, count);
        verify(repository, never()).save(any());
    }

    @Test
    void testAutoCloseMatchingMonths_MultipleMonths() {
        LocalDate closeDate = LocalDate.of(2025, 1, 10);
        SalesForecastConfig c1 = createConfig(1, "202501", closeDate, false);
        SalesForecastConfig c2 = createConfig(2, "202502", closeDate, false);
        SalesForecastConfig c3 = createConfig(3, "202503", closeDate, false);
        when(repository.findByIsClosedFalseAndAutoCloseDate(closeDate))
                .thenReturn(List.of(c1, c2, c3));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = service.autoCloseMatchingMonths(closeDate);

        assertEquals(3, count);
        assertTrue(c1.getIsClosed());
        assertTrue(c2.getIsClosed());
        assertTrue(c3.getIsClosed());
    }
}
