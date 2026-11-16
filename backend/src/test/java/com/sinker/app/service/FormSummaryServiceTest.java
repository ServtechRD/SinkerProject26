package com.sinker.app.service;

import com.sinker.app.dto.forecast.ChannelCellDTO;
import com.sinker.app.dto.forecast.ChannelRemarkDTO;
import com.sinker.app.dto.forecast.FormSummaryRowDTO;
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
import org.mockito.ArgumentCaptor;
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
    @Mock
    private FormLogService formLogService;

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
                () -> service.saveFormSummaryVersion(MONTH, req, 1L)
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
        row.setRemarks(List.of(new ChannelRemarkDTO("7-11", MONTH, "row remark")));
        row.setChannelQuantities(new ArrayList<>(List.of(new BigDecimal("22"))));
        req.setRows(List.of(row));

        int versionNo = service.saveFormSummaryVersion(MONTH, req, 1L);

        assertEquals(3, versionNo);
        verify(formVersionRepository).save(argThat(v -> v.getVersionNo() == 3 && "new reason".equals(v.getChangeReason())));
        verify(forecastRepository, times(12)).save(any(SalesForecast.class));
        verify(formLogService).record(eq(MONTH), isNull(),
                eq(com.sinker.app.entity.FormLogAction.CORRECTION), eq(1L), contains("form_v" + versionNo));
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

    // F126：同品項多通路備註皆保留，前一通路無備註時後一通路的備註也不能遺失
    @Test
    void createFormVersion1Snapshot_keepsRemarkPerChannel() {
        SalesForecastConfig config = closedConfig();
        when(formVersionRepository.countByMonth(MONTH)).thenReturn(0L);
        when(forecastRepository.findDistinctVersionsByMonthAndChannel(anyString(), anyString())).thenReturn(List.of());
        when(giftForecastRepository.findLatestByMonthAndChannel(anyString(), anyString())).thenReturn(List.of());
        // P001：PX 與 7-11 各有備註；P002：PX 無備註、7-11 有備註
        when(forecastRepository.findDistinctVersionsByMonthAndChannel(MONTH, CH)).thenReturn(List.of("vPX"));
        when(forecastRepository.findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(MONTH, CH, "vPX"))
                .thenReturn(List.of(
                        sales(MONTH, CH, "vPX", null, "P001", BigDecimal.ONE, "PX 備註"),
                        sales(MONTH, CH, "vPX", null, "P002", BigDecimal.ONE, "")));
        when(forecastRepository.findDistinctVersionsByMonthAndChannel(MONTH, "7-11")).thenReturn(List.of("v711"));
        when(forecastRepository.findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(MONTH, "7-11", "v711"))
                .thenReturn(List.of(
                        sales(MONTH, "7-11", "v711", null, "P001", BigDecimal.ONE, "711 備註"),
                        sales(MONTH, "7-11", "v711", null, "P002", BigDecimal.ONE, "9/30前需生產完成")));
        GiftSalesForecast g = gift("7-11", "P001", BigDecimal.ONE);
        g.setRemark("禮品備註");
        when(giftForecastRepository.findLatestByMonthAndChannel(MONTH, "7-11")).thenReturn(List.of(g));

        service.createFormVersion1Snapshot(MONTH, config);

        ArgumentCaptor<SalesForecast> captor = ArgumentCaptor.forClass(SalesForecast.class);
        verify(forecastRepository, times(24)).save(captor.capture());
        assertEquals("PX 備註", remarkOf(captor.getAllValues(), CH, "P001"));
        assertEquals("711 備註；禮品備註", remarkOf(captor.getAllValues(), "7-11", "P001"));
        assertEquals("", remarkOf(captor.getAllValues(), CH, "P002"));
        assertEquals("9/30前需生產完成", remarkOf(captor.getAllValues(), "7-11", "P002"));
    }

    @Test
    void getFormSummaryByVersion_returnsRemarksPerChannelInChannelOrder() {
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(closedConfig()));
        doNothing().when(service).ensureFormVersion1Exists(eq(MONTH), any(SalesForecastConfig.class));
        when(formVersionRepository.findByMonthOrderByVersionNoDesc(MONTH)).thenReturn(List.of(formVer(2, null)));
        // repository 依通路名稱排序回傳，7-11 會排在 PX 前面；輸出需依 CHANNEL_ORDER
        when(forecastRepository.findByMonthAndFormVersionNoOrderByChannelCategorySpecProductCode(MONTH, 2))
                .thenReturn(List.of(
                        sales(MONTH, "7-11", "form_v2", 2, "P001", BigDecimal.ONE, "711 備註"),
                        sales(MONTH, CH, "form_v2", 2, "P001", BigDecimal.ONE, "PX 備註"),
                        sales(MONTH, "全家", "form_v2", 2, "P001", BigDecimal.ONE, "")));
        when(forecastRepository.findByMonthAndFormVersionNoOrderByChannelCategorySpecProductCode(MONTH, 1))
                .thenReturn(List.of());

        FormSummaryResponse resp = service.getFormSummaryByVersion(MONTH, 2);

        List<ChannelRemarkDTO> remarks = resp.getRows().get(0).getRemarks();
        assertEquals(2, remarks.size());
        assertEquals(CH, remarks.get(0).getChannel());
        assertEquals("PX 備註", remarks.get(0).getText());
        assertEquals(MONTH, remarks.get(0).getMonth());
        assertEquals("7-11", remarks.get(1).getChannel());
        assertEquals("711 備註", remarks.get(1).getText());
    }

    @Test
    void getFormSummaryLegacy_includesGiftRemarkWithChannel() {
        when(forecastRepository.findDistinctVersionsByMonthAndChannel(anyString(), anyString())).thenReturn(List.of());
        when(giftForecastRepository.findLatestByMonthAndChannel(anyString(), anyString())).thenReturn(List.of());
        GiftSalesForecast g = gift("全家", "P009", BigDecimal.ONE);
        g.setRemark("禮盒改版");
        when(giftForecastRepository.findLatestByMonthAndChannel(MONTH, "全家")).thenReturn(List.of(g));

        FormSummaryResponse resp = service.getFormSummaryLegacy(MONTH);

        List<ChannelRemarkDTO> remarks = resp.getRows().get(0).getRemarks();
        assertEquals(1, remarks.size());
        assertEquals("全家", remarks.get(0).getChannel());
        assertEquals("禮盒改版", remarks.get(0).getText());
    }

    @Test
    void saveFormSummaryVersion_writesEachChannelRemarkToItsOwnRow() {
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(closedConfig()));
        doNothing().when(service).ensureFormVersion1Exists(eq(MONTH), any(SalesForecastConfig.class));
        when(formVersionRepository.findByMonthOrderByVersionNoDesc(MONTH)).thenReturn(List.of(formVer(1, null)));

        SaveFormSummaryVersionRequest.FormSummaryRowEditDTO row = new SaveFormSummaryVersionRequest.FormSummaryRowEditDTO();
        row.setProductCode("P001");
        row.setRemarks(List.of(
                new ChannelRemarkDTO(CH, MONTH, "PX 原備註"),
                new ChannelRemarkDTO("7-11", null, "  711 修改後  "),
                new ChannelRemarkDTO("全家", MONTH, "   ")));
        SaveFormSummaryVersionRequest req = new SaveFormSummaryVersionRequest();
        req.setRows(List.of(row));

        service.saveFormSummaryVersion(MONTH, req, 1L);

        ArgumentCaptor<SalesForecast> captor = ArgumentCaptor.forClass(SalesForecast.class);
        verify(forecastRepository, times(12)).save(captor.capture());
        assertEquals("PX 原備註", remarkOf(captor.getAllValues(), CH, "P001"));
        assertEquals("711 修改後", remarkOf(captor.getAllValues(), "7-11", "P001"));
        assertEquals("", remarkOf(captor.getAllValues(), "全家", "P001"));
        assertEquals("", remarkOf(captor.getAllValues(), "家樂福", "P001"));
    }

    // F127：跨月彙整，已關帳取最新表單版本、未關帳取各通路最新上傳，依品號合併
    @Test
    void getFormSummaryRange_mergesClosedAndOpenMonthsByProductCode() {
        SalesForecastConfig nov = closedConfig();
        nov.setMonth("202611");
        SalesForecastConfig dec = new SalesForecastConfig();
        dec.setMonth("202612");
        dec.setIsClosed(false);
        when(configRepository.findByMonth("202611")).thenReturn(Optional.of(nov));
        when(configRepository.findByMonth("202612")).thenReturn(Optional.of(dec));
        when(configRepository.findByMonth("202701")).thenReturn(Optional.empty());
        when(formVersionRepository.findByMonthOrderByVersionNoDesc("202611"))
                .thenReturn(List.of(formVer(2, null), formVer(1, null)));

        FormSummaryResponse novSummary = summaryOf(
                summaryRow("P001", "舊品名", "10", "5", List.of(
                        new ChannelRemarkDTO("7-11", "202611", "11月 7-11"),
                        new ChannelRemarkDTO(CH, "202611", "11月 PX"))));
        FormSummaryResponse decSummary = summaryOf(
                summaryRow("P001", "新品名", "20", "0", List.of(new ChannelRemarkDTO(CH, "202612", "12月 PX"))),
                summaryRow("P002", "只有12月", "7", "0", List.of()));
        doReturn(novSummary).when(service).getFormSummaryByVersion("202611", 2);
        doReturn(decSummary).when(service).getFormSummaryLegacy("202612");
        doReturn(summaryOf()).when(service).getFormSummaryLegacy("202701");

        var resp = service.getFormSummaryRange("202611", "202701");

        assertEquals(List.of("202611", "202612", "202701"),
                resp.getMonths().stream().map(m -> m.getMonth()).toList());
        assertTrue(resp.getMonths().get(0).isClosed());
        assertFalse(resp.getMonths().get(1).isClosed());
        assertFalse(resp.getMonths().get(2).isClosed());

        assertEquals(2, resp.getRows().size());
        var p001 = resp.getRows().stream().filter(r -> "P001".equals(r.getProductCode())).findFirst().orElseThrow();
        assertEquals("新品名", p001.getProductName());
        assertEquals(new BigDecimal("15"), p001.getMonthQuantities().get("202611"));
        assertEquals(new BigDecimal("20"), p001.getMonthQuantities().get("202612"));
        assertEquals(BigDecimal.ZERO, p001.getMonthQuantities().get("202701"));
        assertEquals(new BigDecimal("35"), p001.getTotal());
        // 備註先依月份，同月內維持單月彙總已排好的通路順序
        assertEquals(List.of("11月 7-11", "11月 PX", "12月 PX"),
                p001.getRemarks().stream().map(ChannelRemarkDTO::getText).toList());

        var p002 = resp.getRows().stream().filter(r -> "P002".equals(r.getProductCode())).findFirst().orElseThrow();
        assertEquals(BigDecimal.ZERO, p002.getMonthQuantities().get("202611"));
        assertEquals(new BigDecimal("7"), p002.getTotal());
    }

    @Test
    void getFormSummaryRange_closedMonthWithoutVersion_usesVersion1() {
        SalesForecastConfig cfg = closedConfig();
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(cfg));
        when(formVersionRepository.findByMonthOrderByVersionNoDesc(MONTH)).thenReturn(List.of());
        doReturn(summaryOf()).when(service).getFormSummaryByVersion(MONTH, 1);

        service.getFormSummaryRange(MONTH, MONTH);

        verify(service).getFormSummaryByVersion(MONTH, 1);
    }

    @Test
    void getFormSummaryRange_invalidRange_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.getFormSummaryRange("202611", "202703"));
        assertThrows(IllegalArgumentException.class, () -> service.getFormSummaryRange("202612", "202611"));
        assertThrows(IllegalArgumentException.class, () -> service.getFormSummaryRange("2026-11", "202612"));
        verifyNoInteractions(configRepository);
    }

    private static FormSummaryResponse summaryOf(FormSummaryRowDTO... rows) {
        FormSummaryResponse r = new FormSummaryResponse();
        r.setRows(new ArrayList<>(List.of(rows)));
        return r;
    }

    private static FormSummaryRowDTO summaryRow(String code, String name, String qtyCh1, String qtyCh2, List<ChannelRemarkDTO> remarks) {
        FormSummaryRowDTO row = new FormSummaryRowDTO();
        row.setProductCode(code);
        row.setProductName(name);
        ChannelCellDTO c1 = new ChannelCellDTO();
        c1.setCurrentQty(new BigDecimal(qtyCh1));
        ChannelCellDTO c2 = new ChannelCellDTO();
        c2.setCurrentQty(new BigDecimal(qtyCh2));
        row.setChannelCells(List.of(c1, c2));
        row.setRemarks(remarks);
        return row;
    }

    private static String remarkOf(List<SalesForecast> saved, String channel, String code) {
        return saved.stream()
                .filter(s -> channel.equals(s.getChannel()) && code.equals(s.getProductCode()))
                .findFirst().orElseThrow().getRemark();
    }

    private SalesForecast sales(String month, String channel, String version, Integer formVersionNo, String code, BigDecimal qty, String remark) {
        SalesForecast s = sales(month, channel, version, formVersionNo, code, qty);
        s.setRemark(remark);
        return s;
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
