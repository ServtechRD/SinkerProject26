package com.sinker.app.service;

import com.sinker.app.dto.productionplan.ProductionFormChannelDTO;
import com.sinker.app.dto.productionplan.ProductionFormRowDTO;
import com.sinker.app.entity.InventorySalesForecast;
import com.sinker.app.entity.ProductionForm;
import com.sinker.app.entity.SalesForecast;
import com.sinker.app.repository.InventorySalesForecastRepository;
import com.sinker.app.repository.ProductionFormRepository;
import com.sinker.app.repository.SalesForecastRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductionFormService {

    private static final Logger log = LoggerFactory.getLogger(ProductionFormService.class);
    private static final List<String> CHANNEL_ORDER = List.of(
            "PX + 大全聯", "家樂福", "愛買", "7-11", "全家", "Ok+萊爾富",
            "好市多", "楓康", "美聯社", "康是美", "電商", "市面經銷");
    private static final List<String> MONTH_KEYS = List.of("2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");

    private final SalesForecastRepository salesForecastRepository;
    private final InventorySalesForecastRepository inventoryForecastRepository;
    private final ProductionFormRepository productionFormRepository;

    public ProductionFormService(SalesForecastRepository salesForecastRepository,
                                 InventorySalesForecastRepository inventoryForecastRepository,
                                 ProductionFormRepository productionFormRepository) {
        this.salesForecastRepository = salesForecastRepository;
        this.inventoryForecastRepository = inventoryForecastRepository;
        this.productionFormRepository = productionFormRepository;
    }

    /**
     * Build production form data for the given year.
     * Products = union from sales forecast (months 2-12). Channel months from sales forecast, aggregate months from inventory integration 修改後小計.
     */
    @Transactional(readOnly = true)
    public List<ProductionFormRowDTO> getProductionForm(int year) {
        log.info("Building production form for year {}", year);

        Map<ProductKey, ProductInfo> productMap = new LinkedHashMap<>();
        Map<ProductKey, Map<String, Map<String, BigDecimal>>> channelMonths = new LinkedHashMap<>(); // product -> channel -> monthKey -> qty
        Map<ProductKey, Map<String, BigDecimal>> aggregateMonths = new LinkedHashMap<>(); // product -> monthKey -> modified_subtotal
        Map<String, ProductionForm> formByProduct = productionFormRepository.findByYearOrderByProductCodeAsc(year)
                .stream().collect(Collectors.toMap(ProductionForm::getProductCode, f -> f, (a, b) -> a));

        for (String monthKey : MONTH_KEYS) {
            String month = year + String.format("%02d", Integer.parseInt(monthKey));

            List<String> versions = salesForecastRepository.findDistinctVersionsByMonth(month);
            if (!versions.isEmpty()) {
                List<SalesForecast> forecasts = salesForecastRepository.findByMonthAndVersion(month, versions.get(0));
                for (SalesForecast f : forecasts) {
                    ProductKey key = new ProductKey(f.getProductCode(), f.getWarehouseLocation(), f.getCategory(), f.getSpec(), f.getProductName());
                    productMap.putIfAbsent(key, new ProductInfo(f.getWarehouseLocation(), f.getCategory(), f.getSpec(), f.getProductName(), f.getProductCode()));
                    channelMonths
                            .computeIfAbsent(key, k -> new LinkedHashMap<>())
                            .computeIfAbsent(f.getChannel(), c -> new LinkedHashMap<>())
                            .put(monthKey, f.getQuantity());
                }
            }

            List<String> invVersions = inventoryForecastRepository.findDistinctVersionsByMonth(month);
            if (!invVersions.isEmpty()) {
                List<InventorySalesForecast> inv = inventoryForecastRepository.findByMonthAndVersionOrderByProductCodeAsc(month, invVersions.get(0));
                for (InventorySalesForecast i : inv) {
                    ProductKey key = new ProductKey(i.getProductCode(), i.getWarehouseLocation(), i.getCategory(), i.getSpec(), i.getProductName());
                    productMap.putIfAbsent(key, new ProductInfo(i.getWarehouseLocation(), i.getCategory(), i.getSpec(), i.getProductName(), i.getProductCode()));
                    aggregateMonths
                            .computeIfAbsent(key, k -> new LinkedHashMap<>())
                            .put(monthKey, i.getModifiedSubtotal() != null ? i.getModifiedSubtotal() : BigDecimal.ZERO);
                }
            }
        }

        List<ProductionFormRowDTO> rows = new ArrayList<>();
        for (Map.Entry<ProductKey, ProductInfo> e : productMap.entrySet()) {
            ProductKey key = e.getKey();
            ProductInfo info = e.getValue();
            ProductionFormRowDTO row = new ProductionFormRowDTO();
            row.setWarehouseLocation(info.warehouseLocation);
            row.setCategory(info.category);
            row.setSpec(info.spec);
            row.setProductName(info.productName);
            row.setProductCode(info.productCode);

            Map<String, Map<String, BigDecimal>> chan = channelMonths.getOrDefault(key, new LinkedHashMap<>());
            BigDecimal originalForecast = BigDecimal.ZERO;
            for (String ch : CHANNEL_ORDER) {
                ProductionFormChannelDTO cd = new ProductionFormChannelDTO();
                cd.setChannel(ch);
                Map<String, BigDecimal> months = chan.getOrDefault(ch, new LinkedHashMap<>());
                BigDecimal channelTotal = BigDecimal.ZERO;
                for (String mk : MONTH_KEYS) {
                    BigDecimal v = months.getOrDefault(mk, BigDecimal.ZERO);
                    cd.getMonths().put(mk, v);
                    channelTotal = channelTotal.add(v);
                }
                cd.setTotal(channelTotal);
                originalForecast = originalForecast.add(channelTotal);
                row.getChannelData().add(cd);
            }
            row.setOriginalForecast(originalForecast);

            Map<String, BigDecimal> agg = aggregateMonths.getOrDefault(key, new LinkedHashMap<>());
            BigDecimal aggregateSum = BigDecimal.ZERO;
            for (String mk : MONTH_KEYS) {
                BigDecimal v = agg.getOrDefault(mk, BigDecimal.ZERO);
                row.getAggregateMonths().put(mk, v);
                aggregateSum = aggregateSum.add(v);
            }
            ProductionForm form = formByProduct.get(key.productCode);
            BigDecimal buffer = form != null ? form.getBufferQuantity() : BigDecimal.ZERO;
            row.setBufferQuantity(buffer);
            row.setAggregateTotal(aggregateSum.add(buffer));
            row.setDifference(originalForecast.subtract(row.getAggregateTotal()));
            row.setRemarks(form != null ? form.getRemarks() : null);
            row.setProductionFormId(form != null ? form.getId() : null);
            rows.add(row);
        }

        rows.sort(Comparator.comparing(ProductionFormRowDTO::getCategory)
                .thenComparing(ProductionFormRowDTO::getSpec)
                .thenComparing(ProductionFormRowDTO::getProductCode));
        log.info("Production form rows: {}", rows.size());
        return rows;
    }

    @Transactional
    public ProductionFormRowDTO updateBuffer(Integer year, String productCode, BigDecimal bufferQuantity) {
        ProductionForm form = productionFormRepository.findByYearAndProductCode(year, productCode)
                .orElseGet(() -> {
                    ProductionForm newForm = new ProductionForm();
                    newForm.setYear(year);
                    newForm.setProductCode(productCode);
                    newForm.setBufferQuantity(bufferQuantity != null ? bufferQuantity : BigDecimal.ZERO);
                    return productionFormRepository.save(newForm);
                });
        form.setBufferQuantity(bufferQuantity != null ? bufferQuantity : BigDecimal.ZERO);
        productionFormRepository.save(form);
        return null;
    }

    private static class ProductKey {
        final String productCode;
        final String warehouseLocation;
        final String category;
        final String spec;
        final String productName;

        ProductKey(String productCode, String warehouseLocation, String category, String spec, String productName) {
            this.productCode = productCode;
            this.warehouseLocation = warehouseLocation != null ? warehouseLocation : "";
            this.category = category != null ? category : "";
            this.spec = spec != null ? spec : "";
            this.productName = productName != null ? productName : "";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProductKey that = (ProductKey) o;
            return Objects.equals(productCode, that.productCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productCode);
        }
    }

    private static class ProductInfo {
        final String warehouseLocation;
        final String category;
        final String spec;
        final String productName;
        final String productCode;

        ProductInfo(String warehouseLocation, String category, String spec, String productName, String productCode) {
            this.warehouseLocation = warehouseLocation;
            this.category = category;
            this.spec = spec;
            this.productName = productName;
            this.productCode = productCode;
        }
    }
}
