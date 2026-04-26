package com.sinker.app.service;

import com.sinker.app.dto.forecast.FormVersionListItemDTO;
import com.sinker.app.dto.forecast.FormSummaryResponse;
import com.sinker.app.dto.forecast.SaveFormSummaryVersionRequest;
import com.sinker.app.entity.GiftSalesForecast;
import com.sinker.app.entity.SalesForecast;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.entity.SalesForecastFormVersion;
import com.sinker.app.entity.SalesForecastVersionReason;
import com.sinker.app.repository.GiftSalesForecastRepository;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastFormVersionRepository;
import com.sinker.app.repository.SalesForecastRepository;
import com.sinker.app.repository.SalesForecastVersionReasonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormSummaryServiceTest {
    private static final String MONTH = "202601";
    private static final String CH = "PX + 大全聯";

    @Mock
    private SalesForecastRepository forecastRepository;
    @Mock
    private SalesForecastVersionReasonRepository versionReasonRepository;
    @Mock
    private SalesForecastConfigRepository configRepository;
    @Mock
    private SalesForecastFormVersionRepository formVersionRepository;
    @Mock
    private GiftSalesForecastRepository giftForecastRepository;

    @Spy
    @InjectMocks
    private FormSummaryService service;

    @Test
    void listFormVersions_openMonth_returnsEmptyList() {
        SalesForecastConfig config = new SalesForecastConfig();
        config.setIsClosed(false);
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(config));

        List<FormVersionListItemDTO> result = service.listFormVersions(MONTH);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(formVersionRepository, never()).findByMonthOrderByVersionNoDesc(anyString());
    }

    @Test
    void listFormVersions_closedMonth_formatsCreatedAtDisplay() {
        SalesForecastConfig config = new SalesForecastConfig();
        config.setIsClosed(true);
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(config));
        doNothing().when(service).ensureFormVersion1Exists(eq(MONTH), any(SalesForecastConfig.class));

        SalesForecastFormVersion v = new SalesForecastFormVersion();
        v.setVersionNo(2);
        v.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0, 0)); // UTC -> Taipei +8
        v.setChangeReason("reason");
        when(formVersionRepository.findByMonthOrderByVersionNoDesc(MONTH)).thenReturn(List.of(v));

        List<FormVersionListItemDTO> result = service.listFormVersions(MONTH);

        assertEquals(1, result.size());
        assertEquals("2026-01-01 08:00:00", result.get(0).getCreatedAtDisplay());
        assertEquals("reason", result.get(0).getChangeReason());
    }

    @Test
    void getFormSummaryByVersion_monthNotClosed_throws() {
        SalesForecastConfig config = new SalesForecastConfig();
        config.setIsClosed(false);
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(config));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.getFormSummaryByVersion(MONTH, 1)
        );
        assertTrue(ex.getMessage().contains("not closed"));
    }

    @Test
    void saveFormSummaryVersion_monthNotClosed_throws() {
        SalesForecastConfig config = new SalesForecastConfig();
        config.setIsClosed(false);
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(config));

        SaveFormSummaryVersionRequest req = new SaveFormSummaryVersionRequest();
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.saveFormSummaryVersion(MONTH, req)
        );
        assertTrue(ex.getMessage().contains("not closed"));
        verify(formVersionRepository, never()).save(any());
    }

    @Test
    void getFormSummaryByVersion_aggregatedVersion_buildsCellsWithPreviousAndRemark() {
        SalesForecastConfig config = closedConfig();
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(config));
        doNothing().when(service).ensureFormVersion1Exists(eq(MONTH), any(SalesForecastConfig.class));
        when(formVersionRepository.findByMonthOrderByVersionNoDesc(MONTH)).thenReturn(List.of(formVer(2, "調整原因")));

        SalesForecast curr = sales(MONTH, CH, "form_v2", 2, "P001", new BigDecimal("10"));
        SalesForecast prev = sales(MONTH, CH, "form_v1", 1, "P001", new BigDecimal("7"));
        when(forecastRepository.findByMonthAndFormVersionNoOrderByChannelCategorySpecProductCode(MONTH, 2))
                .thenReturn(List.of(curr));
        when(forecastRepository.findByMonthAndFormVersionNoOrderByChannelCategorySpecProductCode(MONTH, 1))
                .thenReturn(List.of(prev));

        FormSummaryResponse resp = service.getFormSummaryByVersion(MONTH, 2);

        assertEquals(2, resp.getVersionNo());
        assertEquals("調整原因", resp.getVersionRemark());
        assertEquals(1, resp.getRows().size());
        assertEquals(new BigDecimal("7"), resp.getRows().get(0).getChannelCells().get(0).getPreviousQty());
        assertEquals(new BigDecimal("10"), resp.getRows().get(0).getChannelCells().get(0).getCurrentQty());
    }

    @Test
    void getFormSummaryByVersion_nonAggregatedVersion1_includesGiftAndSetsPreviousEqualCurrent() {
        SalesForecastConfig config = closedConfig();
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(config));
        doNothing().when(service).ensureFormVersion1Exists(eq(MONTH), any(SalesForecastConfig.class));
        when(formVersionRepository.findByMonthOrderByVersionNoDesc(MONTH)).thenReturn(List.of(formVer(1, null)));

        SalesForecast curr = sales(MONTH, CH, "2026/01/01 10:00:00(PX + 大全聯)", 1, "P001", new BigDecimal("10"));
        when(forecastRepository.findByMonthAndFormVersionNoOrderByChannelCategorySpecProductCode(MONTH, 1))
                .thenReturn(List.of(curr));
        when(giftForecastRepository.findLatestByMonthAndChannel(anyString(), anyString())).thenReturn(List.of());
        GiftSalesForecast gift = gift(CH, "P001", new BigDecimal("3"));
        when(giftForecastRepository.findLatestByMonthAndChannel(MONTH, CH)).thenReturn(List.of(gift));

        FormSummaryResponse resp = service.getFormSummaryByVersion(MONTH, 1);

        var cell = resp.getRows().get(0).getChannelCells().get(0);
        assertEquals(new BigDecimal("13"), cell.getCurrentQty());
        assertEquals(new BigDecimal("13"), cell.getPreviousQty());
        assertEquals(new BigDecimal("10"), cell.getCurrentSalesQty());
    }

    @Test
    void getFormSummaryLegacy_combinesSalesAndGiftAndVersionReason() {
        when(forecastRepository.findDistinctVersionsByMonthAndChannel(anyString(), anyString())).thenReturn(List.of());
        when(giftForecastRepository.findLatestByMonthAndChannel(anyString(), anyString())).thenReturn(List.of());
        when(forecastRepository.findDistinctVersionsByMonthAndChannel(MONTH, CH)).thenReturn(List.of("v2", "v1"));
        when(forecastRepository.findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(MONTH, CH, "v2"))
                .thenReturn(List.of(sales(MONTH, CH, "v2", null, "P001", new BigDecimal("9"))));
        when(forecastRepository.findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(MONTH, CH, "v1"))
                .thenReturn(List.of(sales(MONTH, CH, "v1", null, "P001", new BigDecimal("5"))));
        SalesForecastVersionReason reason = new SalesForecastVersionReason();
        reason.setChangeReason("v2 reason");
        when(versionReasonRepository.findByMonthAndChannelAndVersion(MONTH, CH, "v2")).thenReturn(Optional.of(reason));
        when(giftForecastRepository.findLatestByMonthAndChannel(MONTH, CH))
                .thenReturn(List.of(gift(CH, "P001", new BigDecimal("1"))));

        FormSummaryResponse resp = service.getFormSummaryLegacy(MONTH);

        assertEquals(1, resp.getRows().size());
        var cell = resp.getRows().get(0).getChannelCells().get(0);
        assertEquals(new BigDecimal("10"), cell.getCurrentQty());
        assertEquals(new BigDecimal("5"), cell.getPreviousQty());
        assertEquals("v2 reason", cell.getRemark());
    }

    @Test
    void saveFormSummaryVersion_persistsNewVersionAndRows() {
        SalesForecastConfig config = closedConfig();
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(config));
        doNothing().when(service).ensureFormVersion1Exists(eq(MONTH), any(SalesForecastConfig.class));
        when(formVersionRepository.findByMonthOrderByVersionNoDesc(MONTH)).thenReturn(List.of(formVer(2, "old")));

        SaveFormSummaryVersionRequest req = new SaveFormSummaryVersionRequest();
        req.setChangeReason("new reason");
        SaveFormSummaryVersionRequest.FormSummaryRowEditDTO row = new SaveFormSummaryVersionRequest.FormSummaryRowEditDTO();
        row.setWarehouseLocation("A01");
        row.setCategory("C");
        row.setSpec("S");
        row.setProductName("N");
        row.setProductCode("P001");
        row.setRemark("row remark");
        row.setChannelQuantities(new ArrayList<>(List.of(new BigDecimal("22"))));
        req.setRows(List.of(row));

        int versionNo = service.saveFormSummaryVersion(MONTH, req);

        assertEquals(3, versionNo);
        verify(formVersionRepository).save(argThat(v -> v.getVersionNo() == 3 && "new reason".equals(v.getChangeReason())));
        verify(forecastRepository, times(12)).save(any(SalesForecast.class));
    }

    @Test
    void createFormVersion1Snapshot_notClosed_throws() {
        SalesForecastConfig config = new SalesForecastConfig();
        config.setIsClosed(false);
        assertThrows(IllegalArgumentException.class, () -> service.createFormVersion1Snapshot(MONTH, config));
    }

    @Test
    void createFormVersion1Snapshot_existingVersion_skipsInsert() {
        SalesForecastConfig config = closedConfig();
        when(formVersionRepository.countByMonth(MONTH)).thenReturn(1L);

        service.createFormVersion1Snapshot(MONTH, config);

        verify(formVersionRepository, never()).save(any(SalesForecastFormVersion.class));
        verify(forecastRepository, never()).save(any(SalesForecast.class));
    }

    @Test
    void createFormVersion1Snapshot_mergesSalesAndGiftAndCreatesFormRows() {
        SalesForecastConfig config = closedConfig();
        config.setClosedAt(LocalDateTime.of(2026, 1, 20, 10, 0));
        when(formVersionRepository.countByMonth(MONTH)).thenReturn(0L);
        when(forecastRepository.findDistinctVersionsByMonthAndChannel(anyString(), anyString())).thenReturn(List.of());
        when(giftForecastRepository.findLatestByMonthAndChannel(anyString(), anyString())).thenReturn(List.of());
        when(forecastRepository.findDistinctVersionsByMonthAndChannel(MONTH, CH)).thenReturn(List.of("v2"));
        when(forecastRepository.findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(MONTH, CH, "v2"))
                .thenReturn(List.of(sales(MONTH, CH, "v2", null, "P001", new BigDecimal("10"))));
        when(giftForecastRepository.findLatestByMonthAndChannel(MONTH, CH))
                .thenReturn(List.of(gift(CH, "P001", new BigDecimal("2"))));

        service.createFormVersion1Snapshot(MONTH, config);

        verify(formVersionRepository).save(argThat(v -> v.getVersionNo() == 1 && MONTH.equals(v.getMonth())));
        verify(forecastRepository, times(12)).save(any(SalesForecast.class));
    }

    private SalesForecastConfig closedConfig() {
        SalesForecastConfig cfg = new SalesForecastConfig();
        cfg.setMonth(MONTH);
        cfg.setIsClosed(true);
        return cfg;
    }

    private SalesForecastFormVersion formVer(int no, String reason) {
        SalesForecastFormVersion v = new SalesForecastFormVersion();
        v.setVersionNo(no);
        v.setChangeReason(reason);
        v.setCreatedAt(LocalDateTime.now());
        return v;
    }

    private SalesForecast sales(String month, String channel, String version, Integer formVersionNo, String code, BigDecimal qty) {
        SalesForecast s = new SalesForecast();
        s.setMonth(month);
        s.setChannel(channel);
        s.setVersion(version);
        s.setFormVersionNo(formVersionNo);
        s.setWarehouseLocation("A01");
        s.setCategory("C");
        s.setSpec("S");
        s.setProductName("N");
        s.setProductCode(code);
        s.setQuantity(qty);
        s.setRemark("r");
        return s;
    }

    private GiftSalesForecast gift(String channel, String code, BigDecimal qty) {
        GiftSalesForecast g = new GiftSalesForecast();
        g.setMonth(MONTH);
        g.setChannel(channel);
        g.setWarehouseLocation("A01");
        g.setCategory("C");
        g.setSpec("S");
        g.setProductName("N");
        g.setProductCode(code);
        g.setQuantity(qty);
        return g;
    }
}
