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

    @InjectMocks
    private MaterialDemandService materialDemandService;

    @Test
    void testQueryMaterialDemandSuccess() {
        // Arrange
        LocalDate weekStart = LocalDate.of(2026, 2, 17);
        String factory = "F1";

        MaterialDemand entity1 = new MaterialDemand();
        entity1.setId(1);
        entity1.setWeekStart(weekStart);
        entity1.setFactory(factory);
        entity1.setMaterialCode("M001");
        entity1.setMaterialName("原料A");
        entity1.setUnit("kg");
        entity1.setLastPurchaseDate(LocalDate.of(2026, 2, 10));
        entity1.setDemandDate(LocalDate.of(2026, 2, 20));
        entity1.setExpectedDelivery(new BigDecimal("100.50"));
        entity1.setDemandQuantity(new BigDecimal("500.00"));
        entity1.setEstimatedInventory(new BigDecimal("50.25"));

        MaterialDemand entity2 = new MaterialDemand();
        entity2.setId(2);
        entity2.setWeekStart(weekStart);
        entity2.setFactory(factory);
        entity2.setMaterialCode("M002");
        entity2.setMaterialName("原料B");
        entity2.setUnit("pcs");
        entity2.setLastPurchaseDate(null);
        entity2.setDemandDate(LocalDate.of(2026, 2, 22));
        entity2.setExpectedDelivery(new BigDecimal("0.00"));
        entity2.setDemandQuantity(new BigDecimal("1000.00"));
        entity2.setEstimatedInventory(new BigDecimal("0.00"));

        when(materialDemandRepository.findByWeekStartAndFactoryOrderByMaterialCodeAsc(weekStart, factory))
                .thenReturn(Arrays.asList(entity1, entity2));

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

        verify(materialDemandRepository, times(1))
                .findByWeekStartAndFactoryOrderByMaterialCodeAsc(weekStart, factory);
    }

    @Test
    void testQueryMaterialDemandEmptyResult() {
        // Arrange
        LocalDate weekStart = LocalDate.of(2026, 12, 31);
        String factory = "F999";

        when(materialDemandRepository.findByWeekStartAndFactoryOrderByMaterialCodeAsc(weekStart, factory))
                .thenReturn(Arrays.asList());

        // Act
        List<MaterialDemandDTO> results = materialDemandService.queryMaterialDemand(weekStart, factory);

        // Assert
        assertNotNull(results);
        assertEquals(0, results.size());

        verify(materialDemandRepository, times(1))
                .findByWeekStartAndFactoryOrderByMaterialCodeAsc(weekStart, factory);
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
        verify(jdbcTemplate).update(
                contains("material_demand_pending_confirm"),
                eq(entity.getWeekStart()),
                eq("F1"),
                eq(MaterialDemandService.REVIEW_STATUS_PENDING),
                eq(MaterialDemandService.REVIEW_STATUS_PENDING));
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
        verify(jdbcTemplate).update(
                contains("material_demand_pending_confirm"),
                eq(weekStart),
                eq(factory),
                eq(MaterialDemandService.REVIEW_STATUS_PENDING),
                eq(MaterialDemandService.REVIEW_STATUS_PENDING));
    }

    @Test
    void upload_factoryBlank_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> materialDemandService.upload(file(), LocalDate.of(2026, 2, 17), " "));
    }

    @Test
    void getPendingConfirmForRole_admin_mapsResultSetRows() throws Exception {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class),
                eq(MaterialDemandService.REVIEW_STATUS_PENDING)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    org.springframework.jdbc.core.RowMapper<MaterialDemandPendingConfirmItemDTO> mapper =
                            invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getObject("week_start", LocalDate.class)).thenReturn(LocalDate.of(2026, 2, 17));
                    when(rs.getString("factory")).thenReturn("F1");
                    when(rs.getInt("status")).thenReturn(0);
                    when(rs.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 2, 17, 10, 0)));
                    return List.of(mapper.mapRow(rs, 0));
                });

        List<MaterialDemandPendingConfirmItemDTO> result = materialDemandService.getPendingConfirmForRole("admin");

        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2026, 2, 17), result.get(0).getWeekStart());
        assertEquals("F1", result.get(0).getFactory());
        assertEquals(Integer.valueOf(0), result.get(0).getStatus());
        assertEquals(LocalDateTime.of(2026, 2, 17, 10, 0), result.get(0).getUpdatedAt());
    }

    @Test
    void getPendingConfirmForRole_procurement_queriesApprovedAndRejected() {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class),
                eq(MaterialDemandService.REVIEW_STATUS_APPROVED),
                eq(MaterialDemandService.REVIEW_STATUS_REJECTED)))
                .thenReturn(List.of());

        materialDemandService.getPendingConfirmForRole("procurement");

        verify(jdbcTemplate).query(contains("status IN (?, ?)"),
                any(org.springframework.jdbc.core.RowMapper.class),
                eq(MaterialDemandService.REVIEW_STATUS_APPROVED),
                eq(MaterialDemandService.REVIEW_STATUS_REJECTED));
    }

    @Test
    void getPendingConfirmForRole_unknownRole_returnsEmpty() {
        assertTrue(materialDemandService.getPendingConfirmForRole("sales").isEmpty());
    }

    @Test
    void getLastEditSavedAt_hasRecord_returnsTimestamp() throws Exception {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    org.springframework.jdbc.core.RowMapper<MaterialDemandPendingConfirmItemDTO> mapper =
                            invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getObject("week_start", LocalDate.class)).thenReturn(LocalDate.of(2026, 2, 17));
                    when(rs.getString("factory")).thenReturn("F1");
                    when(rs.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 2, 17, 11, 0)));
                    when(rs.getInt("status")).thenReturn(0);
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

        MaterialDemandPendingConfirmItemDTO nullTime = new MaterialDemandPendingConfirmItemDTO();
        nullTime.setUpdatedAt(null);
        nullTime.setStatus(0);
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(), any()))
                .thenReturn(List.of(nullTime));
        assertTrue(materialDemandService.getLastEditSavedAt(LocalDate.of(2026, 2, 17), "F1").isEmpty());
    }

    @Test
    void confirmSendErp_callsErpAndClearsPending() {
        LocalDate weekStart = LocalDate.of(2026, 2, 17);
        when(jdbcTemplate.query(contains("SELECT status"), any(org.springframework.jdbc.core.RowMapper.class), any(), any()))
                .thenReturn(List.of(MaterialDemandService.REVIEW_STATUS_APPROVED));
        materialDemandService.confirmSendErp(weekStart, "F1", "V001");
        verify(erpPurchaseOrderService).createPurchaseOrder(weekStart, "F1", "V001");
        verify(jdbcTemplate).update(contains("DELETE FROM material_demand_pending_confirm"), eq(weekStart), eq("F1"));
    }

    @Test
    void confirmSendErp_wrongStatus_skipsErp() {
        LocalDate weekStart = LocalDate.of(2026, 2, 17);
        when(jdbcTemplate.query(contains("SELECT status"), any(org.springframework.jdbc.core.RowMapper.class), any(), any()))
                .thenReturn(List.of(MaterialDemandService.REVIEW_STATUS_PENDING));
        assertThrows(IllegalArgumentException.class, () -> materialDemandService.confirmSendErp(weekStart, "F1", "V001"));
        verify(erpPurchaseOrderService, never()).createPurchaseOrder(any(), any(), any());
    }

    @Test
    void markPendingConfirm_executesUpsert() {
        LocalDate weekStart = LocalDate.of(2026, 2, 17);
        materialDemandService.markPendingConfirm(weekStart, "F1");
        verify(jdbcTemplate).update(
                contains("INSERT INTO material_demand_pending_confirm"),
                eq(weekStart),
                eq("F1"),
                eq(MaterialDemandService.REVIEW_STATUS_PENDING),
                eq(MaterialDemandService.REVIEW_STATUS_PENDING));
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
