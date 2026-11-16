package com.sinker.app.controller;

import com.sinker.app.dto.forecast.ConfigResponse;
import com.sinker.app.dto.forecast.CreateMonthsRequest;
import com.sinker.app.dto.forecast.EditStatusResponse;
import com.sinker.app.dto.forecast.FormDetailResponse;
import com.sinker.app.dto.forecast.FormLogDTO;
import com.sinker.app.dto.forecast.ReopenRequest;
import com.sinker.app.dto.forecast.UpdateConfigRequest;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.security.JwtUserPrincipal;
import com.sinker.app.service.ForecastFormDetailService;
import com.sinker.app.service.ReopenService;
import com.sinker.app.service.SalesForecastConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales-forecast/config")
public class SalesForecastConfigController {

    private static final Logger log = LoggerFactory.getLogger(SalesForecastConfigController.class);

    private final SalesForecastConfigService service;
    private final ReopenService reopenService;
    private final ForecastFormDetailService detailService;

    public SalesForecastConfigController(SalesForecastConfigService service, ReopenService reopenService,
                                         ForecastFormDetailService detailService) {
        this.service = service;
        this.reopenService = reopenService;
        this.detailService = detailService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sales_forecast_config.edit')")
    public ResponseEntity<ConfigResponse> createMonth(
            @Valid @RequestBody CreateMonthsRequest request) {
        log.info("POST /api/sales-forecast/config - Creating month {}, autoCloseAt={}",
                request.getMonth(), request.getAutoCloseAt());
        LocalDateTime autoCloseAt = SalesForecastConfigService.parseDateTime(request.getAutoCloseAt());
        ConfigResponse response = service.createMonth(request.getMonth(), autoCloseAt);
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
            @RequestBody UpdateConfigRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        log.info("PUT /api/sales-forecast/config/{} - Updating config", id);
        ConfigResponse response = service.updateConfig(id, request, principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAuthority('sales_forecast_config.reopen')")
    public ResponseEntity<Void> reopen(
            @PathVariable Integer id,
            @Valid @RequestBody ReopenRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        log.info("POST /api/sales-forecast/config/{}/reopen - channels={}", id, request.getChannels());
        reopenService.open(id, request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{id}/reopen/{windowId}/end")
    @PreAuthorize("hasAuthority('sales_forecast_config.reopen')")
    public ResponseEntity<Void> endReopen(
            @PathVariable Integer id,
            @PathVariable Integer windowId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        log.info("POST /api/sales-forecast/config/{}/reopen/{}/end", id, windowId);
        reopenService.end(id, windowId, principal.getUserId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/detail")
    @PreAuthorize("hasAuthority('sales_forecast_config.view')")
    public ResponseEntity<FormDetailResponse> getFormDetail(@PathVariable Integer id) {
        log.info("GET /api/sales-forecast/config/{}/detail", id);
        return ResponseEntity.ok(detailService.getFormDetail(id));
    }

    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAuthority('sales_forecast_config.view')")
    public ResponseEntity<List<FormLogDTO>> getFormLogs(@PathVariable Integer id) {
        log.info("GET /api/sales-forecast/config/{}/logs", id);
        return ResponseEntity.ok(detailService.getLogs(id));
    }

    @GetMapping("/edit-status")
    @PreAuthorize("hasAuthority('sales_forecast_config.view')")
    public ResponseEntity<EditStatusResponse> getEditStatus(@RequestParam String month, @RequestParam String channel) {
        log.info("GET /api/sales-forecast/config/edit-status - month={}, channel={}", month, channel);
        return ResponseEntity.ok(detailService.getEditStatus(month, channel));
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
