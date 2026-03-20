package com.sinker.app.service;

import com.sinker.app.dto.materialdemand.MaterialDemandDTO;
import com.sinker.app.entity.MaterialDemand;
import com.sinker.app.repository.MaterialDemandRepository;
import com.sinker.app.util.MaterialDemandExcelParser;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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
