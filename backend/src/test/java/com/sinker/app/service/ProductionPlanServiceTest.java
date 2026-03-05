package com.sinker.app.service;

import com.sinker.app.dto.productionplan.ProductionPlanDTO;
import com.sinker.app.dto.productionplan.UpdateProductionPlanRequest;
import com.sinker.app.entity.ProductionPlan;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.ProductionPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionPlanServiceTest {

    @Mock
    private ProductionPlanRepository productionPlanRepository;

    private ProductionPlanService productionPlanService;

    @BeforeEach
    void setUp() {
        productionPlanService = new ProductionPlanService(productionPlanRepository);
    }

    @Test
    void testQueryProductionPlans_Success() {
        // Given
        Integer year = 2026;
        List<ProductionPlan> mockPlans = List.of(
                createProductionPlan(1, 2026, "PROD001", "全家"),
                createProductionPlan(2, 2026, "PROD002", "7-11")
        );
        when(productionPlanRepository.findByYearOrderByProductCodeAscChannelAsc(year))
                .thenReturn(mockPlans);

        // When
        List<ProductionPlanDTO> result = productionPlanService.queryProductionPlans(year);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(productionPlanRepository).findByYearOrderByProductCodeAscChannelAsc(year);
    }

    @Test
    void testQueryProductionPlans_EmptyResult() {
        // Given
        Integer year = 2025;
        when(productionPlanRepository.findByYearOrderByProductCodeAscChannelAsc(year))
                .thenReturn(List.of());

        // When
        List<ProductionPlanDTO> result = productionPlanService.queryProductionPlans(year);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(productionPlanRepository).findByYearOrderByProductCodeAscChannelAsc(year);
    }

    @Test
    void testUpdateProductionPlan_Success() {
        // Given
        Integer planId = 1;
        ProductionPlan existingPlan = createProductionPlan(planId, 2026, "PROD001", "全家");
        existingPlan.setOriginalForecast(new BigDecimal("1000.00"));

        Map<String, BigDecimal> newMonthlyAllocation = new HashMap<>();
        newMonthlyAllocation.put("01", new BigDecimal("100"));
        newMonthlyAllocation.put("02", new BigDecimal("150"));
        newMonthlyAllocation.put("03", new BigDecimal("200"));

        UpdateProductionPlanRequest request = new UpdateProductionPlanRequest();
        request.setMonthlyAllocation(newMonthlyAllocation);
        request.setBufferQuantity(new BigDecimal("50"));
        request.setRemarks("Updated remarks");

        when(productionPlanRepository.findById(planId)).thenReturn(Optional.of(existingPlan));
        when(productionPlanRepository.save(any(ProductionPlan.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        ProductionPlanDTO result = productionPlanService.updateProductionPlan(planId, request);

        // Then
        assertNotNull(result);
        verify(productionPlanRepository).findById(planId);
        verify(productionPlanRepository).save(any(ProductionPlan.class));

        ArgumentCaptor<ProductionPlan> captor = ArgumentCaptor.forClass(ProductionPlan.class);
        verify(productionPlanRepository).save(captor.capture());
        ProductionPlan savedPlan = captor.getValue();

        assertEquals(newMonthlyAllocation, savedPlan.getMonthlyAllocation());
        assertEquals(0, new BigDecimal("50").compareTo(savedPlan.getBufferQuantity()));
        assertEquals("Updated remarks", savedPlan.getRemarks());
        // Total = 100 + 150 + 200 + 50 = 500
        assertEquals(0, new BigDecimal("500").compareTo(savedPlan.getTotalQuantity()));
        // Difference = 500 - 1000 = -500
        assertEquals(0, new BigDecimal("-500").compareTo(savedPlan.getDifference()));
    }

    @Test
    void testUpdateProductionPlan_NotFound() {
        // Given
        Integer planId = 999;
        UpdateProductionPlanRequest request = new UpdateProductionPlanRequest();
        request.setMonthlyAllocation(new HashMap<>());
        request.setBufferQuantity(BigDecimal.ZERO);

        when(productionPlanRepository.findById(planId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productionPlanService.updateProductionPlan(planId, request)
        );

        assertTrue(exception.getMessage().contains("Production plan with ID " + planId + " not found"));
        verify(productionPlanRepository).findById(planId);
        verify(productionPlanRepository, never()).save(any());
    }

    @Test
    void testUpdateProductionPlan_EmptyMonthlyAllocation() {
        // Given
        Integer planId = 1;
        ProductionPlan existingPlan = createProductionPlan(planId, 2026, "PROD001", "全家");
        existingPlan.setOriginalForecast(new BigDecimal("1000.00"));

        UpdateProductionPlanRequest request = new UpdateProductionPlanRequest();
        request.setMonthlyAllocation(new HashMap<>());
        request.setBufferQuantity(new BigDecimal("100"));
        request.setRemarks("Only buffer");

        when(productionPlanRepository.findById(planId)).thenReturn(Optional.of(existingPlan));
        when(productionPlanRepository.save(any(ProductionPlan.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        ProductionPlanDTO result = productionPlanService.updateProductionPlan(planId, request);

        // Then
        ArgumentCaptor<ProductionPlan> captor = ArgumentCaptor.forClass(ProductionPlan.class);
        verify(productionPlanRepository).save(captor.capture());
        ProductionPlan savedPlan = captor.getValue();

        // Total = 0 (from empty map) + 100 (buffer) = 100
        assertEquals(0, new BigDecimal("100").compareTo(savedPlan.getTotalQuantity()));
        // Difference = 100 - 1000 = -900
        assertEquals(0, new BigDecimal("-900").compareTo(savedPlan.getDifference()));
    }

    @Test
    void testUpdateProductionPlan_NullMonthlyAllocation() {
        // Given
        Integer planId = 1;
        ProductionPlan existingPlan = createProductionPlan(planId, 2026, "PROD001", "全家");
        existingPlan.setOriginalForecast(new BigDecimal("500.00"));

        UpdateProductionPlanRequest request = new UpdateProductionPlanRequest();
        request.setMonthlyAllocation(null);
        request.setBufferQuantity(new BigDecimal("200"));
        request.setRemarks("Null allocation");

        when(productionPlanRepository.findById(planId)).thenReturn(Optional.of(existingPlan));
        when(productionPlanRepository.save(any(ProductionPlan.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        ProductionPlanDTO result = productionPlanService.updateProductionPlan(planId, request);

        // Then
        ArgumentCaptor<ProductionPlan> captor = ArgumentCaptor.forClass(ProductionPlan.class);
        verify(productionPlanRepository).save(captor.capture());
        ProductionPlan savedPlan = captor.getValue();

        // Total = 0 (from null map) + 200 (buffer) = 200
        assertEquals(0, new BigDecimal("200").compareTo(savedPlan.getTotalQuantity()));
        // Difference = 200 - 500 = -300
        assertEquals(0, new BigDecimal("-300").compareTo(savedPlan.getDifference()));
    }

    @Test
    void testUpdateProductionPlan_PositiveDifference() {
        // Given
        Integer planId = 1;
        ProductionPlan existingPlan = createProductionPlan(planId, 2026, "PROD001", "全家");
        existingPlan.setOriginalForecast(new BigDecimal("100.00"));

        Map<String, BigDecimal> newMonthlyAllocation = new HashMap<>();
        newMonthlyAllocation.put("01", new BigDecimal("80"));
        newMonthlyAllocation.put("02", new BigDecimal("70"));

        UpdateProductionPlanRequest request = new UpdateProductionPlanRequest();
        request.setMonthlyAllocation(newMonthlyAllocation);
        request.setBufferQuantity(new BigDecimal("50"));
        request.setRemarks("Increased quantity");

        when(productionPlanRepository.findById(planId)).thenReturn(Optional.of(existingPlan));
        when(productionPlanRepository.save(any(ProductionPlan.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        ProductionPlanDTO result = productionPlanService.updateProductionPlan(planId, request);

        // Then
        ArgumentCaptor<ProductionPlan> captor = ArgumentCaptor.forClass(ProductionPlan.class);
        verify(productionPlanRepository).save(captor.capture());
        ProductionPlan savedPlan = captor.getValue();

        // Total = 80 + 70 + 50 = 200
        assertEquals(0, new BigDecimal("200").compareTo(savedPlan.getTotalQuantity()));
        // Difference = 200 - 100 = 100
        assertEquals(0, new BigDecimal("100").compareTo(savedPlan.getDifference()));
    }

    @Test
    void testUpdateProductionPlan_ZeroBufferQuantity() {
        // Given
        Integer planId = 1;
        ProductionPlan existingPlan = createProductionPlan(planId, 2026, "PROD001", "全家");
        existingPlan.setOriginalForecast(new BigDecimal("300.00"));

        Map<String, BigDecimal> newMonthlyAllocation = new HashMap<>();
        newMonthlyAllocation.put("01", new BigDecimal("100"));
        newMonthlyAllocation.put("02", new BigDecimal("100"));
        newMonthlyAllocation.put("03", new BigDecimal("100"));

        UpdateProductionPlanRequest request = new UpdateProductionPlanRequest();
        request.setMonthlyAllocation(newMonthlyAllocation);
        request.setBufferQuantity(BigDecimal.ZERO);
        request.setRemarks("No buffer");

        when(productionPlanRepository.findById(planId)).thenReturn(Optional.of(existingPlan));
        when(productionPlanRepository.save(any(ProductionPlan.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        ProductionPlanDTO result = productionPlanService.updateProductionPlan(planId, request);

        // Then
        ArgumentCaptor<ProductionPlan> captor = ArgumentCaptor.forClass(ProductionPlan.class);
        verify(productionPlanRepository).save(captor.capture());
        ProductionPlan savedPlan = captor.getValue();

        // Total = 100 + 100 + 100 + 0 = 300
        assertEquals(0, new BigDecimal("300").compareTo(savedPlan.getTotalQuantity()));
        // Difference = 300 - 300 = 0
        assertEquals(0, BigDecimal.ZERO.compareTo(savedPlan.getDifference()));
    }

    @Test
    void testUpdateProductionPlan_NullRemarks() {
        // Given
        Integer planId = 1;
        ProductionPlan existingPlan = createProductionPlan(planId, 2026, "PROD001", "全家");
        existingPlan.setOriginalForecast(new BigDecimal("100.00"));

        Map<String, BigDecimal> newMonthlyAllocation = new HashMap<>();
        newMonthlyAllocation.put("01", new BigDecimal("50"));

        UpdateProductionPlanRequest request = new UpdateProductionPlanRequest();
        request.setMonthlyAllocation(newMonthlyAllocation);
        request.setBufferQuantity(new BigDecimal("25"));
        request.setRemarks(null);

        when(productionPlanRepository.findById(planId)).thenReturn(Optional.of(existingPlan));
        when(productionPlanRepository.save(any(ProductionPlan.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        ProductionPlanDTO result = productionPlanService.updateProductionPlan(planId, request);

        // Then
        ArgumentCaptor<ProductionPlan> captor = ArgumentCaptor.forClass(ProductionPlan.class);
        verify(productionPlanRepository).save(captor.capture());
        ProductionPlan savedPlan = captor.getValue();

        assertNull(savedPlan.getRemarks());
    }

    private ProductionPlan createProductionPlan(Integer id, Integer year, String productCode, String channel) {
        ProductionPlan plan = new ProductionPlan();
        plan.setId(id);
        plan.setYear(year);
        plan.setProductCode(productCode);
        plan.setProductName("Product " + productCode);
        plan.setCategory("Category");
        plan.setSpec("Spec");
        plan.setWarehouseLocation("A01");
        plan.setChannel(channel);
        plan.setMonthlyAllocation(new HashMap<>());
        plan.setBufferQuantity(BigDecimal.ZERO);
        plan.setTotalQuantity(BigDecimal.ZERO);
        plan.setOriginalForecast(BigDecimal.ZERO);
        plan.setDifference(BigDecimal.ZERO);
        return plan;
    }
}
