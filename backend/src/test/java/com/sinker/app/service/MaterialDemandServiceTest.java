package com.sinker.app.service;

import com.sinker.app.dto.materialdemand.MaterialDemandDTO;
import com.sinker.app.entity.MaterialDemand;
import com.sinker.app.repository.MaterialDemandRepository;
import org.junit.jupiter.api.Test;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialDemandServiceTest {

    @Mock
    private MaterialDemandRepository materialDemandRepository;

    @InjectMocks
    private MaterialDemandService materialDemandService;

    @Test
    void testQueryMaterialDemandSuccess() {
        // Arrange
        LocalDate weekStart = LocalDate.of(2026, 2, 17);
        String factory = "F1";

        MaterialDemand demand1 = createMaterialDemand(1, weekStart, factory, "M001", "原料A", "kg",
                LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 20),
                new BigDecimal("100.50"), new BigDecimal("500.00"), new BigDecimal("50.25"));

        MaterialDemand demand2 = createMaterialDemand(2, weekStart, factory, "M002", "原料B", "pcs",
                null, LocalDate.of(2026, 2, 22),
                new BigDecimal("0.00"), new BigDecimal("1000.00"), new BigDecimal("0.00"));

        when(materialDemandRepository.findByWeekStartAndFactoryOrderByMaterialCodeAsc(weekStart, factory))
                .thenReturn(Arrays.asList(demand1, demand2));

        // Act
        List<MaterialDemandDTO> results = materialDemandService.queryMaterialDemand(weekStart, factory);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());

        MaterialDemandDTO dto1 = results.get(0);
        assertEquals(1, dto1.getId());
        assertEquals(weekStart, dto1.getWeekStart());
        assertEquals(factory, dto1.getFactory());
        assertEquals("M001", dto1.getMaterialCode());
        assertEquals("原料A", dto1.getMaterialName());
        assertEquals("kg", dto1.getUnit());
        assertEquals(LocalDate.of(2026, 2, 10), dto1.getLastPurchaseDate());
        assertEquals(LocalDate.of(2026, 2, 20), dto1.getDemandDate());
        assertEquals(new BigDecimal("100.50"), dto1.getExpectedDelivery());
        assertEquals(new BigDecimal("500.00"), dto1.getDemandQuantity());
        assertEquals(new BigDecimal("50.25"), dto1.getEstimatedInventory());

        MaterialDemandDTO dto2 = results.get(1);
        assertEquals(2, dto2.getId());
        assertEquals("M002", dto2.getMaterialCode());
        assertNull(dto2.getLastPurchaseDate());

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
