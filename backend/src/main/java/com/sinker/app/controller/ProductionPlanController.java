package com.sinker.app.controller;

import com.sinker.app.dto.productionplan.ProductionFormRowDTO;
import com.sinker.app.dto.productionplan.UpdateBufferRequest;
import com.sinker.app.dto.productionplan.UpdateProductionPlanRequest;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.security.JwtUserPrincipal;
import com.sinker.app.service.ProductionFormService;
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
    private final ProductionFormService productionFormService;

    public ProductionPlanController(ProductionPlanService productionPlanService,
                                    ProductionFormService productionFormService) {
        this.productionPlanService = productionPlanService;
        this.productionFormService = productionFormService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('production_plan.view')")
    public ResponseEntity<List<ProductionFormRowDTO>> getProductionForm(
            @RequestParam Integer year,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("GET /api/production-plan - user={}, year={}", principal.getUserId(), year);

        if (year == null) {
            throw new IllegalArgumentException("year parameter is required");
        }

        List<ProductionFormRowDTO> rows = productionFormService.getProductionForm(year);
        return ResponseEntity.ok(rows);
    }

    @PutMapping("/buffer")
    @PreAuthorize("hasAuthority('production_plan.edit')")
    public ResponseEntity<Void> updateBuffer(
            @Valid @RequestBody UpdateBufferRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("PUT /api/production-plan/buffer - user={}, year={}, productCode={}",
                principal.getUserId(), request.getYear(), request.getProductCode());

        productionFormService.updateBuffer(request.getYear(), request.getProductCode(), request.getBufferQuantity());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('production_plan.edit')")
    public ResponseEntity<com.sinker.app.dto.productionplan.ProductionPlanDTO> updateProductionPlan(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateProductionPlanRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("PUT /api/production-plan/{} - user={}", id, principal.getUserId());

        com.sinker.app.dto.productionplan.ProductionPlanDTO updatedPlan = productionPlanService.updateProductionPlan(id, request);
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
