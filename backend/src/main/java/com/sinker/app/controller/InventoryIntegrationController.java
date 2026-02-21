package com.sinker.app.controller;

import com.sinker.app.dto.forecast.InventoryIntegrationDTO;
import com.sinker.app.security.JwtUserPrincipal;
import com.sinker.app.service.InventoryIntegrationService;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/inventory-integration")
public class InventoryIntegrationController {

    private static final Logger log = LoggerFactory.getLogger(InventoryIntegrationController.class);

    private final InventoryIntegrationService inventoryIntegrationService;

    public InventoryIntegrationController(InventoryIntegrationService inventoryIntegrationService) {
        this.inventoryIntegrationService = inventoryIntegrationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory.view')")
    public ResponseEntity<List<InventoryIntegrationDTO>> queryInventoryIntegration(
            @RequestParam String month,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String version,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("GET /api/inventory-integration - user={}, month={}, startDate={}, endDate={}, version={}",
                principal.getUserId(), month, startDate, endDate, version);

        // Validate required parameters
        if (month == null || month.isEmpty()) {
            throw new IllegalArgumentException("month parameter is required");
        }

        List<InventoryIntegrationDTO> results = inventoryIntegrationService.queryInventoryIntegration(
                month, startDate, endDate, version);

        return ResponseEntity.ok(results);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden",
                "Insufficient permissions: inventory.view required", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error in inventory integration: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Failed to fetch data from ERP system", request.getRequestURI());
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
