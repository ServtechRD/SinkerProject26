package com.sinker.app.service;

import com.sinker.app.dto.forecast.IntegrationRowDTO;
import com.sinker.app.entity.SalesForecast;
import com.sinker.app.repository.SalesForecastRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ForecastIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(ForecastIntegrationService.class);

    // Channel name mapping to DTO setter methods
    private static final Map<String, String> CHANNEL_MAPPING = Map.ofEntries(
            Map.entry("PX/大全聯", "qtyPx"),
            Map.entry("家樂福", "qtyCarrefour"),
            Map.entry("愛買", "qtyAimall"),
            Map.entry("711", "qty711"),
            Map.entry("7-11", "qty711"), // Alternative name
            Map.entry("全家", "qtyFamilymart"),
            Map.entry("OK/萊爾富", "qtyOk"),
            Map.entry("萊爾富", "qtyOk"), // Alternative name
            Map.entry("OK超商", "qtyOk"), // Alternative name
            Map.entry("好市多", "qtyCostco"),
            Map.entry("楓康", "qtyFkmart"),
            Map.entry("美聯社", "qtyWellsociety"),
            Map.entry("康是美", "qtyCosmed"),
            Map.entry("電商", "qtyEcommerce"),
            Map.entry("市面經銷", "qtyDistributor")
    );

    private final SalesForecastRepository forecastRepository;

    public ForecastIntegrationService(SalesForecastRepository forecastRepository) {
        this.forecastRepository = forecastRepository;
    }

    @Transactional(readOnly = true)
    public List<IntegrationRowDTO> queryIntegration(String month, String version) {
        long startTime = System.currentTimeMillis();

        log.info("Starting integration query for month={}, version={}", month, version);

        // If version is null, get latest version
        String targetVersion = version;
        if (targetVersion == null || targetVersion.isEmpty()) {
            targetVersion = getLatestVersion(month);
            log.info("Using latest version: {}", targetVersion);
        }

        // Get all forecast data for the month and version
        List<SalesForecast> forecasts = forecastRepository.findByMonthAndVersion(month, targetVersion);

        if (forecasts.isEmpty()) {
            log.info("No forecast data found for month={}, version={}", month, targetVersion);
            return Collections.emptyList();
        }

        // Group by product code
        Map<String, IntegrationRowDTO> productMap = new HashMap<>();

        for (SalesForecast forecast : forecasts) {
            String productCode = forecast.getProductCode();

            IntegrationRowDTO row = productMap.computeIfAbsent(productCode, k -> {
                IntegrationRowDTO newRow = new IntegrationRowDTO();
                newRow.setProductCode(productCode);
                newRow.setProductName(forecast.getProductName());
                newRow.setCategory(forecast.getCategory());
                newRow.setSpec(forecast.getSpec());
                newRow.setWarehouseLocation(forecast.getWarehouseLocation());
                return newRow;
            });

            // Set quantity for the appropriate channel
            String channel = forecast.getChannel();
            String fieldName = CHANNEL_MAPPING.get(channel);

            if (fieldName != null) {
                setChannelQuantity(row, fieldName, forecast.getQuantity());
            } else {
                log.warn("Unknown channel: {} for product: {}", channel, productCode);
            }
        }

        // Calculate subtotals
        for (IntegrationRowDTO row : productMap.values()) {
            BigDecimal subtotal = calculateSubtotal(row);
            row.setOriginalSubtotal(subtotal);
        }

        // Calculate differences with previous version
        String previousVersion = getPreviousVersion(month, targetVersion);
        if (previousVersion != null) {
            calculateDifferences(productMap, month, previousVersion);
        }

        // Generate remarks (skip rows already marked as "新增產品")
        for (IntegrationRowDTO row : productMap.values()) {
            if (row.getRemarks() == null) {
                row.setRemarks(generateRemarks(row.getDifference()));
            }
        }

        // Sort by category code
        List<IntegrationRowDTO> result = new ArrayList<>(productMap.values());
        result.sort(this::compareByCategoryCode);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Integration query completed in {}ms, returned {} products", duration, result.size());

        return result;
    }

    private String getLatestVersion(String month) {
        List<String> versions = forecastRepository.findDistinctVersionsByMonth(month);
        return versions.isEmpty() ? null : versions.get(0);
    }

    private String getPreviousVersion(String month, String currentVersion) {
        List<String> versions = forecastRepository.findDistinctVersionsByMonth(month);
        int currentIndex = versions.indexOf(currentVersion);

        // versions are in DESC order, so previous version is at currentIndex + 1
        if (currentIndex >= 0 && currentIndex < versions.size() - 1) {
            return versions.get(currentIndex + 1);
        }

        return null;
    }

    private void calculateDifferences(Map<String, IntegrationRowDTO> productMap, String month, String previousVersion) {
        List<SalesForecast> previousForecasts = forecastRepository.findByMonthAndVersion(month, previousVersion);

        // Build previous version product map
        Map<String, BigDecimal> previousSubtotals = new HashMap<>();
        Map<String, IntegrationRowDTO> previousProductMap = new HashMap<>();

        for (SalesForecast forecast : previousForecasts) {
            String productCode = forecast.getProductCode();

            IntegrationRowDTO prevRow = previousProductMap.computeIfAbsent(productCode, k -> {
                IntegrationRowDTO newRow = new IntegrationRowDTO();
                newRow.setProductCode(productCode);
                return newRow;
            });

            String fieldName = CHANNEL_MAPPING.get(forecast.getChannel());
            if (fieldName != null) {
                setChannelQuantity(prevRow, fieldName, forecast.getQuantity());
            }
        }

        // Calculate previous subtotals
        for (Map.Entry<String, IntegrationRowDTO> entry : previousProductMap.entrySet()) {
            BigDecimal prevSubtotal = calculateSubtotal(entry.getValue());
            previousSubtotals.put(entry.getKey(), prevSubtotal);
        }

        // Calculate differences
        for (Map.Entry<String, IntegrationRowDTO> entry : productMap.entrySet()) {
            String productCode = entry.getKey();
            IntegrationRowDTO currentRow = entry.getValue();
            BigDecimal currentSubtotal = currentRow.getOriginalSubtotal();

            if (!previousSubtotals.containsKey(productCode)) {
                // New product not present in previous version
                currentRow.setDifference(currentSubtotal);
                currentRow.setRemarks("新增產品");
            } else {
                BigDecimal previousSubtotal = previousSubtotals.get(productCode);
                currentRow.setDifference(currentSubtotal.subtract(previousSubtotal));
            }
        }
    }

    private BigDecimal calculateSubtotal(IntegrationRowDTO row) {
        return row.getQtyPx()
                .add(row.getQtyCarrefour())
                .add(row.getQtyAimall())
                .add(row.getQty711())
                .add(row.getQtyFamilymart())
                .add(row.getQtyOk())
                .add(row.getQtyCostco())
                .add(row.getQtyFkmart())
                .add(row.getQtyWellsociety())
                .add(row.getQtyCosmed())
                .add(row.getQtyEcommerce())
                .add(row.getQtyDistributor());
    }

    private void setChannelQuantity(IntegrationRowDTO row, String fieldName, BigDecimal quantity) {
        switch (fieldName) {
            case "qtyPx" -> row.setQtyPx(quantity);
            case "qtyCarrefour" -> row.setQtyCarrefour(quantity);
            case "qtyAimall" -> row.setQtyAimall(quantity);
            case "qty711" -> row.setQty711(quantity);
            case "qtyFamilymart" -> row.setQtyFamilymart(quantity);
            case "qtyOk" -> row.setQtyOk(quantity);
            case "qtyCostco" -> row.setQtyCostco(quantity);
            case "qtyFkmart" -> row.setQtyFkmart(quantity);
            case "qtyWellsociety" -> row.setQtyWellsociety(quantity);
            case "qtyCosmed" -> row.setQtyCosmed(quantity);
            case "qtyEcommerce" -> row.setQtyEcommerce(quantity);
            case "qtyDistributor" -> row.setQtyDistributor(quantity);
        }
    }

    private String generateRemarks(BigDecimal difference) {
        if (difference == null || difference.compareTo(BigDecimal.ZERO) == 0) {
            return "無變化";
        } else if (difference.compareTo(BigDecimal.ZERO) > 0) {
            return "數量增加";
        } else {
            return "數量減少";
        }
    }

    private int compareByCategoryCode(IntegrationRowDTO a, IntegrationRowDTO b) {
        // Extract category codes for sorting
        CategoryCode codeA = extractCategoryCode(a.getCategory());
        CategoryCode codeB = extractCategoryCode(b.getCategory());

        // Compare by category first
        int categoryCompare = Integer.compare(codeA.category, codeB.category);
        if (categoryCompare != 0) {
            return categoryCompare;
        }

        // Then by flavor
        int flavorCompare = Integer.compare(codeA.flavor, codeB.flavor);
        if (flavorCompare != 0) {
            return flavorCompare;
        }

        // Finally by product code as tiebreaker
        return nullSafeCompare(a.getProductCode(), b.getProductCode());
    }

    private CategoryCode extractCategoryCode(String category) {
        if (category == null || category.isEmpty()) {
            return new CategoryCode(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        // Pattern to extract 2-digit category + 2-digit flavor (e.g., "01飲料類" -> 01, "0102" -> 01, 02)
        Pattern pattern = Pattern.compile("^(\\d{2})(\\d{2})?");
        Matcher matcher = pattern.matcher(category);

        if (matcher.find()) {
            int cat = Integer.parseInt(matcher.group(1));
            int flavor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            return new CategoryCode(cat, flavor);
        }

        // Fallback: no numeric code found
        return new CategoryCode(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    private int nullSafeCompare(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return a.compareTo(b);
    }

    private static class CategoryCode {
        final int category;
        final int flavor;

        CategoryCode(int category, int flavor) {
            this.category = category;
            this.flavor = flavor;
        }
    }
}
