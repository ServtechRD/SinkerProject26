package com.sinker.app.service;

import com.sinker.app.dto.erp.ErpProductItem;
import com.sinker.app.dto.erp.ErpProductListRequest;
import com.sinker.app.dto.erp.ErpProductPageResponse;
import com.sinker.app.dto.erp.ErpProductSyncParam;
import com.sinker.app.entity.Product;
import com.sinker.app.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ErpProductSyncService {

    private static final Logger log = LoggerFactory.getLogger(ErpProductSyncService.class);
    private static final int DEFAULT_PAGE_SIZE = 1000;

    private final ErpProductSyncClient client;
    private final ProductRepository productRepository;

    public ErpProductSyncService(ErpProductSyncClient client, ProductRepository productRepository) {
        this.client = client;
        this.productRepository = productRepository;
    }

    public Map<String, Object> syncProducts(ErpProductSyncParam param) {
        if (!client.isConfigured()) {
            throw new IllegalStateException("ERP product sync not configured");
        }

        boolean isOnlyUpdate = Boolean.TRUE.equals(param.getIsOnlyUpdate());
        String prdNo = param.getPrdNo() != null ? param.getPrdNo() : "";
        String name = param.getName() != null ? param.getName() : "";
        String idx1 = param.getIdx1() != null ? param.getIdx1() : "";
        String knd = param.getKnd() != null ? param.getKnd() : "";
        int pageSize = (param.getPageSize() != null && param.getPageSize() > 0)
                ? param.getPageSize() : DEFAULT_PAGE_SIZE;

        log.info("ERP product sync started: isOnlyUpdate={}, prdNo='{}', name='{}', idx1='{}', knd='{}', pageSize={}",
                isOnlyUpdate, prdNo, name, idx1, knd, pageSize);

        long totalStart = System.currentTimeMillis();
        int nowPage = 1;
        int totalPage = 1;
        int totalFetched = 0;
        int totalSaved = 0;

        while (true) {
            long pageStart = System.currentTimeMillis();

            ErpProductListRequest request = buildRequest(isOnlyUpdate, prdNo, name, idx1, knd, nowPage, pageSize);
            ErpProductPageResponse response = client.fetchPage(request);

            if (response == null || response.getDatas() == null || response.getDatas().isEmpty()) {
                log.info("ERP product sync page {}: empty response, stopping", nowPage);
                break;
            }

            totalPage = response.getTotalPage();
            List<ErpProductItem> items = response.getDatas();
            totalFetched += items.size();

            int saved = upsertPage(items, isOnlyUpdate);
            totalSaved += saved;

            long pageMs = System.currentTimeMillis() - pageStart;
            log.info("ERP product sync page {}/{}: fetched={}, saved={}, elapsed={}ms",
                    nowPage, totalPage, items.size(), saved, pageMs);

            if (nowPage >= totalPage) {
                break;
            }
            nowPage++;
        }

        long totalMs = System.currentTimeMillis() - totalStart;
        log.info("ERP product sync completed: totalFetched={}, totalSaved={}, pages={}, totalElapsed={}ms",
                totalFetched, totalSaved, nowPage, totalMs);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalFetched", totalFetched);
        result.put("totalSaved", totalSaved);
        result.put("totalPages", nowPage);
        result.put("elapsedMs", totalMs);
        return result;
    }

    private int upsertPage(List<ErpProductItem> items, boolean isOnlyUpdate) {
        List<String> codes = items.stream()
                .map(ErpProductItem::getPrdNo)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());

        Map<String, Product> existingByCode = productRepository.findByCodeIn(codes).stream()
                .collect(Collectors.toMap(Product::getCode, p -> p));

        LocalDateTime now = LocalDateTime.now();
        // 以 productCode 為 key，避免同頁重複 PrdNo 造成 uk_product_code 衝突
        Map<String, Product> toSave = new LinkedHashMap<>();

        for (ErpProductItem item : items) {
            if (!StringUtils.hasText(item.getPrdNo())) {
                continue;
            }
            Product product = existingByCode.get(item.getPrdNo());
            if (product != null) {
                product.setName(item.getName() != null ? item.getName() : product.getName());
                product.setCategoryName(item.getIdxName());
                product.setSpec(item.getSpc());
                product.setWarehouseLocation(item.getWh());
                product.setUpdatedAt(now);
                toSave.put(item.getPrdNo(), product);
            } else if (!isOnlyUpdate && !toSave.containsKey(item.getPrdNo())) {
                Product newProduct = new Product();
                newProduct.setCode(item.getPrdNo());
                newProduct.setName(item.getName() != null ? item.getName() : "");
                newProduct.setCategoryName(item.getIdxName());
                newProduct.setSpec(item.getSpc());
                newProduct.setWarehouseLocation(item.getWh());
                newProduct.setCreatedAt(now);
                newProduct.setUpdatedAt(now);
                toSave.put(item.getPrdNo(), newProduct);
            }
        }

        productRepository.saveAll(toSave.values());
        return toSave.size();
    }

    private ErpProductListRequest buildRequest(boolean isOnlyUpdate, String prdNo, String name,
                                               String idx1, String knd, int nowPage, int pageSize) {
        ErpProductListRequest req = new ErpProductListRequest();
        req.setIsOnlyUpdate(isOnlyUpdate);
        req.setPrdNo(prdNo);
        req.setName(name);
        req.setIdx1(idx1);
        req.setKnd(knd);
        req.setNowPage(nowPage);
        req.setPageSize(pageSize);
        return req;
    }
}
