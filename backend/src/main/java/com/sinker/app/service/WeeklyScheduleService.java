package com.sinker.app.service;

import com.sinker.app.dto.schedule.UploadScheduleResponse;
import com.sinker.app.dto.schedule.UpdateScheduleRequest;
import com.sinker.app.dto.schedule.WeeklyScheduleDTO;
import com.sinker.app.entity.WeeklySchedule;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.WeeklyScheduleRepository;
import com.sinker.app.service.WeeklyScheduleExcelParser.WeeklyScheduleRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WeeklyScheduleService {

    private static final Logger log = LoggerFactory.getLogger(WeeklyScheduleService.class);

    private final WeeklyScheduleRepository repository;
    private final WeeklyScheduleExcelParser excelParser;
    private final PdcaIntegrationService pdcaIntegrationService;

    public WeeklyScheduleService(WeeklyScheduleRepository repository,
                                 WeeklyScheduleExcelParser excelParser,
                                 @Lazy PdcaIntegrationService pdcaIntegrationService) {
        this.repository = repository;
        this.excelParser = excelParser;
        this.pdcaIntegrationService = pdcaIntegrationService;
    }

    @Transactional
    public UploadScheduleResponse upload(MultipartFile file, String weekStartStr, String factory) {
        long startTime = System.currentTimeMillis();
        log.info("Starting upload: weekStart={}, factory={}", weekStartStr, factory);

        // 1. Parse and validate week_start
        LocalDate weekStart = parseWeekStart(weekStartStr);
        validateMonday(weekStart);

        // 2. Validate factory
        if (factory == null || factory.trim().isEmpty()) {
            throw new IllegalArgumentException("factory parameter is required");
        }

        // 3. Parse Excel
        List<WeeklyScheduleRow> rows = excelParser.parse(file);

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Excel file has no valid data rows");
        }

        // 4. Delete existing data for week_start + factory
        repository.deleteByWeekStartAndFactory(weekStart, factory);
        log.info("Deleted existing data for weekStart={}, factory={}", weekStart, factory);

        // 5. Insert all rows
        LocalDateTime now = LocalDateTime.now();
        List<WeeklySchedule> entities = rows.stream()
                .map(row -> {
                    WeeklySchedule entity = new WeeklySchedule();
                    entity.setWeekStart(weekStart);
                    entity.setFactory(factory);
                    entity.setDemandDate(row.getDemandDate());
                    entity.setProductCode(row.getProductCode());
                    entity.setProductName(row.getProductName());
                    entity.setWarehouseLocation(row.getWarehouseLocation());
                    entity.setQuantity(row.getQuantity());
                    entity.setCreatedAt(now);
                    entity.setUpdatedAt(now);
                    return entity;
                })
                .collect(Collectors.toList());

        repository.saveAll(entities);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Upload complete: weekStart={}, factory={}, rows={}, duration={}ms",
                weekStart, factory, rows.size(), duration);

        // Trigger PDCA integration asynchronously
        pdcaIntegrationService.triggerPdcaIntegration(entities, weekStart, factory);

        return new UploadScheduleResponse(
                "Upload successful",
                rows.size(),
                weekStart,
                factory
        );
    }

    public List<WeeklyScheduleDTO> getSchedules(String weekStartStr, String factory) {
        // 1. Parse week_start
        LocalDate weekStart = parseWeekStart(weekStartStr);

        // 2. Validate factory
        if (factory == null || factory.trim().isEmpty()) {
            throw new IllegalArgumentException("factory parameter is required");
        }

        // 3. Query schedules
        List<WeeklySchedule> entities = repository.findByWeekStartAndFactoryOrderByDemandDateAscProductCodeAsc(
                weekStart, factory);

        return entities.stream()
                .map(WeeklyScheduleDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public WeeklyScheduleDTO updateSchedule(Integer id, UpdateScheduleRequest request) {
        // 1. Load existing record
        WeeklySchedule entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));

        // 2. Validate and update fields
        if (request.getDemandDate() != null) {
            entity.setDemandDate(request.getDemandDate());
        }

        if (request.getQuantity() != null) {
            if (request.getQuantity().compareTo(java.math.BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("quantity must be >= 0");
            }
            entity.setQuantity(request.getQuantity());
        }

        entity.setUpdatedAt(LocalDateTime.now());

        // 3. Save and return
        WeeklySchedule updated = repository.save(entity);
        log.info("Updated schedule id={}: demandDate={}, quantity={}",
                id, updated.getDemandDate(), updated.getQuantity());

        // 4. Re-trigger PDCA integration for this week+factory
        List<WeeklySchedule> schedules = repository.findByWeekStartAndFactoryOrderByDemandDateAscProductCodeAsc(
                updated.getWeekStart(), updated.getFactory());
        pdcaIntegrationService.triggerPdcaIntegration(schedules, updated.getWeekStart(), updated.getFactory());

        return WeeklyScheduleDTO.fromEntity(updated);
    }

    private LocalDate parseWeekStart(String weekStartStr) {
        if (weekStartStr == null || weekStartStr.trim().isEmpty()) {
            throw new IllegalArgumentException("week_start parameter is required");
        }

        try {
            return LocalDate.parse(weekStartStr.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid week_start format. Expected YYYY-MM-DD, got: " + weekStartStr);
        }
    }

    private void validateMonday(LocalDate weekStart) {
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            String dayName = weekStart.getDayOfWeek().toString();
            dayName = dayName.charAt(0) + dayName.substring(1).toLowerCase();
            throw new IllegalArgumentException(
                    "week_start must be a Monday. Provided date: " + weekStart + " (" + dayName + ")");
        }
    }
}
