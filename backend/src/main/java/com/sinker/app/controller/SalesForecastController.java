package com.sinker.app.controller;

import com.sinker.app.dto.forecast.CreateForecastRequest;
import com.sinker.app.dto.forecast.ForecastResponse;
import com.sinker.app.dto.forecast.UpdateForecastRequest;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.security.JwtUserPrincipal;
import com.sinker.app.service.SalesForecastService;
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
@RequestMapping("/api/sales-forecast")
public class SalesForecastController {

    private static final Logger log = LoggerFactory.getLogger(SalesForecastController.class);

    private final SalesForecastService forecastService;

    public SalesForecastController(SalesForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sales_forecast.create')")
    public ResponseEntity<ForecastResponse> createForecast(
            @Valid @RequestBody CreateForecastRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("POST /api/sales-forecast - user={}, month={}, channel={}, productCode={}",
                principal.getUserId(), request.getMonth(), request.getChannel(), request.getProductCode());

        ForecastResponse response = forecastService.createForecast(
                request, principal.getUserId(), principal.getRoleCode());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sales_forecast.edit')")
    public ResponseEntity<ForecastResponse> updateForecast(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateForecastRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("PUT /api/sales-forecast/{} - user={}, quantity={}",
                id, principal.getUserId(), request.getQuantity());

        ForecastResponse response = forecastService.updateForecast(
                id, request, principal.getUserId(), principal.getRoleCode());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sales_forecast.delete')")
    public ResponseEntity<Void> deleteForecast(
            @PathVariable Integer id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("DELETE /api/sales-forecast/{} - user={}", id, principal.getUserId());

        forecastService.deleteForecast(id, principal.getUserId(), principal.getRoleCode());

        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(SalesForecastService.DuplicateEntryException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEntry(
            SalesForecastService.DuplicateEntryException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Duplicate entry",
                ex.getMessage(), request.getRequestURI());
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
