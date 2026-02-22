package com.sinker.app.controller;

import com.sinker.app.dto.materialpurchase.MaterialPurchaseDTO;
import com.sinker.app.exception.AlreadyTriggeredErpException;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.security.JwtUserPrincipal;
import com.sinker.app.service.MaterialPurchaseService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/material-purchase")
public class MaterialPurchaseController {

    private static final Logger log = LoggerFactory.getLogger(MaterialPurchaseController.class);

    private final MaterialPurchaseService materialPurchaseService;

    public MaterialPurchaseController(MaterialPurchaseService materialPurchaseService) {
        this.materialPurchaseService = materialPurchaseService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('material_purchase.view')")
    public ResponseEntity<List<MaterialPurchaseDTO>> queryMaterialPurchase(
            @RequestParam("week_start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam String factory,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("GET /api/material-purchase - user={}, weekStart={}, factory={}",
                principal.getUserId(), weekStart, factory);

        // Validate required parameters
        if (weekStart == null) {
            throw new IllegalArgumentException("Required parameter 'week_start' is missing");
        }
        if (factory == null || factory.isEmpty()) {
            throw new IllegalArgumentException("Required parameter 'factory' is missing");
        }

        List<MaterialPurchaseDTO> purchases = materialPurchaseService.queryMaterialPurchase(weekStart, factory);

        return ResponseEntity.ok(purchases);
    }

    @PostMapping("/{id}/trigger-erp")
    @PreAuthorize("hasAuthority('material_purchase.trigger_erp')")
    public ResponseEntity<MaterialPurchaseDTO> triggerErp(
            @PathVariable Integer id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("POST /api/material-purchase/{}/trigger-erp - user={}", id, principal.getUserId());

        MaterialPurchaseDTO result = materialPurchaseService.triggerErp(id);

        return ResponseEntity.ok(result);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(
            org.springframework.web.bind.MissingServletRequestParameterException ex, HttpServletRequest request) {
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                message, request.getRequestURI());
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("Invalid format for parameter '%s'", ex.getName());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                message, request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden",
                "Insufficient permissions", request.getRequestURI());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AlreadyTriggeredErpException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyTriggeredErp(
            AlreadyTriggeredErpException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Conflict",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        log.error("Runtime exception occurred", ex);
        String message = "Failed to create ERP order: " + ex.getMessage();
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                message, request.getRequestURI());
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
