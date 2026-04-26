package com.sinker.app.service;

import com.sinker.app.dto.forecast.ChannelCellDTO;
import com.sinker.app.dto.forecast.FormSummaryResponse;
import com.sinker.app.dto.forecast.FormSummaryRowDTO;
import com.sinker.app.dto.productionplan.ProductionFormRangeResponse;
import com.sinker.app.dto.productionplan.ProductionFormRowDTO;
import com.sinker.app.entity.InventorySalesForecast;
import com.sinker.app.entity.ProductionForm;
import com.sinker.app.entity.SalesForecast;
import com.sinker.app.entity.SalesForecastFormVersion;
import com.sinker.app.repository.InventorySalesForecastRepository;
import com.sinker.app.repository.ProductionFormRepository;
import com.sinker.app.repository.SalesForecastFormVersionRepository;
import com.sinker.app.repository.SalesForecastRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionFormServiceTest {
    private static final String CH = "PX + 大全聯";

    @Mock
    private SalesForecastRepository salesForecastRepository;
    @Mock
    private InventorySalesForecastRepository inventoryForecastRepository;
    @Mock
    private ProductionFormRepository productionFormRepository;
    @Mock
    private FormSummaryService formSummaryService;
    @Mock
    private SalesForecastFormVersionRepository formVersionRepository;

    @InjectMocks
    private ProductionFormService service;

    @Test
    void listInventoryVersionsInRange_invalidFormat_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.listInventoryVersionsInRange("2026-01", "202604"));
    }

    @Test
    void listInventoryVersionsInRange_overFourMonths_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.listInventoryVersionsInRange("202601", "202606"));
        assertTrue(ex.getMessage().contains("4"));
    }

    @Test
    void getProductionFormByMonthRange_selectedVersionNotInRange_throws() {
        when(inventoryForecastRepository.findDistinctVersionsByMonthBetween("202601", "202603"))
                .thenReturn(List.of("v3", "v2"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.getProductionFormByMonthRange("202601", "202603", "v1"));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void getProductionFormByMonthRange_happyPath_returnsRows() {
        when(inventoryForecastRepository.findDistinctVersionsByMonthBetween("202601", "202601"))
                .thenReturn(List.of("v1"));

        InventorySalesForecast inv = new InventorySalesForecast();
        inv.setProductCode("P001");
        inv.setWarehouseLocation("A01");
        inv.setCategory("C");
        inv.setSpec("S");
        inv.setProductName("N");
        when(inventoryForecastRepository.findByMonthAndVersionOrderByProductCodeAsc("202601", "v1"))
                .thenReturn(List.of(inv));

        SalesForecastFormVersion fv = new SalesForecastFormVersion();
        fv.setVersionNo(2);
        when(formVersionRepository.findByMonthOrderByVersionNoDesc("202601")).thenReturn(List.of(fv));

        FormSummaryResponse fs = new FormSummaryResponse();
        fs.setRows(List.of());
        when(formSummaryService.getFormSummary("202601", 2)).thenReturn(fs);

        var resp = service.getProductionFormByMonthRange("202601", "202601", "v1");

        assertEquals(List.of("202601"), resp.getMonthKeys());
        assertEquals(List.of("v1"), resp.getVersions());
        assertEquals(1, resp.getRows().size());
        assertEquals("P001", resp.getRows().get(0).getProductCode());
    }

    @Test
    void getProductionForm_buildsRowFromSalesInventoryAndBuffer() {
        when(productionFormRepository.findByYearOrderByProductCodeAsc(2026)).thenReturn(List.of(savedForm("P001", "5", "note", 88)));

        when(salesForecastRepository.findDistinctVersionsByMonth(anyString())).thenReturn(List.of());
        when(inventoryForecastRepository.findDistinctVersionsByMonth(anyString())).thenReturn(List.of());

        when(salesForecastRepository.findDistinctVersionsByMonth("202602")).thenReturn(List.of("sv2"));
        when(salesForecastRepository.findByMonthAndVersion("202602", "sv2"))
                .thenReturn(List.of(sales("202602", CH, "P001", "11")));

        when(inventoryForecastRepository.findDistinctVersionsByMonth("202602")).thenReturn(List.of("iv2"));
        when(inventoryForecastRepository.findByMonthAndVersionOrderByProductCodeAsc("202602", "iv2"))
                .thenReturn(List.of(inv("202602", "P001", "9")));

        List<ProductionFormRowDTO> rows = service.getProductionForm(2026);

        assertEquals(1, rows.size());
        ProductionFormRowDTO row = rows.get(0);
        assertEquals("P001", row.getProductCode());
        assertEquals(new BigDecimal("11"), row.getOriginalForecast());
        assertEquals(new BigDecimal("5"), row.getBufferQuantity());
        assertEquals(new BigDecimal("14"), row.getAggregateTotal()); // 9 + 5
        assertEquals(new BigDecimal("-3"), row.getDifference()); // 11 - 14
        assertEquals("note", row.getRemarks());
        assertEquals(88, row.getProductionFormId());
    }

    @Test
    void getProductionForm_inventoryModifiedSubtotalNull_usesZero() {
        when(productionFormRepository.findByYearOrderByProductCodeAsc(2026)).thenReturn(List.of());
        when(salesForecastRepository.findDistinctVersionsByMonth(anyString())).thenReturn(List.of());
        when(inventoryForecastRepository.findDistinctVersionsByMonth(anyString())).thenReturn(List.of());

        when(inventoryForecastRepository.findDistinctVersionsByMonth("202602")).thenReturn(List.of("iv2"));
        InventorySalesForecast inv = inv("202602", "P001", null);
        when(inventoryForecastRepository.findByMonthAndVersionOrderByProductCodeAsc("202602", "iv2"))
                .thenReturn(List.of(inv));

        List<ProductionFormRowDTO> rows = service.getProductionForm(2026);

        assertEquals(1, rows.size());
        ProductionFormRowDTO row = rows.get(0);
        assertEquals(BigDecimal.ZERO, row.getAggregateTotal());
        assertEquals(BigDecimal.ZERO, row.getOriginalForecast());
        assertEquals(BigDecimal.ZERO, row.getDifference());
    }

    @Test
    void getProductionFormByMonthRange_emptyInventoryVersion_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getProductionFormByMonthRange("202601", "202602", ""));
    }

    @Test
    void getProductionFormByMonthRange_aggregatesFromFormSummaryAcrossMonths() {
        when(inventoryForecastRepository.findDistinctVersionsByMonthBetween("202601", "202602"))
                .thenReturn(List.of("inv-v2"));
        when(inventoryForecastRepository.findByMonthAndVersionOrderByProductCodeAsc("202601", "inv-v2"))
                .thenReturn(List.of(inv("202601", "P001", "1")));
        when(inventoryForecastRepository.findByMonthAndVersionOrderByProductCodeAsc("202602", "inv-v2"))
                .thenReturn(List.of(inv("202602", "P001", "2")));

        when(formVersionRepository.findByMonthOrderByVersionNoDesc("202601")).thenReturn(List.of(formVersion(2)));
        when(formVersionRepository.findByMonthOrderByVersionNoDesc("202602")).thenReturn(List.of(formVersion(3)));

        when(formSummaryService.getFormSummary("202601", 2)).thenReturn(summary("P001", "5", "3", "remark-601"));
        when(formSummaryService.getFormSummary("202602", 3)).thenReturn(summary("P001", "7", "6", "remark-602"));

        ProductionFormRangeResponse resp = service.getProductionFormByMonthRange("202601", "202602", "inv-v2");

        assertEquals(List.of("202601", "202602"), resp.getMonthKeys());
        assertEquals(1, resp.getRows().size());
        ProductionFormRowDTO row = resp.getRows().get(0);
        assertEquals("P001", row.getProductCode());
        assertEquals(new BigDecimal("12"), row.getAggregateTotal()); // 5 + 7
        assertEquals(new BigDecimal("9"), row.getOriginalForecast()); // 3 + 6
        assertEquals(new BigDecimal("-3.00"), row.getDifference()); // (3-5)+(6-7)
        assertEquals("remark-601", row.getRemarks()); // first remark kept
    }

    @Test
    void getProductionFormByMonthRange_formSummaryNullRows_handlesGracefully() {
        when(inventoryForecastRepository.findDistinctVersionsByMonthBetween("202601", "202601"))
                .thenReturn(List.of("inv-v1"));
        when(inventoryForecastRepository.findByMonthAndVersionOrderByProductCodeAsc("202601", "inv-v1"))
                .thenReturn(List.of(inv("202601", "P001", "2")));
        when(formVersionRepository.findByMonthOrderByVersionNoDesc("202601")).thenReturn(List.of(formVersion(1)));
        when(formSummaryService.getFormSummary("202601", 1)).thenReturn(new FormSummaryResponse());

        ProductionFormRangeResponse resp = service.getProductionFormByMonthRange("202601", "202601", "inv-v1");

        assertEquals(1, resp.getRows().size());
        ProductionFormRowDTO row = resp.getRows().get(0);
        assertEquals(BigDecimal.ZERO, row.getAggregateTotal());
        assertEquals(BigDecimal.ZERO, row.getOriginalForecast());
        assertEquals(BigDecimal.ZERO, row.getDifference());
    }

    @Test
    void updateBuffer_existingRow_updatesAndPersists() {
        ProductionForm existing = new ProductionForm();
        existing.setYear(2026);
        existing.setProductCode("P001");
        existing.setBufferQuantity(new BigDecimal("1"));
        when(productionFormRepository.findByYearAndProductCode(2026, "P001")).thenReturn(Optional.of(existing));

        service.updateBuffer(2026, "P001", new BigDecimal("99.5"));

        assertEquals(new BigDecimal("99.5"), existing.getBufferQuantity());
        verify(productionFormRepository).save(existing);
    }

    private SalesForecast sales(String month, String channel, String productCode, String qty) {
        SalesForecast s = new SalesForecast();
        s.setMonth(month);
        s.setChannel(channel);
        s.setProductCode(productCode);
        s.setWarehouseLocation("A01");
        s.setCategory("C");
        s.setSpec("S");
        s.setProductName("N");
        s.setQuantity(new BigDecimal(qty));
        return s;
    }

    private InventorySalesForecast inv(String month, String productCode, String modifiedSubtotal) {
        InventorySalesForecast i = new InventorySalesForecast();
        i.setMonth(month);
        i.setProductCode(productCode);
        i.setWarehouseLocation("A01");
        i.setCategory("C");
        i.setSpec("S");
        i.setProductName("N");
        i.setModifiedSubtotal(modifiedSubtotal == null ? null : new BigDecimal(modifiedSubtotal));
        return i;
    }

    private ProductionForm savedForm(String productCode, String buffer, String remarks, int id) {
        ProductionForm f = new ProductionForm();
        f.setId(id);
        f.setYear(2026);
        f.setProductCode(productCode);
        f.setBufferQuantity(new BigDecimal(buffer));
        f.setRemarks(remarks);
        return f;
    }

    private SalesForecastFormVersion formVersion(int no) {
        SalesForecastFormVersion v = new SalesForecastFormVersion();
        v.setVersionNo(no);
        return v;
    }

    private FormSummaryResponse summary(String productCode, String curr, String prev, String remark) {
        ChannelCellDTO cell = new ChannelCellDTO();
        cell.setCurrentQty(new BigDecimal(curr));
        cell.setPreviousQty(new BigDecimal(prev));
        cell.setRemark("cell");

        FormSummaryRowDTO row = new FormSummaryRowDTO();
        row.setWarehouseLocation("A01");
        row.setCategory("C");
        row.setSpec("S");
        row.setProductName("N");
        row.setProductCode(productCode);
        row.setChannelCells(List.of(cell));

        FormSummaryResponse resp = new FormSummaryResponse();
        resp.setRows(List.of(row));
        resp.setVersionRemark(remark);
        return resp;
    }
}
