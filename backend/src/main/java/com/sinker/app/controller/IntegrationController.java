package com.sinker.app.controller;

import com.sinker.app.service.ErpPurchaseOrderService;
import com.sinker.app.service.PdcaRecomputeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * 後端對外整合：PDCA 重算、ERP 採購單建立（實際呼叫外部 URL 由設定檔注入）。
 */
@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {

    private static final Logger log = LoggerFactory.getLogger(IntegrationController.class);

    private final PdcaRecomputeService pdcaRecomputeService;
    private final ErpPurchaseOrderService erpPurchaseOrderService;

    public IntegrationController(PdcaRecomputeService pdcaRecomputeService,
                               ErpPurchaseOrderService erpPurchaseOrderService) {
        this.pdcaRecomputeService = pdcaRecomputeService;
        this.erpPurchaseOrderService = erpPurchaseOrderService;
    }

    @PostMapping("/pdca/recompute")
    @PreAuthorize("hasAnyAuthority('weekly_schedule.upload','material_demand.upload')")
    public ResponseEntity<Map<String, Object>> pdcaRecompute(
            @RequestParam("week_start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam String factory) {
        log.info("POST /api/integrations/pdca/recompute weekStart={}, factory={}", weekStart, factory);
        pdcaRecomputeService.recomputeSync(weekStart, factory);
        return ResponseEntity.ok(Map.of("message", "PDCA recompute triggered"));
    }

    @PostMapping("/erp/purchase-order")
    @PreAuthorize("hasAuthority('confirm_data_send_erp')")
    public ResponseEntity<Map<String, Object>> erpPurchaseOrder(
            @RequestParam("week_start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam String factory) {
        log.info("POST /api/integrations/erp/purchase-order weekStart={}, factory={}", weekStart, factory);
        erpPurchaseOrderService.createPurchaseOrder(weekStart, factory);
        return ResponseEntity.ok(Map.of("message", "ERP purchase order request sent"));
    }
}
