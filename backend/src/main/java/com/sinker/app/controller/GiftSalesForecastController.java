package com.sinker.app.controller;

import com.sinker.app.dto.forecast.CopyVersionResponse;
import com.sinker.app.dto.forecast.CreateForecastRequest;
import com.sinker.app.dto.forecast.ForecastResponse;
import com.sinker.app.dto.forecast.SaveVersionReasonRequest;
import com.sinker.app.dto.forecast.UpdateForecastRequest;
import com.sinker.app.dto.forecast.VersionDiffItemDTO;
import com.sinker.app.dto.forecast.VersionInfo;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.service.GiftSalesForecastService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gift-sales-forecast")
public class GiftSalesForecastController {

    private static final Logger log = LoggerFactory.getLogger(GiftSalesForecastController.class);

    private final GiftSalesForecastService forecastService;

    public GiftSalesForecastController(GiftSalesForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('sales_forecast.view', 'sales_forecast.view_own')")
    public ResponseEntity<List<ForecastResponse>> queryForecasts(
            @RequestParam String month,
            @RequestParam String channel,
            @RequestParam(required = false) String version,
            @AuthenticationPrincipal com.sinker.app.security.JwtUserPrincipal principal,
            Authentication authentication) {

        if (month == null || month.isEmpty() || channel == null || channel.isEmpty()) {
            throw new IllegalArgumentException("Missing required parameter: month or channel");
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        List<ForecastResponse> forecasts = forecastService.queryForecasts(
                month, channel, version, principal.getUserId(), authorities);
        return ResponseEntity.ok(forecasts);
    }

    @PostMapping("/copy-version")
    @PreAuthorize("hasAuthority('sales_forecast.update_after_closed')")
    public ResponseEntity<CopyVersionResponse> copyLatestToNewVersion(
            @RequestParam String month,
            @RequestParam String channel,
            @AuthenticationPrincipal com.sinker.app.security.JwtUserPrincipal principal) {

        if (month == null || month.isEmpty() || channel == null || channel.isEmpty()) {
            throw new IllegalArgumentException("Missing month or channel");
        }
        CopyVersionResponse response = forecastService.copyLatestToNewVersion(
                month, channel, principal.getUserId(), principal.getRoleCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/versions/reason")
    @PreAuthorize("hasAuthority('sales_forecast.update_after_closed')")
    public ResponseEntity<Void> saveVersionReason(
            @RequestParam String month,
            @RequestParam String channel,
            @RequestParam String version,
            @Valid @RequestBody SaveVersionReasonRequest request,
            @AuthenticationPrincipal com.sinker.app.security.JwtUserPrincipal principal) {

        if (month == null || month.isEmpty() || channel == null || channel.isEmpty() || version == null || version.isEmpty()) {
            throw new IllegalArgumentException("Missing month, channel or version");
        }
        forecastService.saveVersionReason(month, channel, version, request.getChangeReason());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/versions")
    @PreAuthorize("hasAuthority('sales_forecast.update_after_closed')")
    public ResponseEntity<Void> deleteVersion(
            @RequestParam String month,
            @RequestParam String channel,
            @RequestParam String version,
            @AuthenticationPrincipal com.sinker.app.security.JwtUserPrincipal principal) {

        if (month == null || month.isEmpty() || channel == null || channel.isEmpty() || version == null || version.isEmpty()) {
            throw new IllegalArgumentException("Missing month, channel or version");
        }
        forecastService.deleteVersion(month, channel, version, principal.getUserId(), principal.getRoleCode());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/versions/diff")
    @PreAuthorize("hasAnyAuthority('sales_forecast.view', 'sales_forecast.view_own')")
    public ResponseEntity<List<VersionDiffItemDTO>> getVersionDiff(
            @RequestParam String month,
            @RequestParam String channel,
            @RequestParam String version,
            @AuthenticationPrincipal com.sinker.app.security.JwtUserPrincipal principal,
            Authentication authentication) {

        if (month == null || month.isEmpty() || channel == null || channel.isEmpty() || version == null || version.isEmpty()) {
            throw new IllegalArgumentException("Missing month, channel or version");
        }
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        List<VersionDiffItemDTO> diff = forecastService.getVersionDiff(
                month, channel, version, principal.getUserId(), authorities);
        return ResponseEntity.ok(diff);
    }

    @GetMapping("/versions")
    @PreAuthorize("hasAnyAuthority('sales_forecast.view', 'sales_forecast.view_own')")
    public ResponseEntity<List<VersionInfo>> queryVersions(
            @RequestParam String month,
            @RequestParam String channel,
            @AuthenticationPrincipal com.sinker.app.security.JwtUserPrincipal principal,
            Authentication authentication) {

        if (month == null || month.isEmpty() || channel == null || channel.isEmpty()) {
            throw new IllegalArgumentException("Missing required parameter: month or channel");
        }
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        List<VersionInfo> versions = forecastService.queryVersions(
                month, channel, principal.getUserId(), authorities);
        return ResponseEntity.ok(versions);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sales_forecast.create')")
    public ResponseEntity<ForecastResponse> createForecast(
            @Valid @RequestBody CreateForecastRequest request,
            @AuthenticationPrincipal com.sinker.app.security.JwtUserPrincipal principal,
            Authentication authentication) {

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        ForecastResponse response = forecastService.createForecast(
                request, principal.getUserId(), principal.getRoleCode(), authorities);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sales_forecast.edit')")
    public ResponseEntity<ForecastResponse> updateForecast(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateForecastRequest request,
            @AuthenticationPrincipal com.sinker.app.security.JwtUserPrincipal principal,
            Authentication authentication) {

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        ForecastResponse response = forecastService.updateForecast(
                id, request, principal.getUserId(), principal.getRoleCode(), authorities);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sales_forecast.delete')")
    public ResponseEntity<Void> deleteForecast(
            @PathVariable Integer id,
            @AuthenticationPrincipal com.sinker.app.security.JwtUserPrincipal principal) {

        forecastService.deleteForecast(id, principal.getUserId(), principal.getRoleCode());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(GiftSalesForecastService.DuplicateEntryException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEntry(
            GiftSalesForecastService.DuplicateEntryException ex, HttpServletRequest request) {
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
