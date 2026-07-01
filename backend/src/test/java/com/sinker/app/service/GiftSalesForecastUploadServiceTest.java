package com.sinker.app.service;

import com.sinker.app.dto.forecast.UploadResponse;
import com.sinker.app.dto.reference.ProductDTO;
import com.sinker.app.entity.GiftSalesForecast;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.exception.ExcelParseException;
import com.sinker.app.repository.GiftSalesForecastRepository;
import com.sinker.app.repository.SalesForecastConfigRepository;
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
class GiftSalesForecastUploadServiceTest {

    @Mock
    private GiftSalesForecastRepository forecastRepository;
    @Mock
    private SalesForecastConfigRepository configRepository;
    @Mock
    private ExcelParserService excelParserService;
    @Mock
    private ErpProductService erpProductService;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private GiftSalesForecastUploadService service;

    @BeforeEach
    void setUp() {
        service = new GiftSalesForecastUploadService(
                forecastRepository, configRepository, excelParserService, erpProductService, jdbcTemplate
        );
    }

    @Test
    void upload_csvFile_usesCsvParser() {
        SalesForecastConfig cfg = new SalesForecastConfig();
        cfg.setIsClosed(false);
        when(configRepository.findByMonth("202601")).thenReturn(Optional.of(cfg));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq("家樂福"))).thenReturn(1);
        when(excelParserService.parseCsv(any())).thenReturn(List.of(
                new SalesForecastRow("C", "S", "P1", "N", "A01", new BigDecimal("10"), "", 2)
        ));
        when(erpProductService.validateProduct("P1")).thenReturn(true);
        ProductDTO p = new ProductDTO();
        p.setCode("P1");
        p.setName("N1");
        p.setCategoryName("C1");
        p.setSpec("S1");
        p.setWarehouseLocation("A99");
        when(erpProductService.findProduct("P1")).thenReturn(Optional.of(p));

        UploadResponse resp = service.upload(csvFile(), "202601", "家樂福", 1L, "sales");

        assertEquals(1, resp.getRowsProcessed());
        verify(excelParserService).parseCsv(any());
        verify(excelParserService, never()).parse(any());
    }

    @Test
    void upload_invalidExtension_throws() {
        SalesForecastConfig cfg = new SalesForecastConfig();
        cfg.setIsClosed(false);
        when(configRepository.findByMonth("202601")).thenReturn(Optional.of(cfg));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq("家樂福"))).thenReturn(1);

        assertThrows(IllegalArgumentException.class,
                () -> service.upload(txtFile(), "202601", "家樂福", 1L, "sales"));
    }

    @Test
    void upload_userNotOwner_throws() {
        SalesForecastConfig cfg = new SalesForecastConfig();
        cfg.setIsClosed(false);
        when(configRepository.findByMonth("202601")).thenReturn(Optional.of(cfg));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq("家樂福"))).thenReturn(0);

        assertThrows(AccessDeniedException.class,
                () -> service.upload(xlsxFile(), "202601", "家樂福", 1L, "sales"));
    }

    @Test
    void upload_invalidProductCode_throwsExcelParseException() {
        SalesForecastConfig cfg = new SalesForecastConfig();
        cfg.setIsClosed(false);
        when(configRepository.findByMonth("202601")).thenReturn(Optional.of(cfg));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq("家樂福"))).thenReturn(1);
        when(excelParserService.parse(any())).thenReturn(List.of(
                new SalesForecastRow("C", "S", "BAD", "N", "A01", new BigDecimal("10"), "", 2)
        ));
        when(erpProductService.validateProduct("BAD")).thenReturn(false);

        ExcelParseException ex = assertThrows(ExcelParseException.class,
                () -> service.upload(xlsxFile(), "202601", "家樂福", 1L, "sales"));
        assertTrue(ex.getMessage().contains("BAD"));
    }

    @Test
    void upload_productMasterOverridesUploadedFields() {
        SalesForecastConfig cfg = new SalesForecastConfig();
        cfg.setIsClosed(false);
        when(configRepository.findByMonth("202601")).thenReturn(Optional.of(cfg));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq("家樂福"))).thenReturn(1);
        when(excelParserService.parse(any())).thenReturn(List.of(
                new SalesForecastRow("uploaded-category", "uploaded-spec", "P1", "uploaded-name", "uploaded-wh", new BigDecimal("10"), "", 2)
        ));
        when(erpProductService.validateProduct("P1")).thenReturn(true);
        ProductDTO p = new ProductDTO();
        p.setCode("P1");
        p.setName("db-name");
        p.setCategoryName("db-category");
        p.setSpec("db-spec");
        p.setWarehouseLocation("db-wh");
        when(erpProductService.findProduct("P1")).thenReturn(Optional.of(p));

        service.upload(xlsxFile(), "202601", "家樂福", 1L, "sales");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GiftSalesForecast>> captor = ArgumentCaptor.forClass(List.class);
        verify(forecastRepository).saveAll(captor.capture());
        GiftSalesForecast saved = captor.getValue().get(0);
        assertEquals("db-category", saved.getCategory());
        assertEquals("db-spec", saved.getSpec());
        assertEquals("db-name", saved.getProductName());
        assertEquals("db-wh", saved.getWarehouseLocation());
    }

    private MockMultipartFile xlsxFile() {
        return new MockMultipartFile(
                "file",
                "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy".getBytes()
        );
    }

    private MockMultipartFile csvFile() {
        return new MockMultipartFile("file", "test.csv", "text/csv", "a,b".getBytes());
    }

    private MockMultipartFile txtFile() {
        return new MockMultipartFile("file", "test.txt", "text/plain", "x".getBytes());
    }
}
