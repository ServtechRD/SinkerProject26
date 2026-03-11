package com.sinker.app.service;

import com.sinker.app.dto.forecast.FormSummaryResponse;
import com.sinker.app.dto.forecast.FormSummaryRowDTO;
import com.sinker.app.dto.productionplan.ProductionFormChannelDTO;
import com.sinker.app.dto.productionplan.ProductionFormRangeResponse;
import com.sinker.app.dto.productionplan.ProductionFormRowDTO;
import com.sinker.app.entity.InventorySalesForecast;
import com.sinker.app.entity.ProductionForm;
import com.sinker.app.entity.SalesForecast;
import com.sinker.app.repository.InventorySalesForecastRepository;
import com.sinker.app.repository.ProductionFormRepository;
import com.sinker.app.repository.SalesForecastFormVersionRepository;
import com.sinker.app.repository.SalesForecastRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final FormSummaryService formSummaryService;
    private final SalesForecastFormVersionRepository formVersionRepository;

    public ProductionFormService(SalesForecastRepository salesForecastRepository,
                                 InventorySalesForecastRepository inventoryForecastRepository,
                                 ProductionFormRepository productionFormRepository,
                                 FormSummaryService formSummaryService,
                                 SalesForecastFormVersionRepository formVersionRepository) {
        this.salesForecastRepository = salesForecastRepository;
        this.inventoryForecastRepository = inventoryForecastRepository;
        this.productionFormRepository = productionFormRepository;
        this.formSummaryService = formSummaryService;
        this.formVersionRepository = formVersionRepository;
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

    private static final int MAX_MONTH_RANGE = 4;

    /**
     * 列出庫存銷量預估量整合表單在 [startMonth, endMonth] 內出現過的版本（最多查詢 4 個月）。
     */
    @Transactional(readOnly = true)
    public List<String> listInventoryVersionsInRange(String startMonth, String endMonth) {
        validateMonthRange(startMonth, endMonth);
        return inventoryForecastRepository.findDistinctVersionsByMonthBetween(startMonth, endMonth);
    }

    /**
     * 依查詢起始/結束月份與庫存整合版本取得生產表單。
     * 產品來自庫存整合表單；12 通路每月數量與合計來自銷售預估量表單最後版本；合計為各通路每月加總，total 為合計欄位加總；原始預估/差異/備註取自銷售預估量表單最後版本。
     */
    @Transactional(readOnly = true)
    public ProductionFormRangeResponse getProductionFormByMonthRange(String startMonth, String endMonth, String inventoryVersion) {
        validateMonthRange(startMonth, endMonth);
        if (inventoryVersion == null || inventoryVersion.isEmpty()) {
            throw new IllegalArgumentException("version is required");
        }
        List<String> monthKeys = listMonthsInRange(startMonth, endMonth);
        List<String> versions = inventoryForecastRepository.findDistinctVersionsByMonthBetween(startMonth, endMonth);
        if (!versions.contains(inventoryVersion)) {
            throw new IllegalArgumentException("Selected version not found in range");
        }

        Map<ProductKey, ProductInfo> productMap = new LinkedHashMap<>();
        for (String month : monthKeys) {
            List<InventorySalesForecast> inv = inventoryForecastRepository.findByMonthAndVersionOrderByProductCodeAsc(month, inventoryVersion);
            for (InventorySalesForecast i : inv) {
                ProductKey key = new ProductKey(i.getProductCode(), i.getWarehouseLocation(), i.getCategory(), i.getSpec(), i.getProductName());
                productMap.putIfAbsent(key, new ProductInfo(i.getWarehouseLocation(), i.getCategory(), i.getSpec(), i.getProductName(), i.getProductCode()));
            }
        }

        // productKey -> month -> list of 12 channel quantities (currentQty from form summary)
        Map<ProductKey, Map<String, List<BigDecimal>>> channelDataByProductMonth = new LinkedHashMap<>();
        Map<ProductKey, BigDecimal> originalForecastSum = new LinkedHashMap<>();
        Map<ProductKey, BigDecimal> differenceSum = new LinkedHashMap<>();
        Map<ProductKey, String> remarksFirst = new LinkedHashMap<>();
        for (String month : monthKeys) {
            Integer lastFormVersion = formVersionRepository.findByMonthOrderByVersionNoDesc(month).stream()
                    .findFirst()
                    .map(v -> v.getVersionNo())
                    .orElse(null);
            FormSummaryResponse fs = formSummaryService.getFormSummary(month, lastFormVersion);
            if (fs == null || fs.getRows() == null) continue;
            for (FormSummaryRowDTO r : fs.getRows()) {
                String pk = r.getProductCode();
                if (pk == null) continue;
                ProductKey key = new ProductKey(pk, r.getWarehouseLocation(), r.getCategory(), r.getSpec(), r.getProductName());
                List<BigDecimal> channelQtys = new ArrayList<>();
                BigDecimal prevSum = BigDecimal.ZERO;
                BigDecimal currSum = BigDecimal.ZERO;
                if (r.getChannelCells() != null) {
                    for (var c : r.getChannelCells()) {
                        BigDecimal prev = c.getPreviousQty() != null ? c.getPreviousQty() : BigDecimal.ZERO;
                        BigDecimal curr = c.getCurrentQty() != null ? c.getCurrentQty() : BigDecimal.ZERO;
                        channelQtys.add(curr);
                        prevSum = prevSum.add(prev);
                        currSum = currSum.add(curr);
                    }
                }
                while (channelQtys.size() < CHANNEL_ORDER.size()) {
                    channelQtys.add(BigDecimal.ZERO);
                }
                channelDataByProductMonth.computeIfAbsent(key, k -> new LinkedHashMap<>()).put(month, channelQtys);
                BigDecimal diff = prevSum.subtract(currSum).setScale(2, RoundingMode.HALF_UP);
                originalForecastSum.merge(key, prevSum, BigDecimal::add);
                differenceSum.merge(key, diff, BigDecimal::add);
                if (!remarksFirst.containsKey(key))
                    remarksFirst.put(key, fs.getVersionRemark() != null ? fs.getVersionRemark() : "");
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
            row.setChannelData(new ArrayList<>());
            Map<String, List<BigDecimal>> productChannelMonths = channelDataByProductMonth.getOrDefault(key, new LinkedHashMap<>());

            for (int chIdx = 0; chIdx < CHANNEL_ORDER.size(); chIdx++) {
                ProductionFormChannelDTO cd = new ProductionFormChannelDTO();
                cd.setChannel(CHANNEL_ORDER.get(chIdx));
                BigDecimal channelTotal = BigDecimal.ZERO;
                for (String m : monthKeys) {
                    List<BigDecimal> list = productChannelMonths.get(m);
                    BigDecimal v = (list != null && chIdx < list.size()) ? list.get(chIdx) : BigDecimal.ZERO;
                    cd.getMonths().put(m, v);
                    channelTotal = channelTotal.add(v);
                }
                cd.setTotal(channelTotal);
                row.getChannelData().add(cd);
            }

            Map<String, BigDecimal> agg = new LinkedHashMap<>();
            BigDecimal aggregateTotal = BigDecimal.ZERO;
            for (String m : monthKeys) {
                BigDecimal sum = BigDecimal.ZERO;
                for (ProductionFormChannelDTO cd : row.getChannelData()) {
                    sum = sum.add(cd.getMonths().getOrDefault(m, BigDecimal.ZERO));
                }
                agg.put(m, sum);
                aggregateTotal = aggregateTotal.add(sum);
            }
            row.setAggregateMonths(agg);
            row.setBufferQuantity(BigDecimal.ZERO);
            row.setAggregateTotal(aggregateTotal);
            row.setOriginalForecast(originalForecastSum.getOrDefault(key, BigDecimal.ZERO));
            row.setDifference(differenceSum.getOrDefault(key, BigDecimal.ZERO));
            row.setRemarks(remarksFirst.get(key));
            rows.add(row);
        }
        rows.sort(Comparator.comparing(ProductionFormRowDTO::getCategory)
                .thenComparing(ProductionFormRowDTO::getSpec)
                .thenComparing(ProductionFormRowDTO::getProductCode));

        ProductionFormRangeResponse resp = new ProductionFormRangeResponse();
        resp.setMonthKeys(monthKeys);
        resp.setChannelOrder(new ArrayList<>(CHANNEL_ORDER));
        resp.setVersions(versions);
        resp.setRows(rows);
        return resp;
    }

    private static void validateMonthRange(String startMonth, String endMonth) {
        if (startMonth == null || startMonth.length() != 6 || !startMonth.matches("\\d{6}")) {
            throw new IllegalArgumentException("start_month must be YYYYMM");
        }
        if (endMonth == null || endMonth.length() != 6 || !endMonth.matches("\\d{6}")) {
            throw new IllegalArgumentException("end_month must be YYYYMM");
        }
        if (startMonth.compareTo(endMonth) > 0) {
            throw new IllegalArgumentException("start_month must not be after end_month");
        }
        int count = 0;
        String m = startMonth;
        while (m.compareTo(endMonth) <= 0) {
            count++;
            if (count > MAX_MONTH_RANGE) {
                throw new IllegalArgumentException("查詢區間最多 4 個月");
            }
            int y = Integer.parseInt(m.substring(0, 4));
            int mon = Integer.parseInt(m.substring(4, 6));
            mon++;
            if (mon > 12) { mon = 1; y++; }
            m = String.format("%d%02d", y, mon);
        }
    }

    private static List<String> listMonthsInRange(String startMonth, String endMonth) {
        List<String> out = new ArrayList<>();
        String m = startMonth;
        while (m.compareTo(endMonth) <= 0) {
            out.add(m);
            int y = Integer.parseInt(m.substring(0, 4));
            int mon = Integer.parseInt(m.substring(4, 6));
            mon++;
            if (mon > 12) { mon = 1; y++; }
            m = String.format("%d%02d", y, mon);
        }
        return out;
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
