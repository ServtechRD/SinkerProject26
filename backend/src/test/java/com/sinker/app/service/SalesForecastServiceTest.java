package com.sinker.app.service;

import com.sinker.app.dto.forecast.CreateForecastRequest;
import com.sinker.app.dto.forecast.ForecastResponse;
import com.sinker.app.dto.forecast.UpdateForecastRequest;
import com.sinker.app.entity.SalesForecast;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesForecastServiceTest {

    @Mock
    private SalesForecastRepository forecastRepository;

    @Mock
    private SalesForecastConfigRepository configRepository;

    @Mock
    private ErpProductService erpProductService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SalesForecastService service;

    private SalesForecastConfig openConfig;
    private SalesForecastConfig closedConfig;

    @BeforeEach
    void setUp() {
        openConfig = new SalesForecastConfig();
        openConfig.setMonth("202601");
        openConfig.setIsClosed(false);

        closedConfig = new SalesForecastConfig();
        closedConfig.setMonth("202512");
        closedConfig.setIsClosed(true);
    }

    @Test
    void createForecast_success() {
        CreateForecastRequest request = new CreateForecastRequest();
        request.setMonth("202601");
        request.setChannel("大全聯");
        request.setCategory("飲料類");
        request.setSpec("600ml*24入");
        request.setProductCode("P001");
        request.setProductName("可口可樂");
        request.setWarehouseLocation("A01");
        request.setQuantity(new BigDecimal("100.50"));

        when(configRepository.findByMonth("202601")).thenReturn(Optional.of(openConfig));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong(), anyString()))
                .thenReturn(1);
        when(erpProductService.validateProduct("P001")).thenReturn(true);
        when(forecastRepository.findByMonthAndChannelAndProductCode("202601", "大全聯", "P001"))
                .thenReturn(Optional.empty());

        SalesForecast savedForecast = new SalesForecast();
        savedForecast.setId(1);
        savedForecast.setMonth("202601");
        savedForecast.setChannel("大全聯");
        savedForecast.setCategory("飲料類");
        savedForecast.setSpec("600ml*24入");
        savedForecast.setProductCode("P001");
        savedForecast.setProductName("可口可樂");
        savedForecast.setWarehouseLocation("A01");
        savedForecast.setQuantity(new BigDecimal("100.50"));
        savedForecast.setVersion("2026/01/15 14:30:00(大全聯)");
        savedForecast.setIsModified(true);
        savedForecast.setCreatedAt(LocalDateTime.now());
        savedForecast.setUpdatedAt(LocalDateTime.now());

        when(forecastRepository.save(any(SalesForecast.class))).thenReturn(savedForecast);

        ForecastResponse response = service.createForecast(request, 1L, "user");

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("202601", response.getMonth());
        assertEquals("大全聯", response.getChannel());
        assertEquals("P001", response.getProductCode());
        assertEquals(new BigDecimal("100.50"), response.getQuantity());
        assertTrue(response.getIsModified());

        verify(forecastRepository).save(any(SalesForecast.class));
    }

    @Test
    void createForecast_invalidProductCode() {
        CreateForecastRequest request = new CreateForecastRequest();
        request.setMonth("202601");
        request.setChannel("大全聯");
        request.setProductCode("INVALID");
        request.setQuantity(new BigDecimal("100.50"));

        when(configRepository.findByMonth("202601")).thenReturn(Optional.of(openConfig));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong(), anyString()))
                .thenReturn(1);
        when(erpProductService.validateProduct("INVALID")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
                service.createForecast(request, 1L, "user"));

        verify(forecastRepository, never()).save(any());
    }

    @Test
    void createForecast_duplicate() {
        CreateForecastRequest request = new CreateForecastRequest();
        request.setMonth("202601");
        request.setChannel("大全聯");
        request.setProductCode("P001");
        request.setQuantity(new BigDecimal("100.50"));

        when(configRepository.findByMonth("202601")).thenReturn(Optional.of(openConfig));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong(), anyString()))
                .thenReturn(1);
        when(erpProductService.validateProduct("P001")).thenReturn(true);
        when(forecastRepository.findByMonthAndChannelAndProductCode("202601", "大全聯", "P001"))
                .thenReturn(Optional.of(new SalesForecast()));

        assertThrows(SalesForecastService.DuplicateEntryException.class, () ->
                service.createForecast(request, 1L, "user"));

        verify(forecastRepository, never()).save(any());
    }

    @Test
    void createForecast_monthClosed() {
        CreateForecastRequest request = new CreateForecastRequest();
        request.setMonth("202512");
        request.setChannel("大全聯");
        request.setProductCode("P001");
        request.setQuantity(new BigDecimal("100.50"));

        when(configRepository.findByMonth("202512")).thenReturn(Optional.of(closedConfig));

        assertThrows(AccessDeniedException.class, () ->
                service.createForecast(request, 1L, "user"));

        verify(forecastRepository, never()).save(any());
    }

    @Test
    void createForecast_noChannelOwnership() {
        CreateForecastRequest request = new CreateForecastRequest();
        request.setMonth("202601");
        request.setChannel("大全聯");
        request.setProductCode("P001");
        request.setQuantity(new BigDecimal("100.50"));

        when(configRepository.findByMonth("202601")).thenReturn(Optional.of(openConfig));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong(), anyString()))
                .thenReturn(0);

        assertThrows(AccessDeniedException.class, () ->
                service.createForecast(request, 1L, "user"));

        verify(forecastRepository, never()).save(any());
    }

    @Test
    void updateForecast_success() {
        UpdateForecastRequest request = new UpdateForecastRequest();
        request.setQuantity(new BigDecimal("150.75"));

        SalesForecast existingForecast = new SalesForecast();
        existingForecast.setId(1);
        existingForecast.setMonth("202601");
        existingForecast.setChannel("大全聯");
        existingForecast.setProductCode("P001");
        existingForecast.setQuantity(new BigDecimal("100.50"));
        existingForecast.setVersion("2026/01/15 14:30:00(大全聯)");
        existingForecast.setIsModified(false);

        when(forecastRepository.findById(1)).thenReturn(Optional.of(existingForecast));
        when(configRepository.findByMonth("202601")).thenReturn(Optional.of(openConfig));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong(), anyString()))
                .thenReturn(1);
        when(forecastRepository.save(any(SalesForecast.class))).thenReturn(existingForecast);

        ForecastResponse response = service.updateForecast(1, request, 1L, "user");

        assertNotNull(response);
        verify(forecastRepository).save(argThat(forecast ->
                forecast.getQuantity().equals(new BigDecimal("150.75")) &&
                Boolean.TRUE.equals(forecast.getIsModified())
        ));
    }

    @Test
    void updateForecast_notFound() {
        UpdateForecastRequest request = new UpdateForecastRequest();
        request.setQuantity(new BigDecimal("150.75"));

        when(forecastRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.updateForecast(999, request, 1L, "user"));

        verify(forecastRepository, never()).save(any());
    }

    @Test
    void deleteForecast_success() {
        SalesForecast existingForecast = new SalesForecast();
        existingForecast.setId(1);
        existingForecast.setMonth("202601");
        existingForecast.setChannel("大全聯");
        existingForecast.setProductCode("P001");

        when(forecastRepository.findById(1)).thenReturn(Optional.of(existingForecast));
        when(configRepository.findByMonth("202601")).thenReturn(Optional.of(openConfig));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong(), anyString()))
                .thenReturn(1);

        service.deleteForecast(1, 1L, "user");

        verify(forecastRepository).delete(existingForecast);
    }

    @Test
    void deleteForecast_notFound() {
        when(forecastRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.deleteForecast(999, 1L, "user"));

        verify(forecastRepository, never()).delete(any());
    }

    @Test
    void adminBypassesChannelOwnership() {
        CreateForecastRequest request = new CreateForecastRequest();
        request.setMonth("202601");
        request.setChannel("大全聯");
        request.setProductCode("P001");
        request.setQuantity(new BigDecimal("100.50"));

        when(configRepository.findByMonth("202601")).thenReturn(Optional.of(openConfig));
        when(erpProductService.validateProduct("P001")).thenReturn(true);
        when(forecastRepository.findByMonthAndChannelAndProductCode("202601", "大全聯", "P001"))
                .thenReturn(Optional.empty());

        SalesForecast savedForecast = new SalesForecast();
        savedForecast.setId(1);
        savedForecast.setMonth("202601");
        savedForecast.setChannel("大全聯");
        savedForecast.setProductCode("P001");
        savedForecast.setQuantity(new BigDecimal("100.50"));
        savedForecast.setVersion("2026/01/15 14:30:00(大全聯)");
        savedForecast.setIsModified(true);
        savedForecast.setCreatedAt(LocalDateTime.now());
        savedForecast.setUpdatedAt(LocalDateTime.now());

        when(forecastRepository.save(any(SalesForecast.class))).thenReturn(savedForecast);

        ForecastResponse response = service.createForecast(request, 1L, "admin");

        assertNotNull(response);
        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Integer.class), anyLong(), anyString());
    }
}
