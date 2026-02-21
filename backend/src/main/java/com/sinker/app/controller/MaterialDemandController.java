package com.sinker.app.controller;

import com.sinker.app.dto.materialdemand.MaterialDemandDTO;
import com.sinker.app.security.JwtUserPrincipal;
import com.sinker.app.service.MaterialDemandService;
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
@RequestMapping("/api/material-demand")
public class MaterialDemandController {

    private static final Logger log = LoggerFactory.getLogger(MaterialDemandController.class);

    private final MaterialDemandService materialDemandService;

    public MaterialDemandController(MaterialDemandService materialDemandService) {
        this.materialDemandService = materialDemandService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('material_demand.view')")
    public ResponseEntity<List<MaterialDemandDTO>> queryMaterialDemand(
            @RequestParam("week_start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam String factory,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("GET /api/material-demand - user={}, weekStart={}, factory={}",
                principal.getUserId(), weekStart, factory);

        // Validate required parameters
        if (weekStart == null) {
            throw new IllegalArgumentException("Required parameter 'week_start' is missing");
        }
        if (factory == null || factory.isEmpty()) {
            throw new IllegalArgumentException("Required parameter 'factory' is missing");
        }

        List<MaterialDemandDTO> demands = materialDemandService.queryMaterialDemand(weekStart, factory);

        return ResponseEntity.ok(demands);
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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden",
                "Insufficient permissions", request.getRequestURI());
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
