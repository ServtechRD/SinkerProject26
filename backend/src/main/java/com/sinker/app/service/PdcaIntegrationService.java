package com.sinker.app.service;

import com.sinker.app.dto.materialdemand.MaterialDemandDTO;
import com.sinker.app.dto.pdca.PdcaRequest;
import com.sinker.app.dto.pdca.PdcaResponse;
import com.sinker.app.entity.MaterialDemand;
import com.sinker.app.entity.WeeklySchedule;
import com.sinker.app.repository.MaterialDemandRepository;
import com.sinker.app.repository.WeeklyScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for integrating with PDCA API to calculate material requirements.
 * Orchestrates the flow: build request → call PDCA API → save results to material_demand table.
 */
@Service
public class PdcaIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(PdcaIntegrationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PdcaApiClient pdcaApiClient;
    private final MaterialDemandRepository materialDemandRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;

    public PdcaIntegrationService(PdcaApiClient pdcaApiClient,
                                    MaterialDemandRepository materialDemandRepository,
                                    WeeklyScheduleRepository weeklyScheduleRepository) {
        this.pdcaApiClient = pdcaApiClient;
        this.materialDemandRepository = materialDemandRepository;
        this.weeklyScheduleRepository = weeklyScheduleRepository;
    }

    /**
     * 查詢物料需求：依週排程組 PDCA 請求 → 呼叫外部 PDCA（或 URL 未設定時 {@link PdcaLocalStub}）→ 覆寫該週+廠區 material_demand 並回傳。
     */
    @Transactional
    public List<MaterialDemandDTO> syncMaterialDemandFromPdca(LocalDate weekStart, String factory) {
        log.info("Sync material demand from PDCA: weekStart={}, factory={}", weekStart, factory);
        List<WeeklySchedule> schedules = weeklyScheduleRepository
                .findByWeekStartAndFactoryOrderByDemandDateAscProductCodeAsc(weekStart, factory);
        PdcaRequest request = buildPdcaRequest(schedules);
        try {
            PdcaResponse response = pdcaApiClient.calculateMaterialRequirements(request, weekStart, factory);
            materialDemandRepository.deleteByWeekStartAndFactory(weekStart, factory);
            List<MaterialDemand> demands = mapResponseToEntities(response, weekStart, factory);
            materialDemandRepository.saveAll(demands);
            return demands.stream()
                    .map(MaterialDemandDTO::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("syncMaterialDemandFromPdca failed: weekStart={}, factory={}", weekStart, factory, e);
            throw new RuntimeException("PDCA 物料需求查詢失敗: " + e.getMessage(), e);
        }
    }

    /**
     * Triggers PDCA integration for the given weekly schedules.
     * Runs asynchronously to avoid blocking the upload response.
     *
     * @param schedules List of weekly schedule records
     * @param weekStart Week start date
     * @param factory Factory identifier
     */
    @Async
    @Transactional
    public void triggerPdcaIntegration(List<WeeklySchedule> schedules, LocalDate weekStart, String factory) {
        log.info("PDCA integration started: weekStart={}, factory={}, schedules={}",
                weekStart, factory, schedules.size());

        try {
            // 1. Build PDCA request from schedule data
            PdcaRequest request = buildPdcaRequest(schedules);

            // 2. Call PDCA API
            PdcaResponse response = pdcaApiClient.calculateMaterialRequirements(request, weekStart, factory);

            // 3. Delete existing material_demand records for this week+factory
            materialDemandRepository.deleteByWeekStartAndFactory(weekStart, factory);
            log.info("Deleted existing material demand records: weekStart={}, factory={}", weekStart, factory);

            // 4. Map response to MaterialDemand entities and save
            List<MaterialDemand> demands = mapResponseToEntities(response, weekStart, factory);
            materialDemandRepository.saveAll(demands);

            log.info("PDCA integration completed successfully: weekStart={}, factory={}, materials={}",
                    weekStart, factory, demands.size());

        } catch (Exception e) {
            // Log error but do not fail the upload
            log.error("PDCA integration failed: weekStart={}, factory={}, error={}",
                    weekStart, factory, e.getMessage(), e);
        }
    }

    /**
     * Builds PDCA request from weekly schedule records.
     */
    private PdcaRequest buildPdcaRequest(List<WeeklySchedule> schedules) {
        List<PdcaRequest.ScheduleItem> items = schedules.stream()
                .map(schedule -> new PdcaRequest.ScheduleItem(
                        schedule.getProductCode(),
                        schedule.getQuantity().doubleValue(),
                        schedule.getDemandDate().format(DATE_FORMATTER)
                ))
                .collect(Collectors.toList());

        return new PdcaRequest(items);
    }

    /**
     * Maps PDCA response to MaterialDemand entities.
     */
    private List<MaterialDemand> mapResponseToEntities(PdcaResponse response,
                                                       LocalDate weekStart,
                                                       String factory) {
        LocalDateTime now = LocalDateTime.now();

        return response.getMaterials().stream()
                .map(item -> {
                    MaterialDemand demand = new MaterialDemand();
                    demand.setWeekStart(weekStart);
                    demand.setFactory(factory);
                    demand.setMaterialCode(item.getMaterialCode());
                    demand.setMaterialName(item.getMaterialName());
                    demand.setUnit(item.getUnit());
                    demand.setDemandDate(LocalDate.parse(item.getDemandDate(), DATE_FORMATTER));
                    demand.setExpectedDelivery(BigDecimal.valueOf(item.getExpectedDelivery()));
                    demand.setDemandQuantity(BigDecimal.valueOf(item.getDemandQuantity()));
                    demand.setEstimatedInventory(BigDecimal.valueOf(item.getEstimatedInventory()));
                    demand.setLastPurchaseDate(null); // Set by purchase module later
                    demand.setCreatedAt(now);
                    demand.setUpdatedAt(now);
                    return demand;
                })
                .collect(Collectors.toList());
    }
}
