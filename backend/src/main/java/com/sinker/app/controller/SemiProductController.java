package com.sinker.app.controller;

import com.sinker.app.dto.semiproduct.SemiProductDTO;
import com.sinker.app.dto.semiproduct.SemiProductUpdateDTO;
import com.sinker.app.dto.semiproduct.SemiProductUploadResponse;
import com.sinker.app.exception.ExcelParseException;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.service.SemiProductService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/semi-product")
public class SemiProductController {

    private static final Logger log = LoggerFactory.getLogger(SemiProductController.class);

    private final SemiProductService semiProductService;

    public SemiProductController(SemiProductService semiProductService) {
        this.semiProductService = semiProductService;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('semi_product.upload')")
    public ResponseEntity<SemiProductUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        log.info("POST /api/semi-product/upload - filename={}", file.getOriginalFilename());

        SemiProductUploadResponse response = semiProductService.upload(file);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('semi_product.view')")
    public ResponseEntity<List<SemiProductDTO>> findAll() {
        log.info("GET /api/semi-product");

        List<SemiProductDTO> products = semiProductService.findAll();

        return ResponseEntity.ok(products);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('semi_product.edit')")
    public ResponseEntity<SemiProductDTO> update(
            @PathVariable Integer id,
            @RequestBody SemiProductUpdateDTO updateDTO) {
        log.info("PUT /api/semi-product/{} - advanceDays={}", id, updateDTO.getAdvanceDays());

        SemiProductDTO updated = semiProductService.update(id, updateDTO);

        return ResponseEntity.ok(updated);
    }

    @GetMapping("/template")
    @PreAuthorize("hasAuthority('semi_product.view')")
    public ResponseEntity<byte[]> downloadTemplate() {
        log.info("GET /api/semi-product/template");

        byte[] excelBytes = semiProductService.generateTemplate();

        String filename = "semi_product_template.xlsx";
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
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Semi-product not found",
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
