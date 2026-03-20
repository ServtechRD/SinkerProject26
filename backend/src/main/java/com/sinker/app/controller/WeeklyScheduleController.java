package com.sinker.app.controller;

import com.sinker.app.dto.schedule.UploadScheduleResponse;
import com.sinker.app.dto.schedule.UpdateScheduleRequest;
import com.sinker.app.dto.schedule.WeeklyScheduleDTO;
import com.sinker.app.exception.ExcelParseException;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.service.WeeklyScheduleService;
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
@RequestMapping("/api/weekly-schedule")
public class WeeklyScheduleController {

    private static final Logger log = LoggerFactory.getLogger(WeeklyScheduleController.class);

    private final WeeklyScheduleService service;

    public WeeklyScheduleController(WeeklyScheduleService service) {
        this.service = service;
    }

    @GetMapping("/factories")
    public ResponseEntity<List<String>> getFactories() {
        log.info("GET /api/weekly-schedule/factories");
        return ResponseEntity.ok(service.getFactories());
    }

    @GetMapping("/template/{factory}")
    @PreAuthorize("hasAuthority('weekly_schedule.view')")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable String factory) {
        log.info("GET /api/weekly-schedule/template/{}", factory);
        byte[] excelBytes = service.generateTemplate(factory);
        String filename = "weekly_schedule_template_" + factory + ".xlsx";
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

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('weekly_schedule.upload')")
    public ResponseEntity<UploadScheduleResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("week_start") String weekStart,
            @RequestParam("factory") String factory) {

        log.info("POST /api/weekly-schedule/upload - weekStart={}, factory={}", weekStart, factory);

        UploadScheduleResponse response = service.upload(file, weekStart, factory);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('weekly_schedule.view')")
    public ResponseEntity<List<WeeklyScheduleDTO>> getSchedules(
            @RequestParam("week_start") String weekStart,
            @RequestParam("factory") String factory) {

        log.info("GET /api/weekly-schedule - weekStart={}, factory={}", weekStart, factory);

        List<WeeklyScheduleDTO> schedules = service.getSchedules(weekStart, factory);

        return ResponseEntity.ok(schedules);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('weekly_schedule.edit')")
    public ResponseEntity<WeeklyScheduleDTO> updateSchedule(
            @PathVariable Integer id,
            @RequestBody UpdateScheduleRequest request) {

        log.info("PUT /api/weekly-schedule/{} - demandDate={}, quantity={}",
                id, request.getDemandDate(), request.getQuantity());

        WeeklyScheduleDTO updated = service.updateSchedule(id, request);

        return ResponseEntity.ok(updated);
    }

    @ExceptionHandler(ExcelParseException.class)
    public ResponseEntity<Map<String, Object>> handleExcelParseException(
            ExcelParseException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request",
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
