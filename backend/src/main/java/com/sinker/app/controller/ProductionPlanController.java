package com.sinker.app.controller;

import com.sinker.app.dto.productionplan.ProductionFormRangeResponse;
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
    public ResponseEntity<?> getProductionForm(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String start_month,
            @RequestParam(required = false) String end_month,
            @RequestParam(required = false) String version,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        if (start_month != null && end_month != null) {
            log.info("GET /api/production-plan - user={}, start_month={}, end_month={}, version={}",
                    principal.getUserId(), start_month, end_month, version);
            ProductionFormRangeResponse resp = productionFormService.getProductionFormByMonthRange(start_month, end_month, version);
            return ResponseEntity.ok(resp);
        }
        if (year != null) {
            log.info("GET /api/production-plan - user={}, year={}", principal.getUserId(), year);
            List<ProductionFormRowDTO> rows = productionFormService.getProductionForm(year);
            return ResponseEntity.ok(rows);
        }
        throw new IllegalArgumentException("Either year or (start_month and end_month) is required");
    }

    @GetMapping("/versions")
    @PreAuthorize("hasAuthority('production_plan.view')")
    public ResponseEntity<List<String>> listProductionPlanVersions(
            @RequestParam String start_month,
            @RequestParam String end_month,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("GET /api/production-plan/versions - user={}, start_month={}, end_month={}",
                principal.getUserId(), start_month, end_month);

        List<String> versions = productionFormService.listInventoryVersionsInRange(start_month, end_month);
        return ResponseEntity.ok(versions);
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
