package com.sinker.app.service;

import com.sinker.app.dto.forecast.CopyVersionResponse;
import com.sinker.app.dto.forecast.CreateForecastRequest;
import com.sinker.app.dto.forecast.ForecastResponse;
import com.sinker.app.dto.forecast.UpdateForecastRequest;
import com.sinker.app.dto.forecast.VersionDiffItemDTO;
import com.sinker.app.dto.forecast.VersionInfo;
import com.sinker.app.entity.GiftSalesForecast;
import com.sinker.app.entity.GiftSalesForecastVersionReason;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.GiftSalesForecastRepository;
import com.sinker.app.repository.GiftSalesForecastVersionReasonRepository;
import com.sinker.app.repository.SalesForecastConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GiftSalesForecastServiceTest {
    private static final String MONTH = "202601";
    private static final String CH = "家樂福";

    @Mock
    private GiftSalesForecastRepository forecastRepository;
    @Mock
    private SalesForecastConfigRepository configRepository;
    @Mock
    private GiftSalesForecastVersionReasonRepository versionReasonRepository;
    @Mock
    private ErpProductService erpProductService;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private GiftSalesForecastService service;

    @Test
    void createForecast_success_usesLatestVersion() {
        SalesForecastConfig cfg = new SalesForecastConfig();
        cfg.setIsClosed(false);
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(cfg));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq(CH))).thenReturn(1);
        when(erpProductService.validateProduct("P001")).thenReturn(true);
        when(forecastRepository.findByMonthAndChannelAndProductCode(MONTH, CH, "P001")).thenReturn(Optional.empty());
        when(forecastRepository.findDistinctVersionsByMonthAndChannel(MONTH, CH)).thenReturn(List.of("v-latest"));
        when(forecastRepository.save(any(GiftSalesForecast.class))).thenAnswer(i -> i.getArgument(0));

        ForecastResponse resp = service.createForecast(createReq(), 1L, "user", Set.of());

        assertEquals(MONTH, resp.getMonth());
        assertEquals(CH, resp.getChannel());
        assertEquals("P001", resp.getProductCode());
        assertEquals(new BigDecimal("12"), resp.getQuantity());
        assertEquals("v-latest", resp.getVersion());
        assertTrue(resp.getIsModified());
    }

    @Test
    void createForecast_duplicate_throws() {
        SalesForecastConfig cfg = new SalesForecastConfig();
        cfg.setIsClosed(false);
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(cfg));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq(CH))).thenReturn(1);
        when(erpProductService.validateProduct("P001")).thenReturn(true);
        when(forecastRepository.findByMonthAndChannelAndProductCode(MONTH, CH, "P001"))
                .thenReturn(Optional.of(new GiftSalesForecast()));

        assertThrows(GiftSalesForecastService.DuplicateEntryException.class,
                () -> service.createForecast(createReq(), 1L, "user", Set.of()));
    }

    @Test
    void updateForecast_notFound_throws() {
        when(forecastRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                service.updateForecast(999, new UpdateForecastRequest(), 1L, "user", Set.of()));
    }

    @Test
    void updateForecast_success_updatesQuantity() {
        GiftSalesForecast f = forecast(1, MONTH, CH, "P001", "8", "v1");
        SalesForecastConfig cfg = new SalesForecastConfig();
        cfg.setIsClosed(false);
        when(forecastRepository.findById(1)).thenReturn(Optional.of(f));
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(cfg));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq(CH))).thenReturn(1);
        when(forecastRepository.save(any(GiftSalesForecast.class))).thenAnswer(i -> i.getArgument(0));

        UpdateForecastRequest req = new UpdateForecastRequest();
        req.setQuantity(new BigDecimal("20"));
        ForecastResponse resp = service.updateForecast(1, req, 1L, "user", Set.of());

        assertEquals(new BigDecimal("20"), resp.getQuantity());
        assertTrue(resp.getIsModified());
    }

    @Test
    void queryForecasts_withoutPermission_throws() {
        assertThrows(AccessDeniedException.class, () ->
                service.queryForecasts(MONTH, CH, null, 1L, Set.of("other.permission")));
    }

    @Test
    void queryForecasts_withView_returnsLatestOrSpecificVersion() {
        GiftSalesForecast v1 = forecast(1, MONTH, CH, "P001", "9", "v1");
        GiftSalesForecast v2 = forecast(2, MONTH, CH, "P001", "11", "v2");
        when(forecastRepository.findLatestByMonthAndChannel(MONTH, CH)).thenReturn(List.of(v2));
        when(forecastRepository.findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(MONTH, CH, "v1"))
                .thenReturn(List.of(v1));

        List<ForecastResponse> latest = service.queryForecasts(MONTH, CH, null, 1L, Set.of("sales_forecast.view"));
        List<ForecastResponse> specific = service.queryForecasts(MONTH, CH, "v1", 1L, Set.of("sales_forecast.view"));

        assertEquals("v2", latest.get(0).getVersion());
        assertEquals("v1", specific.get(0).getVersion());
    }

    @Test
    void queryVersions_withViewOwnOwnedChannel_returnsVersionInfos() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq(CH))).thenReturn(1);
        when(forecastRepository.findDistinctVersionsByMonthAndChannel(MONTH, CH)).thenReturn(List.of("v2", "v1"));
        when(forecastRepository.countByMonthAndChannelAndVersion(MONTH, CH, "v2")).thenReturn(2);
        when(forecastRepository.countByMonthAndChannelAndVersion(MONTH, CH, "v1")).thenReturn(1);
        when(forecastRepository.findMaxUpdatedAtByMonthAndChannelAndVersion(MONTH, CH, "v2"))
                .thenReturn(LocalDateTime.of(2026, 1, 10, 10, 0));
        when(forecastRepository.findMaxUpdatedAtByMonthAndChannelAndVersion(MONTH, CH, "v1"))
                .thenReturn(LocalDateTime.of(2026, 1, 9, 10, 0));

        List<VersionInfo> infos = service.queryVersions(MONTH, CH, 1L, Set.of("sales_forecast.view_own"));

        assertEquals(2, infos.size());
        assertEquals("v2", infos.get(0).getVersion());
        assertEquals(2, infos.get(0).getItemCount());
    }

    @Test
    void copyLatestToNewVersion_noData_throws() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong(), anyString())).thenReturn(1);
        when(forecastRepository.findLatestByMonthAndChannel(MONTH, CH)).thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.copyLatestToNewVersion(MONTH, CH, 1L, "user"));
        assertTrue(ex.getMessage().contains("No data to copy"));
    }

    @Test
    void copyLatestToNewVersion_success_copiesAllRows() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq(CH))).thenReturn(1);
        when(forecastRepository.findLatestByMonthAndChannel(MONTH, CH))
                .thenReturn(List.of(forecast(1, MONTH, CH, "P001", "9", "v-old"), forecast(2, MONTH, CH, "P002", "4", "v-old")));

        CopyVersionResponse resp = service.copyLatestToNewVersion(MONTH, CH, 1L, "user");

        assertNotNull(resp.getVersion());
        verify(forecastRepository, times(2)).save(argThat(g ->
                g.getVersion().equals(resp.getVersion()) && Boolean.FALSE.equals(g.getIsModified())));
    }

    @Test
    void getVersionDiff_returnsOnlyChangedRows() {
        when(forecastRepository.findDistinctVersionsByMonthAndChannel(MONTH, CH))
                .thenReturn(List.of("v2", "v1"));

        GiftSalesForecast curr = new GiftSalesForecast();
        curr.setProductCode("P001");
        curr.setCategory("C");
        curr.setSpec("S");
        curr.setProductName("N");
        curr.setWarehouseLocation("A01");
        curr.setQuantity(new BigDecimal("10"));

        GiftSalesForecast prev = new GiftSalesForecast();
        prev.setProductCode("P001");
        prev.setQuantity(new BigDecimal("8"));

        when(forecastRepository.findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(MONTH, CH, "v2"))
                .thenReturn(List.of(curr));
        when(forecastRepository.findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(MONTH, CH, "v1"))
                .thenReturn(List.of(prev));

        List<VersionDiffItemDTO> result = service.getVersionDiff(MONTH, CH, "v2", 1L, Set.of("sales_forecast.view"));

        assertEquals(1, result.size());
        assertEquals("P001", result.get(0).getProductCode());
        assertEquals(new BigDecimal("10"), result.get(0).getCurrentQuantity());
        assertEquals(new BigDecimal("8"), result.get(0).getPreviousQuantity());
    }

    @Test
    void deleteForecast_closedMonth_throws() {
        GiftSalesForecast f = new GiftSalesForecast();
        f.setMonth(MONTH);
        f.setChannel(CH);
        when(forecastRepository.findById(1)).thenReturn(Optional.of(f));

        SalesForecastConfig config = new SalesForecastConfig();
        config.setIsClosed(true);
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(config));

        assertThrows(AccessDeniedException.class, () -> service.deleteForecast(1, 1L, "admin"));
        verify(forecastRepository, never()).delete(any());
    }

    @Test
    void deleteForecast_success() {
        GiftSalesForecast f = forecast(1, MONTH, CH, "P001", "8", "v1");
        SalesForecastConfig config = new SalesForecastConfig();
        config.setIsClosed(false);
        when(forecastRepository.findById(1)).thenReturn(Optional.of(f));
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(config));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq(CH))).thenReturn(1);

        service.deleteForecast(1, 1L, "user");

        verify(forecastRepository).delete(f);
    }

    @Test
    void saveVersionReason_createsWhenAbsent() {
        when(versionReasonRepository.findByMonthAndChannelAndVersion(MONTH, CH, "v2"))
                .thenReturn(Optional.empty());

        service.saveVersionReason(MONTH, CH, "v2", "調整");

        verify(versionReasonRepository).save(argThat(v ->
                MONTH.equals(v.getMonth()) && CH.equals(v.getChannel()) && "v2".equals(v.getVersion()) && "調整".equals(v.getChangeReason())));
    }

    @Test
    void deleteVersion_deletesForecastAndReason() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq(CH))).thenReturn(1);
        GiftSalesForecastVersionReason reason = new GiftSalesForecastVersionReason();
        when(versionReasonRepository.findByMonthAndChannelAndVersion(MONTH, CH, "v1")).thenReturn(Optional.of(reason));

        service.deleteVersion(MONTH, CH, "v1", 1L, "user");

        verify(forecastRepository).deleteByMonthAndChannelAndVersion(MONTH, CH, "v1");
        verify(versionReasonRepository).delete(reason);
    }

    private CreateForecastRequest createReq() {
        CreateForecastRequest req = new CreateForecastRequest();
        req.setMonth(MONTH);
        req.setChannel(CH);
        req.setCategory("C");
        req.setSpec("S");
        req.setProductCode("P001");
        req.setProductName("N");
        req.setWarehouseLocation("A01");
        req.setQuantity(new BigDecimal("12"));
        return req;
    }

    private GiftSalesForecast forecast(int id, String month, String ch, String code, String qty, String version) {
        GiftSalesForecast g = new GiftSalesForecast();
        g.setId(id);
        g.setMonth(month);
        g.setChannel(ch);
        g.setProductCode(code);
        g.setCategory("C");
        g.setSpec("S");
        g.setProductName("N");
        g.setWarehouseLocation("A01");
        g.setQuantity(new BigDecimal(qty));
        g.setVersion(version);
        g.setIsModified(false);
        return g;
    }
}
