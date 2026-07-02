package com.sinker.app.service;

import com.sinker.app.dto.erp.VendorListRequest;
import com.sinker.app.dto.erp.VendorSyncItem;
import com.sinker.app.dto.erp.VendorSyncParam;
import com.sinker.app.entity.Vendor;
import com.sinker.app.repository.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class ErpVendorSyncService {

    private static final Logger log = LoggerFactory.getLogger(ErpVendorSyncService.class);
    private static final int DEFAULT_PAGE_SIZE = 1000;
    private static final DateTimeFormatter ERP_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ErpVendorSyncClient client;
    private final VendorRepository vendorRepository;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile LocalDateTime lastStartedAt = null;
    private volatile LocalDateTime lastFinishedAt = null;
    private volatile Map<String, Object> lastResult = null;
    private volatile String lastError = null;

    public ErpVendorSyncService(ErpVendorSyncClient client, VendorRepository vendorRepository) {
        this.client = client;
        this.vendorRepository = vendorRepository;
    }

    public Map<String, Object> getSyncStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("running", running.get());
        status.put("lastStartedAt", lastStartedAt);
        status.put("lastFinishedAt", lastFinishedAt);
        status.put("lastResult", lastResult);
        status.put("lastError", lastError);
        return status;
    }

    public boolean isRunning() {
        return running.get();
    }

    @Async
    public void syncVendorsAsync(VendorSyncParam param) {
        if (!running.compareAndSet(false, true)) {
            log.warn("ERP vendor sync already running, skipping duplicate trigger");
            return;
        }
        lastStartedAt = LocalDateTime.now();
        lastError = null;
        try {
            lastResult = doSync(param);
        } catch (Exception e) {
            lastError = e.getMessage();
            log.error("ERP vendor sync failed", e);
        } finally {
            lastFinishedAt = LocalDateTime.now();
            running.set(false);
        }
    }

    private Map<String, Object> doSync(VendorSyncParam param) {
        if (!client.isConfigured()) {
            throw new IllegalStateException("ERP vendor sync not configured");
        }

        boolean isOnlyUpdate = Boolean.TRUE.equals(param.getIsOnlyUpdate());
        int pageSize = (param.getPageSize() != null && param.getPageSize() > 0)
                ? param.getPageSize() : DEFAULT_PAGE_SIZE;

        log.info("ERP vendor sync started: isOnlyUpdate={}, pageSize={}", isOnlyUpdate, pageSize);

        long totalStart = System.currentTimeMillis();

        // 廠商 API 未實作分頁，NowPage/PageSize 會被忽略、每次都回傳全量資料，故僅呼叫一次
        VendorListRequest request = new VendorListRequest();
        request.setIsOnlyUpdate(isOnlyUpdate);
        request.setNowPage(1);
        request.setPageSize(pageSize);

        List<VendorSyncItem> items = client.fetchPage(request);
        int totalFetched = (items != null) ? items.size() : 0;
        int totalSaved = (items != null && !items.isEmpty()) ? upsertPage(items, isOnlyUpdate) : 0;

        long totalMs = System.currentTimeMillis() - totalStart;
        log.info("ERP vendor sync completed: totalFetched={}, totalSaved={}, totalElapsed={}ms",
                totalFetched, totalSaved, totalMs);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalFetched", totalFetched);
        result.put("totalSaved", totalSaved);
        result.put("elapsedMs", totalMs);
        return result;
    }

    private int upsertPage(List<VendorSyncItem> items, boolean isOnlyUpdate) {
        List<String> codes = items.stream()
                .map(VendorSyncItem::getCusNo)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());

        // uk_vendor_code 為 utf8mb4_unicode_ci（大小寫不分），統一以大寫作為 map key 避免誤判為新廠商而重複 INSERT
        Map<String, Vendor> existingByCode = vendorRepository.findByCodeIn(codes).stream()
                .collect(Collectors.toMap(v -> v.getCode().toUpperCase(), v -> v));

        LocalDateTime now = LocalDateTime.now();
        Map<String, Vendor> toSave = new LinkedHashMap<>();

        for (VendorSyncItem item : items) {
            if (!StringUtils.hasText(item.getCusNo())) {
                continue;
            }
            String key = item.getCusNo().toUpperCase();
            Vendor vendor = existingByCode.get(key);
            if (vendor != null) {
                vendor.setName(item.getName() != null ? item.getName() : vendor.getName());
                vendor.setSysDate(parseErpDateTime(item.getSysDate()));
                vendor.setModifyDate(parseErpDateTime(item.getModifyDate()));
                vendor.setUpdatedAt(now);
                toSave.put(key, vendor);
            } else if (!isOnlyUpdate && !toSave.containsKey(key)) {
                Vendor newVendor = new Vendor();
                newVendor.setCode(item.getCusNo());
                newVendor.setName(item.getName() != null ? item.getName() : "");
                newVendor.setSysDate(parseErpDateTime(item.getSysDate()));
                newVendor.setModifyDate(parseErpDateTime(item.getModifyDate()));
                newVendor.setCreatedAt(now);
                newVendor.setUpdatedAt(now);
                toSave.put(key, newVendor);
            }
        }

        // saveAll(List) 而非直接傳 Map.values()：Map.values() 回傳的 Collection 視圖非 List 型別
        vendorRepository.saveAll(new ArrayList<>(toSave.values()));
        return toSave.size();
    }

    private LocalDateTime parseErpDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, ERP_DATETIME_FORMAT);
        } catch (Exception e) {
            log.warn("Unparsable ERP datetime '{}', storing as null", value);
            return null;
        }
    }
}
