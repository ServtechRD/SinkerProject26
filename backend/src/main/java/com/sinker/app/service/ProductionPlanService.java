package com.sinker.app.service;

import com.sinker.app.dto.productionplan.ProductionPlanDTO;
import com.sinker.app.dto.productionplan.UpdateProductionPlanRequest;
import com.sinker.app.entity.ProductionPlan;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.ProductionPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductionPlanService {

    private static final Logger log = LoggerFactory.getLogger(ProductionPlanService.class);

    private final ProductionPlanRepository productionPlanRepository;

    public ProductionPlanService(ProductionPlanRepository productionPlanRepository) {
        this.productionPlanRepository = productionPlanRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductionPlanDTO> queryProductionPlans(Integer year) {
        log.info("Querying production plans for year: {}", year);

        List<ProductionPlan> plans = productionPlanRepository.findByYearOrderByProductCodeAscChannelAsc(year);

        return plans.stream()
                .map(ProductionPlanDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductionPlanDTO updateProductionPlan(Integer id, UpdateProductionPlanRequest request) {
        log.info("Updating production plan ID: {}", id);

        ProductionPlan plan = productionPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Production plan with ID " + id + " not found"));

        // Update fields
        plan.setMonthlyAllocation(request.getMonthlyAllocation());
        plan.setBufferQuantity(request.getBufferQuantity());
        plan.setRemarks(request.getRemarks());

        // Calculate total_quantity = sum of all monthly values + buffer_quantity
        BigDecimal monthlySum = calculateMonthlySum(request.getMonthlyAllocation());
        BigDecimal totalQuantity = monthlySum.add(request.getBufferQuantity());
        plan.setTotalQuantity(totalQuantity);

        // Calculate difference = total_quantity - original_forecast
        BigDecimal difference = totalQuantity.subtract(plan.getOriginalForecast());
        plan.setDifference(difference);

        ProductionPlan savedPlan = productionPlanRepository.save(plan);

        log.info("Updated production plan ID: {}, totalQuantity: {}, difference: {}",
                id, totalQuantity, difference);

        return ProductionPlanDTO.fromEntity(savedPlan);
    }

    private BigDecimal calculateMonthlySum(Map<String, BigDecimal> monthlyAllocation) {
        if (monthlyAllocation == null || monthlyAllocation.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return monthlyAllocation.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
