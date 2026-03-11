package com.sinker.app.controller;

import com.sinker.app.dto.forecast.UploadResponse;
import com.sinker.app.exception.ExcelParseException;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.security.JwtUserPrincipal;
import com.sinker.app.service.ExcelTemplateService;
import com.sinker.app.service.GiftSalesForecastUploadService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gift-sales-forecast")
public class GiftSalesForecastUploadController {

    private static final Logger log = LoggerFactory.getLogger(GiftSalesForecastUploadController.class);

    private final GiftSalesForecastUploadService uploadService;
    private final ExcelTemplateService templateService;

    public GiftSalesForecastUploadController(GiftSalesForecastUploadService uploadService,
                                             ExcelTemplateService templateService) {
        this.uploadService = uploadService;
        this.templateService = templateService;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('sales_forecast.upload')")
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("month") String month,
            @RequestParam("channel") String channel,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("POST /api/gift-sales-forecast/upload - user={}, month={}, channel={}",
                principal.getUserId(), month, channel);

        UploadResponse response = uploadService.upload(file, month, channel,
                principal.getUserId(), principal.getRoleCode());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/template")
    @PreAuthorize("hasAnyAuthority('sales_forecast.upload', 'sales_forecast.view')")
    public ResponseEntity<byte[]> downloadTemplate(@RequestParam("channel") String channel) {
        log.info("GET /api/gift-sales-forecast/template?channel={}", channel);

        byte[] excelBytes = templateService.generateTemplate(channel);

        String safeChannel = channel != null ? channel.replace("/", "_") : "channel";
        String filename = "gift_sales_forecast_template_" + safeChannel + ".xlsx";
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
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed",
                ex.getErrors(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                List.of(ex.getMessage()), request.getRequestURI());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found",
                List.of(ex.getMessage()), request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden",
                List.of(ex.getMessage()), request.getRequestURI());
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String error, List<String> details, String path) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", error,
                "details", details,
                "path", path
        ));
    }
}
