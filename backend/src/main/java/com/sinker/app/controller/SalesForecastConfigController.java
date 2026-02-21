package com.sinker.app.controller;

import com.sinker.app.dto.forecast.ConfigResponse;
import com.sinker.app.dto.forecast.CreateMonthsRequest;
import com.sinker.app.dto.forecast.CreateMonthsResponse;
import com.sinker.app.dto.forecast.UpdateConfigRequest;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.service.SalesForecastConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales-forecast/config")
public class SalesForecastConfigController {

    private static final Logger log = LoggerFactory.getLogger(SalesForecastConfigController.class);

    private final SalesForecastConfigService service;

    public SalesForecastConfigController(SalesForecastConfigService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sales_forecast_config.edit')")
    public ResponseEntity<CreateMonthsResponse> createMonths(
            @Valid @RequestBody CreateMonthsRequest request) {
        log.info("POST /api/sales-forecast/config - Creating months from {} to {}",
                request.getStartMonth(), request.getEndMonth());
        CreateMonthsResponse response = service.batchCreateMonths(
                request.getStartMonth(), request.getEndMonth());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('sales_forecast_config.view')")
    public ResponseEntity<List<ConfigResponse>> listConfigs() {
        log.info("GET /api/sales-forecast/config - Listing all configs");
        List<ConfigResponse> configs = service.listAll();
        return ResponseEntity.ok(configs);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sales_forecast_config.edit')")
    public ResponseEntity<ConfigResponse> updateConfig(
            @PathVariable Integer id,
            @RequestBody UpdateConfigRequest request) {
        log.info("PUT /api/sales-forecast/config/{} - Updating config", id);
        ConfigResponse response = service.updateConfig(id, request);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(SalesForecastConfigService.DuplicateMonthException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateMonth(
            SalesForecastConfigService.DuplicateMonthException ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Conflict",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                ex.getMessage(), request.getRequestURI());
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
