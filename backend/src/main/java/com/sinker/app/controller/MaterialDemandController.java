package com.sinker.app.controller;

import com.sinker.app.dto.materialdemand.MaterialDemandDTO;
import com.sinker.app.dto.materialdemand.MaterialDemandUpdateDTO;
import com.sinker.app.exception.ExcelParseException;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.security.JwtUserPrincipal;
import com.sinker.app.service.MaterialDemandService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
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

        if (weekStart == null) {
            throw new IllegalArgumentException("Required parameter 'week_start' is missing");
        }
        if (factory == null || factory.isEmpty()) {
            throw new IllegalArgumentException("Required parameter 'factory' is missing");
        }

        List<MaterialDemandDTO> demands = materialDemandService.queryMaterialDemand(weekStart, factory);

        return ResponseEntity.ok(demands);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('material_demand.edit')")
    public ResponseEntity<MaterialDemandDTO> update(
            @PathVariable Integer id,
            @RequestBody MaterialDemandUpdateDTO dto) {
        log.info("PUT /api/material-demand/{}", id);
        MaterialDemandDTO updated = materialDemandService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/confirm-send-erp")
    @PreAuthorize("hasAuthority('confirm_data_send_erp')")
    public ResponseEntity<Map<String, Object>> confirmSendErp(
            @RequestParam("week_start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam String factory) {
        log.info("POST /api/material-demand/confirm-send-erp - weekStart={}, factory={}", weekStart, factory);
        materialDemandService.confirmSendErp(weekStart, factory);
        return ResponseEntity.ok(Map.of("message", "Confirm send ERP successful"));
    }

    @GetMapping("/pending-confirm")
    @PreAuthorize("hasAuthority('confirm_data_send_erp')")
    public ResponseEntity<List<Map<String, Object>>> getPendingConfirm() {
        log.info("GET /api/material-demand/pending-confirm");
        return ResponseEntity.ok(materialDemandService.getPendingConfirm());
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('material_demand.upload')")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("week_start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam String factory) {
        log.info("POST /api/material-demand/upload - weekStart={}, factory={}", weekStart, factory);
        int count = materialDemandService.upload(file, weekStart, factory);
        return ResponseEntity.ok(Map.of("count", count, "message", "Upload successful"));
    }

    @GetMapping("/template/{factory}")
    @PreAuthorize("hasAuthority('material_demand.view')")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable String factory) {
        log.info("GET /api/material-demand/template/{}", factory);
        byte[] excelBytes = materialDemandService.generateTemplate(factory);
        String filename = "material_demand_template_" + factory + ".xlsx";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build());
        headers.setContentLength(excelBytes.length);
        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }

    @ExceptionHandler(ExcelParseException.class)
    public ResponseEntity<Map<String, Object>> handleExcelParseException(
            ExcelParseException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 400,
                "error", "Validation failed",
                "details", ex.getErrors(),
                "path", request.getRequestURI()
        ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request.getRequestURI());
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
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden", "Insufficient permissions", request.getRequestURI());
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
