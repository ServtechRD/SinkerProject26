package com.sinker.app.service;

import com.sinker.app.dto.erp.ErpCreatePurchaseOrderRequest;
import com.sinker.app.dto.erp.ErpCreatePurchaseOrderRequest.Detail;
import com.sinker.app.dto.erp.ErpOrderRequest;
import com.sinker.app.dto.erp.ErpOrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ErpPurchaseService {

    private static final Logger log = LoggerFactory.getLogger(ErpPurchaseService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AtomicInteger dailySequence = new AtomicInteger(1);
    private volatile String lastDate = "";

    public ErpOrderResponse createOrder(ErpOrderRequest request) {
        String webNo = generateWebNo();
        ErpCreatePurchaseOrderRequest body = buildRequest(webNo, request);

        log.info("ERP createOrder: WebNo={}, PrdNo={}, Wh={}, Qty={}",
                webNo, request.getPrdNo(), request.getWh(), request.getQty());

        // TODO: 替換為實際 HTTP 呼叫（參考 ErpPurchaseOrderService）
        return new ErpOrderResponse(webNo, "SUCCESS");
    }

    private synchronized String generateWebNo() {
        String today = LocalDate.now().format(DATE_FMT);
        if (!today.equals(lastDate)) {
            lastDate = today;
            dailySequence.set(1);
        }
        return String.format("PO%s%04d", today, dailySequence.getAndIncrement());
    }

    private ErpCreatePurchaseOrderRequest buildRequest(String webNo, ErpOrderRequest request) {
        Detail detail = new Detail(
                request.getItm(),
                request.getPrdNo(),
                request.getPrdName(),
                request.getWh(),
                "",
                request.getQty(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                ""
        );

        return new ErpCreatePurchaseOrderRequest(
                webNo,
                "0000",
                "B105-2",
                LocalDate.now().toString(),
                request.getDemandDate().toString(),
                "",
                1,
                List.of(detail)
        );
    }
}
