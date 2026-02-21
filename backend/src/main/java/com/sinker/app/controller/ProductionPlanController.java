package com.sinker.app.controller;

import com.sinker.app.dto.productionplan.ProductionPlanDTO;
import com.sinker.app.dto.productionplan.UpdateProductionPlanRequest;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.security.JwtUserPrincipal;
import com.sinker.app.service.ProductionPlanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/production-plan")
public class ProductionPlanController {

    private static final Logger log = LoggerFactory.getLogger(ProductionPlanController.class);

    private final ProductionPlanService productionPlanService;

    public ProductionPlanController(ProductionPlanService productionPlanService) {
        this.productionPlanService = productionPlanService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('production_plan.view')")
    public ResponseEntity<List<ProductionPlanDTO>> getProductionPlans(
            @RequestParam Integer year,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("GET /api/production-plan - user={}, year={}", principal.getUserId(), year);

        if (year == null) {
            throw new IllegalArgumentException("year parameter is required");
        }

        List<ProductionPlanDTO> plans = productionPlanService.queryProductionPlans(year);

        return ResponseEntity.ok(plans);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('production_plan.edit')")
    public ResponseEntity<ProductionPlanDTO> updateProductionPlan(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateProductionPlanRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("PUT /api/production-plan/{} - user={}", id, principal.getUserId());

        ProductionPlanDTO updatedPlan = productionPlanService.updateProductionPlan(id, request);

        return ResponseEntity.ok(updatedPlan);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden",
                "Insufficient permissions: " + ex.getMessage(), request.getRequestURI());
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String error, String message, String path) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", error,
                "message", message,
                "path", path
        ));
    }
}
