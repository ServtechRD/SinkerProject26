package com.sinker.app.service;

import com.sinker.app.dto.forecast.UploadResponse;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.exception.ExcelParseException;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastRepository;
import com.sinker.app.service.ExcelParserService.SalesForecastRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesForecastUploadServiceTest {

    @Mock private SalesForecastRepository forecastRepository;
    @Mock private SalesForecastConfigRepository configRepository;
    @Mock private ExcelParserService excelParserService;
    @Mock private ErpProductService erpProductService;
    @Mock private JdbcTemplate jdbcTemplate;

    private SalesForecastUploadService service;

    private static final Long USER_ID = 1L;
    private static final String ROLE_SALES = "sales";
    private static final String ROLE_ADMIN = "admin";
    private static final String MONTH = "202601";
    private static final String CHANNEL = "家樂福";

    @BeforeEach
    void setUp() {
        service = new SalesForecastUploadService(forecastRepository, configRepository,
                excelParserService, erpProductService, jdbcTemplate);
    }

    private MockMultipartFile dummyFile() {
        return new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy".getBytes());
    }

    private SalesForecastConfig openConfig() {
        SalesForecastConfig cfg = new SalesForecastConfig();
        cfg.setMonth(MONTH);
        cfg.setIsClosed(false);
        return cfg;
    }

    private SalesForecastConfig closedConfig() {
        SalesForecastConfig cfg = new SalesForecastConfig();
        cfg.setMonth(MONTH);
        cfg.setIsClosed(true);
        return cfg;
    }

    private List<SalesForecastRow> makeRows(int count) {
        List<SalesForecastRow> rows = new java.util.ArrayList<>();
        for (int i = 1; i <= count; i++) {
            rows.add(new SalesForecastRow(
                    "飲料類", "600ml*24入", "P00" + i, "商品" + i, "A0" + i,
                    new BigDecimal("100.50"), i + 1));
        }
        return rows;
    }

    @Test
    void testUpload_Success() {
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(openConfig()));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(USER_ID), eq(CHANNEL)))
                .thenReturn(1);
        when(excelParserService.parse(any())).thenReturn(makeRows(5));
        when(erpProductService.validateProduct(anyString())).thenReturn(true);
        when(forecastRepository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);

        UploadResponse response = service.upload(dummyFile(), MONTH, CHANNEL, USER_ID, ROLE_SALES);

        assertEquals(5, response.getRowsProcessed());
        assertEquals(MONTH, response.getMonth());
        assertEquals(CHANNEL, response.getChannel());
        assertNotNull(response.getVersion());
        assertTrue(response.getVersion().endsWith("(" + CHANNEL + ")"));
        assertNotNull(response.getUploadTimestamp());

        verify(forecastRepository).deleteByMonthAndChannel(MONTH, CHANNEL);
        verify(forecastRepository).saveAll(anyList());
    }

    @Test
    void testUpload_AdminBypassesChannelCheck() {
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(openConfig()));
        when(excelParserService.parse(any())).thenReturn(makeRows(3));
        when(erpProductService.validateProduct(anyString())).thenReturn(true);
        when(forecastRepository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);

        // Admin role → no channel ownership check
        UploadResponse response = service.upload(dummyFile(), MONTH, CHANNEL, USER_ID, ROLE_ADMIN);

        assertEquals(3, response.getRowsProcessed());
        // jdbcTemplate should NOT be called for admin
        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Integer.class), any(), any());
    }

    @Test
    void testUpload_MonthClosed() {
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(closedConfig()));

        assertThrows(AccessDeniedException.class,
                () -> service.upload(dummyFile(), MONTH, CHANNEL, USER_ID, ROLE_SALES));
    }

    @Test
    void testUpload_MonthNotFound() {
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.upload(dummyFile(), MONTH, CHANNEL, USER_ID, ROLE_SALES));
    }

    @Test
    void testUpload_InvalidChannel() {
        assertThrows(IllegalArgumentException.class,
                () -> service.upload(dummyFile(), MONTH, "InvalidChannel", USER_ID, ROLE_SALES));
    }

    @Test
    void testUpload_UserDoesNotOwnChannel() {
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(openConfig()));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(USER_ID), eq(CHANNEL)))
                .thenReturn(0);

        assertThrows(AccessDeniedException.class,
                () -> service.upload(dummyFile(), MONTH, CHANNEL, USER_ID, ROLE_SALES));
    }

    @Test
    void testUpload_ProductValidationFails() {
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(openConfig()));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(USER_ID), eq(CHANNEL)))
                .thenReturn(1);
        List<SalesForecastRow> rows = List.of(
                new SalesForecastRow("飲料類", "spec", "P999", "商品", "A01",
                        new BigDecimal("100.00"), 2)
        );
        when(excelParserService.parse(any())).thenReturn(rows);
        when(erpProductService.validateProduct("P999")).thenReturn(false);

        ExcelParseException ex = assertThrows(ExcelParseException.class,
                () -> service.upload(dummyFile(), MONTH, CHANNEL, USER_ID, ROLE_SALES));
        assertTrue(ex.getMessage().contains("P999"), "Error should mention P999");
    }

    @Test
    void testUpload_GeneratesCorrectVersionFormat() {
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(openConfig()));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(USER_ID), eq(CHANNEL)))
                .thenReturn(1);
        when(excelParserService.parse(any())).thenReturn(makeRows(1));
        when(erpProductService.validateProduct(anyString())).thenReturn(true);
        when(forecastRepository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);

        UploadResponse response = service.upload(dummyFile(), MONTH, CHANNEL, USER_ID, ROLE_SALES);

        // Version format: YYYY/MM/DD HH:MM:SS(channel)
        String version = response.getVersion();
        assertTrue(version.matches("\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}\\(.+\\)"),
                "Version should match format YYYY/MM/DD HH:MM:SS(channel): " + version);
        assertTrue(version.endsWith("(" + CHANNEL + ")"),
                "Version should end with channel: " + version);
    }

    @Test
    void testUpload_AllItemsHaveIsModifiedFalse() {
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(openConfig()));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(USER_ID), eq(CHANNEL)))
                .thenReturn(1);
        when(excelParserService.parse(any())).thenReturn(makeRows(3));
        when(erpProductService.validateProduct(anyString())).thenReturn(true);
        when(forecastRepository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);

        service.upload(dummyFile(), MONTH, CHANNEL, USER_ID, ROLE_SALES);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<com.sinker.app.entity.SalesForecast>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(forecastRepository).saveAll(captor.capture());

        List<com.sinker.app.entity.SalesForecast> saved = captor.getValue();
        assertEquals(3, saved.size());
        for (com.sinker.app.entity.SalesForecast sf : saved) {
            assertFalse(Boolean.TRUE.equals(sf.getIsModified()),
                    "All rows should have isModified=false");
        }
    }

    @Test
    void testUpload_DeletesOldDataBeforeInsert() {
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(openConfig()));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(USER_ID), eq(CHANNEL)))
                .thenReturn(1);
        when(excelParserService.parse(any())).thenReturn(makeRows(2));
        when(erpProductService.validateProduct(anyString())).thenReturn(true);
        when(forecastRepository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);

        service.upload(dummyFile(), MONTH, CHANNEL, USER_ID, ROLE_SALES);

        // Delete should be called before saveAll
        var inOrder = inOrder(forecastRepository);
        inOrder.verify(forecastRepository).deleteByMonthAndChannel(MONTH, CHANNEL);
        inOrder.verify(forecastRepository).saveAll(anyList());
    }

    @Test
    void testUpload_InvalidMonthFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> service.upload(dummyFile(), "2026-01", CHANNEL, USER_ID, ROLE_SALES));
        assertThrows(IllegalArgumentException.class,
                () -> service.upload(dummyFile(), "ABCDEF", CHANNEL, USER_ID, ROLE_SALES));
    }

    @Test
    void testBadRequestException_SingleMessage() {
        SalesForecastUploadService.BadRequestException ex =
                new SalesForecastUploadService.BadRequestException("Single error message");

        assertEquals("Single error message", ex.getMessage());
        assertNotNull(ex.getDetails());
        assertEquals(1, ex.getDetails().size());
        assertEquals("Single error message", ex.getDetails().get(0));
    }

    @Test
    void testBadRequestException_MultipleMessages() {
        List<String> details = List.of("Error 1", "Error 2", "Error 3");
        SalesForecastUploadService.BadRequestException ex =
                new SalesForecastUploadService.BadRequestException(details);

        assertEquals("Error 1; Error 2; Error 3", ex.getMessage());
        assertNotNull(ex.getDetails());
        assertEquals(3, ex.getDetails().size());
        assertEquals("Error 1", ex.getDetails().get(0));
        assertEquals("Error 2", ex.getDetails().get(1));
        assertEquals("Error 3", ex.getDetails().get(2));
    }

    @Test
    void testBadRequestException_EmptyList() {
        List<String> details = List.of();
        SalesForecastUploadService.BadRequestException ex =
                new SalesForecastUploadService.BadRequestException(details);

        assertEquals("", ex.getMessage());
        assertNotNull(ex.getDetails());
        assertEquals(0, ex.getDetails().size());
    }
}
