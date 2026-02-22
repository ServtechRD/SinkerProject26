package com.sinker.app.service;

import com.sinker.app.dto.forecast.InventoryIntegrationDTO;
import com.sinker.app.entity.InventorySalesForecast;
import com.sinker.app.entity.SalesForecast;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.InventorySalesForecastRepository;
import com.sinker.app.repository.SalesForecastRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(InventoryIntegrationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SalesForecastRepository salesForecastRepository;
    private final InventorySalesForecastRepository inventoryForecastRepository;
    private final ErpInventoryService erpInventoryService;

    public InventoryIntegrationService(
            SalesForecastRepository salesForecastRepository,
            InventorySalesForecastRepository inventoryForecastRepository,
            ErpInventoryService erpInventoryService) {
        this.salesForecastRepository = salesForecastRepository;
        this.inventoryForecastRepository = inventoryForecastRepository;
        this.erpInventoryService = erpInventoryService;
    }

    /**
     * Query inventory integration data
     * If version is provided, retrieve saved data
     * If version is null, perform real-time query and save results
     */
    @Transactional
    public List<InventoryIntegrationDTO> queryInventoryIntegration(
            String month, String startDate, String endDate, String version) {

        log.info("queryInventoryIntegration: month={}, startDate={}, endDate={}, version={}",
                month, startDate, endDate, version);

        // Version query mode: retrieve saved data
        if (version != null && !version.isEmpty()) {
            return loadSavedData(month, version);
        }

        // Real-time query mode: aggregate and save
        return performRealTimeQuery(month, startDate, endDate);
    }

    /**
     * Update modified subtotal by creating a new version
     * This preserves the audit trail by not modifying the original record
     */
    @Transactional
    public InventoryIntegrationDTO updateModifiedSubtotal(Integer id, BigDecimal modifiedSubtotal) {
        log.info("updateModifiedSubtotal: id={}, modifiedSubtotal={}", id, modifiedSubtotal);

        // Load existing record
        InventorySalesForecast original = inventoryForecastRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory integration record with ID " + id + " not found"));

        // Create new record with all fields copied from original
        InventorySalesForecast newRecord = new InventorySalesForecast();
        newRecord.setMonth(original.getMonth());
        newRecord.setProductCode(original.getProductCode());
        newRecord.setProductName(original.getProductName());
        newRecord.setCategory(original.getCategory());
        newRecord.setSpec(original.getSpec());
        newRecord.setWarehouseLocation(original.getWarehouseLocation());
        newRecord.setSalesQuantity(original.getSalesQuantity());
        newRecord.setInventoryBalance(original.getInventoryBalance());
        newRecord.setForecastQuantity(original.getForecastQuantity());
        newRecord.setProductionSubtotal(original.getProductionSubtotal());
        newRecord.setModifiedSubtotal(modifiedSubtotal);
        newRecord.setQueryStartDate(original.getQueryStartDate());
        newRecord.setQueryEndDate(original.getQueryEndDate());

        // Generate new version
        String newVersion = generateVersion();
        newRecord.setVersion(newVersion);

        // Save new record (original remains unchanged)
        InventorySalesForecast savedRecord = inventoryForecastRepository.save(newRecord);

        log.info("Created new version: oldId={}, newId={}, newVersion={}",
                id, savedRecord.getId(), newVersion);

        return toDTO(savedRecord);
    }

    /**
     * Load saved data from database by version
     */
    private List<InventoryIntegrationDTO> loadSavedData(String month, String version) {
        log.info("Loading saved data for month={}, version={}", month, version);

        List<InventorySalesForecast> entities = inventoryForecastRepository
                .findByMonthAndVersionOrderByProductCodeAsc(month, version);

        if (entities.isEmpty()) {
            log.info("No saved data found for month={}, version={}", month, version);
            return Collections.emptyList();
        }

        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Perform real-time query: aggregate forecast, call ERP, calculate, and save
     */
    private List<InventoryIntegrationDTO> performRealTimeQuery(String month, String startDate, String endDate) {
        log.info("Performing real-time query for month={}", month);

        // Determine date range for sales query
        String[] dateRange = determineDateRange(month, startDate, endDate);
        String queryStartDate = dateRange[0];
        String queryEndDate = dateRange[1];

        log.info("Query date range: {} to {}", queryStartDate, queryEndDate);

        // Step 1: Get latest version of forecast data for the month
        String forecastVersion = getLatestForecastVersion(month);
        if (forecastVersion == null) {
            log.warn("No forecast data found for month={}", month);
            return Collections.emptyList();
        }

        log.info("Using forecast version: {}", forecastVersion);

        // Step 2: Aggregate forecast data (sum all 12 channels per product)
        List<SalesForecast> forecasts = salesForecastRepository.findByMonthAndVersion(month, forecastVersion);
        Map<ProductKey, AggregatedProduct> aggregatedProducts = aggregateForecasts(forecasts);

        log.info("Aggregated {} products from {} forecast records", aggregatedProducts.size(), forecasts.size());

        // Step 3: For each product, call ERP and calculate production_subtotal
        List<InventorySalesForecast> results = new ArrayList<>();
        String newVersion = generateVersion();

        for (Map.Entry<ProductKey, AggregatedProduct> entry : aggregatedProducts.entrySet()) {
            ProductKey key = entry.getKey();
            AggregatedProduct product = entry.getValue();

            // Call ERP services
            BigDecimal inventoryBalance = erpInventoryService.getInventoryBalance(key.productCode, month);
            BigDecimal salesQuantity = erpInventoryService.getSalesQuantity(key.productCode, queryStartDate, queryEndDate);

            // Calculate production_subtotal = forecast_quantity - inventory_balance - sales_quantity
            BigDecimal productionSubtotal = product.forecastQuantity
                    .subtract(inventoryBalance)
                    .subtract(salesQuantity);

            // Create entity
            InventorySalesForecast entity = new InventorySalesForecast();
            entity.setMonth(month);
            entity.setProductCode(key.productCode);
            entity.setProductName(key.productName);
            entity.setCategory(key.category);
            entity.setSpec(key.spec);
            entity.setWarehouseLocation(key.warehouseLocation);
            entity.setForecastQuantity(product.forecastQuantity);
            entity.setInventoryBalance(inventoryBalance);
            entity.setSalesQuantity(salesQuantity);
            entity.setProductionSubtotal(productionSubtotal);
            entity.setVersion(newVersion);
            entity.setQueryStartDate(queryStartDate);
            entity.setQueryEndDate(queryEndDate);

            results.add(entity);
        }

        // Step 4: Save all results in single transaction
        log.info("Saving {} records with version={}", results.size(), newVersion);
        List<InventorySalesForecast> savedEntities = inventoryForecastRepository.saveAll(results);

        // Step 5: Convert to DTOs and return
        return savedEntities.stream()
                .sorted(Comparator.comparing(InventorySalesForecast::getProductCode))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Aggregate forecast data: sum all 12 channels per product
     */
    private Map<ProductKey, AggregatedProduct> aggregateForecasts(List<SalesForecast> forecasts) {
        Map<ProductKey, AggregatedProduct> productMap = new HashMap<>();

        for (SalesForecast forecast : forecasts) {
            ProductKey key = new ProductKey(
                    forecast.getProductCode(),
                    forecast.getProductName(),
                    forecast.getCategory(),
                    forecast.getSpec(),
                    forecast.getWarehouseLocation()
            );

            AggregatedProduct product = productMap.computeIfAbsent(key, k -> new AggregatedProduct());
            product.forecastQuantity = product.forecastQuantity.add(forecast.getQuantity());
        }

        return productMap;
    }

    /**
     * Get latest forecast version for the month
     */
    private String getLatestForecastVersion(String month) {
        List<String> versions = salesForecastRepository.findDistinctVersionsByMonth(month);
        return versions.isEmpty() ? null : versions.get(0);
    }

    /**
     * Determine date range for sales query
     * If startDate/endDate not provided, use month boundaries
     */
    private String[] determineDateRange(String month, String startDate, String endDate) {
        String start = startDate;
        String end = endDate;

        if (start == null || start.isEmpty() || end == null || end.isEmpty()) {
            // Use month boundaries
            LocalDate monthStart = LocalDate.parse(month + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

            start = monthStart.format(DATE_FORMATTER);
            end = monthEnd.format(DATE_FORMATTER);
        }

        return new String[]{start, end};
    }

    /**
     * Generate unique version identifier
     */
    private String generateVersion() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "v" + timestamp;
    }

    /**
     * Convert entity to DTO
     */
    private InventoryIntegrationDTO toDTO(InventorySalesForecast entity) {
        InventoryIntegrationDTO dto = new InventoryIntegrationDTO();
        dto.setId(entity.getId());
        dto.setMonth(entity.getMonth());
        dto.setProductCode(entity.getProductCode());
        dto.setProductName(entity.getProductName());
        dto.setCategory(entity.getCategory());
        dto.setSpec(entity.getSpec());
        dto.setWarehouseLocation(entity.getWarehouseLocation());
        dto.setSalesQuantity(entity.getSalesQuantity());
        dto.setInventoryBalance(entity.getInventoryBalance());
        dto.setForecastQuantity(entity.getForecastQuantity());
        dto.setProductionSubtotal(entity.getProductionSubtotal());
        dto.setModifiedSubtotal(entity.getModifiedSubtotal());
        dto.setVersion(entity.getVersion());
        dto.setQueryStartDate(entity.getQueryStartDate());
        dto.setQueryEndDate(entity.getQueryEndDate());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    /**
     * Product key for grouping forecasts
     */
    private static class ProductKey {
        final String productCode;
        final String productName;
        final String category;
        final String spec;
        final String warehouseLocation;

        ProductKey(String productCode, String productName, String category, String spec, String warehouseLocation) {
            this.productCode = productCode;
            this.productName = productName;
            this.category = category;
            this.spec = spec;
            this.warehouseLocation = warehouseLocation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProductKey that = (ProductKey) o;
            return Objects.equals(productCode, that.productCode) &&
                    Objects.equals(productName, that.productName) &&
                    Objects.equals(category, that.category) &&
                    Objects.equals(spec, that.spec) &&
                    Objects.equals(warehouseLocation, that.warehouseLocation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productCode, productName, category, spec, warehouseLocation);
        }
    }

    /**
     * Aggregated product data
     */
    private static class AggregatedProduct {
        BigDecimal forecastQuantity = BigDecimal.ZERO;
    }
}
