package com.sinker.app.service;

import com.sinker.app.dto.materialdemand.MaterialDemandDTO;
import com.sinker.app.dto.materialdemand.MaterialDemandPendingConfirmItemDTO;
import com.sinker.app.dto.materialdemand.MaterialDemandUpdateDTO;
import com.sinker.app.entity.MaterialDemand;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.MaterialDemandRepository;
import com.sinker.app.util.MaterialDemandExcelParser;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialDemandServiceTest {

    @Mock
    private MaterialDemandRepository materialDemandRepository;

    @Mock
    private MaterialDemandExcelParser excelParser;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private PdcaRecomputeService pdcaRecomputeService;

    @Mock
    private ErpPurchaseOrderService erpPurchaseOrderService;

    @Mock
    private PdcaIntegrationService pdcaIntegrationService;

    @InjectMocks
    private MaterialDemandService materialDemandService;

    @Test
    void testQueryMaterialDemandSuccess() {
        // Arrange
        LocalDate weekStart = LocalDate.of(2026, 2, 17);
        String factory = "F1";

        MaterialDemandDTO dto1 = new MaterialDemandDTO();
        dto1.setId(1);
        dto1.setWeekStart(weekStart);
        dto1.setFactory(factory);
        dto1.setMaterialCode("M001");
        dto1.setMaterialName("原料A");
        dto1.setUnit("kg");
        dto1.setLastPurchaseDate(LocalDate.of(2026, 2, 10));
        dto1.setDemandDate(LocalDate.of(2026, 2, 20));
        dto1.setExpectedDelivery(new BigDecimal("100.50"));
        dto1.setDemandQuantity(new BigDecimal("500.00"));
        dto1.setEstimatedInventory(new BigDecimal("50.25"));

        MaterialDemandDTO dto2 = new MaterialDemandDTO();
        dto2.setId(2);
        dto2.setWeekStart(weekStart);
        dto2.setFactory(factory);
        dto2.setMaterialCode("M002");
        dto2.setMaterialName("原料B");
        dto2.setUnit("pcs");
        dto2.setLastPurchaseDate(null);
        dto2.setDemandDate(LocalDate.of(2026, 2, 22));
        dto2.setExpectedDelivery(new BigDecimal("0.00"));
        dto2.setDemandQuantity(new BigDecimal("1000.00"));
        dto2.setEstimatedInventory(new BigDecimal("0.00"));

        when(pdcaIntegrationService.syncMaterialDemandFromPdca(weekStart, factory))
                .thenReturn(Arrays.asList(dto1, dto2));

        // Act
        List<MaterialDemandDTO> results = materialDemandService.queryMaterialDemand(weekStart, factory);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());

        MaterialDemandDTO out1 = results.get(0);
        assertEquals(1, out1.getId());
        assertEquals(weekStart, out1.getWeekStart());
        assertEquals(factory, out1.getFactory());
        assertEquals("M001", out1.getMaterialCode());
        assertEquals("原料A", out1.getMaterialName());
        assertEquals("kg", out1.getUnit());
        assertEquals(LocalDate.of(2026, 2, 10), out1.getLastPurchaseDate());
        assertEquals(LocalDate.of(2026, 2, 20), out1.getDemandDate());
        assertEquals(new BigDecimal("100.50"), out1.getExpectedDelivery());
        assertEquals(new BigDecimal("500.00"), out1.getDemandQuantity());
        assertEquals(new BigDecimal("50.25"), out1.getEstimatedInventory());

        MaterialDemandDTO out2 = results.get(1);
        assertEquals(2, out2.getId());
        assertEquals("M002", out2.getMaterialCode());
        assertNull(out2.getLastPurchaseDate());

        verify(pdcaIntegrationService, times(1)).syncMaterialDemandFromPdca(weekStart, factory);
    }

    @Test
    void testQueryMaterialDemandEmptyResult() {
        // Arrange
        LocalDate weekStart = LocalDate.of(2026, 12, 31);
        String factory = "F999";

        when(pdcaIntegrationService.syncMaterialDemandFromPdca(weekStart, factory))
                .thenReturn(Arrays.asList());

        // Act
        List<MaterialDemandDTO> results = materialDemandService.queryMaterialDemand(weekStart, factory);

        // Assert
        assertNotNull(results);
        assertEquals(0, results.size());

        verify(pdcaIntegrationService, times(1)).syncMaterialDemandFromPdca(weekStart, factory);
    }

    @Test
    void update_success_marksPendingAndRecompute() {
        MaterialDemand entity = createMaterialDemand(10, LocalDate.of(2026, 2, 17), "F1", "M1", "N1", "KG",
                null, LocalDate.of(2026, 2, 20), new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"));
        entity.setPurchaseQuantity(new BigDecimal("5"));
        when(materialDemandRepository.findById(10)).thenReturn(Optional.of(entity));
        when(materialDemandRepository.save(any(MaterialDemand.class))).thenAnswer(i -> i.getArgument(0));

        MaterialDemandUpdateDTO dto = new MaterialDemandUpdateDTO();
        dto.setPurchaseQuantity(new BigDecimal("9"));
        MaterialDemandDTO out = materialDemandService.update(10, dto);

        assertEquals(new BigDecimal("9"), out.getPurchaseQuantity());
        verify(jdbcTemplate).update(contains("material_demand_pending_confirm"), eq(entity.getWeekStart()), eq("F1"));
        verify(pdcaRecomputeService).recomputeAsync(entity.getWeekStart(), "F1");
    }

    @Test
    void update_negativePurchaseQuantity_throws() {
        MaterialDemand entity = createMaterialDemand(10, LocalDate.of(2026, 2, 17), "F1", "M1", "N1", "KG",
                null, LocalDate.of(2026, 2, 20), new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"));
        when(materialDemandRepository.findById(10)).thenReturn(Optional.of(entity));

        MaterialDemandUpdateDTO dto = new MaterialDemandUpdateDTO();
        dto.setPurchaseQuantity(new BigDecimal("-1"));
        assertThrows(IllegalArgumentException.class, () -> materialDemandService.update(10, dto));
        verify(materialDemandRepository, never()).save(any());
    }

    @Test
    void update_notFound_throws() {
        when(materialDemandRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> materialDemandService.update(999, new MaterialDemandUpdateDTO()));
    }

    @Test
    void upload_success_mapsRowsAndRecompute() {
        LocalDate weekStart = LocalDate.of(2026, 2, 17);
        String factory = "F1";
        List<MaterialDemandExcelParser.MaterialDemandRow> rows = List.of(
                new MaterialDemandExcelParser.MaterialDemandRow("M1", "N1", "KG", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 20),
                        new BigDecimal("10"), LocalDate.of(2026, 2, 18), new BigDecimal("2"), new BigDecimal("5"), new BigDecimal("7"), new BigDecimal("1"))
        );
        when(excelParser.parse(any(MultipartFile.class))).thenReturn(rows);

        int count = materialDemandService.upload(file(), weekStart, factory);

        assertEquals(1, count);
        verify(materialDemandRepository).deleteByWeekStartAndFactory(weekStart, factory);
        verify(materialDemandRepository).saveAll(argThat(items -> {
            java.util.ArrayList<MaterialDemand> list = new java.util.ArrayList<>();
            items.forEach(list::add);
            return list.size() == 1
                    && "M1".equals(list.get(0).getMaterialCode())
                    && "F1".equals(list.get(0).getFactory());
        }));
        verify(pdcaRecomputeService).recomputeAsync(weekStart, factory);
    }

    @Test
    void upload_factoryBlank_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> materialDemandService.upload(file(), LocalDate.of(2026, 2, 17), " "));
    }

    @Test
    void getPendingConfirm_mapsResultSetRows() throws Exception {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    org.springframework.jdbc.core.RowMapper<MaterialDemandPendingConfirmItemDTO> mapper =
                            invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getObject("week_start", LocalDate.class)).thenReturn(LocalDate.of(2026, 2, 17));
                    when(rs.getString("factory")).thenReturn("F1");
                    when(rs.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 2, 17, 10, 0)));
                    return List.of(mapper.mapRow(rs, 0));
                });

        List<MaterialDemandPendingConfirmItemDTO> result = materialDemandService.getPendingConfirm();

        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2026, 2, 17), result.get(0).getWeekStart());
        assertEquals("F1", result.get(0).getFactory());
        assertEquals(LocalDateTime.of(2026, 2, 17, 10, 0), result.get(0).getUpdatedAt());
    }

    @Test
    void getLastEditSavedAt_hasRecord_returnsTimestamp() throws Exception {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    org.springframework.jdbc.core.RowMapper<LocalDateTime> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getTimestamp(1)).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 2, 17, 11, 0)));
                    return List.of(mapper.mapRow(rs, 0));
                });

        Optional<LocalDateTime> result = materialDemandService.getLastEditSavedAt(LocalDate.of(2026, 2, 17), "F1");

        assertTrue(result.isPresent());
        assertEquals(LocalDateTime.of(2026, 2, 17, 11, 0), result.get());
    }

    @Test
    void getLastEditSavedAt_emptyOrNull_returnsEmpty() {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(), any()))
                .thenReturn(List.of());
        assertTrue(materialDemandService.getLastEditSavedAt(LocalDate.of(2026, 2, 17), "F1").isEmpty());

        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(), any()))
                .thenReturn(Arrays.asList((LocalDateTime) null));
        assertTrue(materialDemandService.getLastEditSavedAt(LocalDate.of(2026, 2, 17), "F1").isEmpty());
    }

    @Test
    void confirmSendErp_callsErpAndClearsPending() {
        LocalDate weekStart = LocalDate.of(2026, 2, 17);
        materialDemandService.confirmSendErp(weekStart, "F1");
        verify(erpPurchaseOrderService).createPurchaseOrder(weekStart, "F1");
        verify(jdbcTemplate).update(contains("DELETE FROM material_demand_pending_confirm"), eq(weekStart), eq("F1"));
    }

    @Test
    void markPendingConfirm_executesUpsert() {
        LocalDate weekStart = LocalDate.of(2026, 2, 17);
        materialDemandService.markPendingConfirm(weekStart, "F1");
        verify(jdbcTemplate).update(contains("INSERT INTO material_demand_pending_confirm"), eq(weekStart), eq("F1"));
    }

    @Test
    void generateTemplate_returnsBytes() {
        byte[] bytes = materialDemandService.generateTemplate("F1");
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    private MultipartFile file() {
        return new MockMultipartFile("file", "material.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "x".getBytes());
    }

    private MaterialDemand createMaterialDemand(Integer id, LocalDate weekStart, String factory,
                                                String materialCode, String materialName, String unit,
                                                LocalDate lastPurchaseDate, LocalDate demandDate,
                                                BigDecimal expectedDelivery, BigDecimal demandQuantity,
                                                BigDecimal estimatedInventory) {
        MaterialDemand demand = new MaterialDemand();
        demand.setId(id);
        demand.setWeekStart(weekStart);
        demand.setFactory(factory);
        demand.setMaterialCode(materialCode);
        demand.setMaterialName(materialName);
        demand.setUnit(unit);
        demand.setLastPurchaseDate(lastPurchaseDate);
        demand.setDemandDate(demandDate);
        demand.setExpectedDelivery(expectedDelivery);
        demand.setDemandQuantity(demandQuantity);
        demand.setEstimatedInventory(estimatedInventory);
        demand.setCreatedAt(LocalDateTime.now());
        demand.setUpdatedAt(LocalDateTime.now());
        return demand;
    }
}
