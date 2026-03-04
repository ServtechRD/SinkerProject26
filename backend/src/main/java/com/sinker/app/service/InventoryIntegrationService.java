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
     * Normalize month to YYYYMM (sales_forecast uses YYYYMM).
     * Accepts YYYY-MM or YYYYMM.
     */
    private static String normalizeMonth(String month) {
        if (month == null || month.isEmpty()) return month;
        if (month.matches("\\d{6}")) return month;
        if (month.matches("\\d{4}-\\d{2}")) {
            return month.replace("-", "");
        }
        return month;
    }

    /**
     * Query inventory integration data
     * If version is provided, retrieve saved data (month optional when querying by version).
     * If version is null, perform real-time query based on sales forecast and save results.
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

        // Real-time query mode: month required, aggregate from sales forecast and save
        if (month == null || month.isEmpty()) {
            throw new IllegalArgumentException("month parameter is required when not querying by version");
        }
        String normalizedMonth = normalizeMonth(month);
        return performRealTimeQuery(normalizedMonth, startDate, endDate);
    }

    /**
     * Update modified subtotal in place for the given record.
     */
    @Transactional
    public InventoryIntegrationDTO updateModifiedSubtotal(Integer id, BigDecimal modifiedSubtotal) {
        log.info("updateModifiedSubtotal: id={}, modifiedSubtotal={}", id, modifiedSubtotal);

        InventorySalesForecast entity = inventoryForecastRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory integration record with ID " + id + " not found"));

        entity.setModifiedSubtotal(modifiedSubtotal);
        InventorySalesForecast saved = inventoryForecastRepository.save(entity);
        return toDTO(saved);
    }

    /**
     * List distinct versions. If month is provided, filter by month (newest first); otherwise return all versions.
     */
    public List<String> getVersions(String month) {
        if (month != null && !month.isEmpty()) {
            return inventoryForecastRepository.findDistinctVersionsByMonth(normalizeMonth(month));
        }
        return inventoryForecastRepository.findDistinctVersionsOrderByVersionDesc();
    }

    /**
     * Copy all records of the given version to a new version. Month is taken from the source records.
     */
    @Transactional
    public String copyVersion(String version) {
        log.info("copyVersion: version={}", version);

        List<InventorySalesForecast> source = inventoryForecastRepository
                .findByVersionOrderByProductCodeAsc(version);

        if (source.isEmpty()) {
            throw new ResourceNotFoundException("No data found for version=" + version);
        }

        String month = source.get(0).getMonth();
        String newVersion = generateVersion();
        List<InventorySalesForecast> copies = new ArrayList<>();

        for (InventorySalesForecast orig : source) {
            InventorySalesForecast copy = new InventorySalesForecast();
            copy.setMonth(orig.getMonth());
            copy.setProductCode(orig.getProductCode());
            copy.setProductName(orig.getProductName());
            copy.setCategory(orig.getCategory());
            copy.setSpec(orig.getSpec());
            copy.setWarehouseLocation(orig.getWarehouseLocation());
            copy.setSalesQuantity(orig.getSalesQuantity());
            copy.setInventoryBalance(orig.getInventoryBalance());
            copy.setForecastQuantity(orig.getForecastQuantity());
            copy.setProductionSubtotal(orig.getProductionSubtotal());
            copy.setModifiedSubtotal(orig.getModifiedSubtotal());
            copy.setQueryStartDate(orig.getQueryStartDate());
            copy.setQueryEndDate(orig.getQueryEndDate());
            copy.setVersion(newVersion);
            copies.add(copy);
        }

        inventoryForecastRepository.saveAll(copies);
        log.info("Copied {} records to new version {} (month={})", copies.size(), newVersion, month);
        return newVersion;
    }

    /**
     * Load saved data from database by version. If month is null or empty, load by version only.
     */
    private List<InventoryIntegrationDTO> loadSavedData(String month, String version) {
        log.info("Loading saved data for month={}, version={}", month, version);

        List<InventorySalesForecast> entities;
        if (month != null && !month.isEmpty()) {
            entities = inventoryForecastRepository
                    .findByMonthAndVersionOrderByProductCodeAsc(normalizeMonth(month), version);
        } else {
            entities = inventoryForecastRepository.findByVersionOrderByProductCodeAsc(version);
        }

        if (entities.isEmpty()) {
            log.info("No saved data found for version={}", version);
            return Collections.emptyList();
        }

        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Perform real-time query: aggregate from sales forecast (base data), call ERP for 銷貨/結存, calculate, and save.
     * Month must be YYYYMM (normalized before call).
     */
    private List<InventoryIntegrationDTO> performRealTimeQuery(String month, String startDate, String endDate) {
        log.info("Performing real-time query for month={} (sales forecast as base)", month);

        // Determine date range for ERP/sales query
        String[] dateRange = determineDateRange(month, startDate, endDate);
        String queryStartDate = dateRange[0];
        String queryEndDate = dateRange[1];

        log.info("Query date range: {} to {}", queryStartDate, queryEndDate);

        // Step 1: Get latest version of sales forecast for the month (data source)
        String forecastVersion = getLatestForecastVersion(month);
        if (forecastVersion == null) {
            log.warn("No sales forecast data found for month={}", month);
            return Collections.emptyList();
        }

        log.info("Using sales forecast version: {}", forecastVersion);

        // Step 2: Aggregate forecast data (sum all channels per product) - this is the base set of products
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
     * Determine date range for ERP/sales query.
     * If startDate/endDate not provided, use month boundaries (month is YYYYMM).
     */
    private String[] determineDateRange(String month, String startDate, String endDate) {
        String start = startDate;
        String end = endDate;

        if (start == null || start.isEmpty() || end == null || end.isEmpty()) {
            // month is YYYYMM
            int y = Integer.parseInt(month.substring(0, 4));
            int m = Integer.parseInt(month.substring(4, 6));
            LocalDate monthStart = LocalDate.of(y, m, 1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            start = monthStart.format(DATE_FORMATTER);
            end = monthEnd.format(DATE_FORMATTER);
        }

        return new String[]{start, end};
    }

    /**
     * Generate version string, same format as sales forecast: yyyy-MM-dd HH:mm:ss
     */
    private String generateVersion() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
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
