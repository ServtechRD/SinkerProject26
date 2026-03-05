package com.sinker.app.service;

import com.sinker.app.dto.forecast.ChannelCellDTO;
import com.sinker.app.dto.forecast.ChannelVersionInfoDTO;
import com.sinker.app.dto.forecast.FormSummaryResponse;
import com.sinker.app.dto.forecast.FormSummaryRowDTO;
import com.sinker.app.entity.SalesForecast;
import com.sinker.app.entity.SalesForecastVersionReason;
import com.sinker.app.repository.SalesForecastRepository;
import com.sinker.app.repository.SalesForecastVersionReasonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class FormSummaryService {

    private static final Logger log = LoggerFactory.getLogger(FormSummaryService.class);
    private static final List<String> CHANNEL_ORDER = List.of(
            "PX/大全聯", "家樂福", "7-11", "全家", "萊爾富", "OK超商",
            "美廉社", "愛買", "大潤發", "好市多", "頂好", "楓康");

    private final SalesForecastRepository forecastRepository;
    private final SalesForecastVersionReasonRepository versionReasonRepository;

    public FormSummaryService(SalesForecastRepository forecastRepository,
                              SalesForecastVersionReasonRepository versionReasonRepository) {
        this.forecastRepository = forecastRepository;
        this.versionReasonRepository = versionReasonRepository;
    }

    @Transactional(readOnly = true)
    public FormSummaryResponse getFormSummary(String month) {
        log.info("Form summary: month={}", month);
        if (month == null || month.length() != 6 || !month.matches("\\d{6}")) {
            throw new IllegalArgumentException("Invalid month format, expected YYYYMM");
        }

        List<ChannelVersionInfoDTO> channelVersions = new ArrayList<>();
        Map<String, Map<String, BigDecimal>> channelLatestQty = new LinkedHashMap<>();
        Map<String, Map<String, BigDecimal>> channelPreviousQty = new LinkedHashMap<>();
        Map<String, String> channelRemark = new LinkedHashMap<>();
        Map<String, FormSummaryRowDTO> keyToRow = new TreeMap<>();

        for (String channel : CHANNEL_ORDER) {
            List<String> versions = forecastRepository.findDistinctVersionsByMonthAndChannel(month, channel);
            String latestVer = versions.isEmpty() ? null : versions.get(0);
            String previousVer = versions.size() > 1 ? versions.get(1) : latestVer;

            channelVersions.add(new ChannelVersionInfoDTO(channel, latestVer != null ? latestVer : ""));

            Map<String, BigDecimal> latestQty = new LinkedHashMap<>();
            Map<String, BigDecimal> previousQty = new LinkedHashMap<>();
            String remark = null;

            if (latestVer != null) {
                List<SalesForecast> latestRows = forecastRepository
                        .findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(month, channel, latestVer);
                for (SalesForecast r : latestRows) {
                    String pk = productKey(r);
                    latestQty.put(pk, r.getQuantity());
                    keyToRow.putIfAbsent(pk, rowFrom(r));
                }
                remark = versionReasonRepository.findByMonthAndChannelAndVersion(month, channel, latestVer)
                        .map(SalesForecastVersionReason::getChangeReason)
                        .orElse(null);
            }
            if (previousVer != null) {
                List<SalesForecast> prevRows = forecastRepository
                        .findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(month, channel, previousVer);
                for (SalesForecast r : prevRows) {
                    previousQty.put(productKey(r), r.getQuantity());
                }
            }

            channelLatestQty.put(channel, latestQty);
            channelPreviousQty.put(channel, previousQty);
            channelRemark.put(channel, remark != null ? remark : "");
        }

        for (Map.Entry<String, FormSummaryRowDTO> e : keyToRow.entrySet()) {
            String pk = e.getKey();
            FormSummaryRowDTO row = e.getValue();
            List<ChannelCellDTO> cells = new ArrayList<>();
            for (String ch : CHANNEL_ORDER) {
                ChannelCellDTO cell = new ChannelCellDTO();
                BigDecimal prev = channelPreviousQty.get(ch).getOrDefault(pk, BigDecimal.ZERO);
                BigDecimal curr = channelLatestQty.get(ch).getOrDefault(pk, BigDecimal.ZERO);
                cell.setPreviousQty(prev);
                cell.setCurrentQty(curr);
                cell.setDiff(prev.subtract(curr).setScale(2, RoundingMode.HALF_UP));
                cell.setRemark(channelRemark.getOrDefault(ch, ""));
                cells.add(cell);
            }
            row.setChannelCells(cells);
        }

        List<FormSummaryRowDTO> rows = new ArrayList<>(keyToRow.values());
        rows.sort((a, b) -> {
            int c = nullSafeCompare(a.getCategory(), b.getCategory());
            if (c != 0) return c;
            c = nullSafeCompare(a.getSpec(), b.getSpec());
            if (c != 0) return c;
            return nullSafeCompare(a.getProductCode(), b.getProductCode());
        });

        FormSummaryResponse resp = new FormSummaryResponse();
        resp.setChannelVersions(channelVersions);
        resp.setChannelOrder(new ArrayList<>(CHANNEL_ORDER));
        resp.setRows(rows);
        return resp;
    }

    private static String productKey(SalesForecast r) {
        return (r.getWarehouseLocation() != null ? r.getWarehouseLocation() : "") + "|"
                + (r.getCategory() != null ? r.getCategory() : "") + "|"
                + (r.getSpec() != null ? r.getSpec() : "") + "|"
                + (r.getProductName() != null ? r.getProductName() : "") + "|"
                + (r.getProductCode() != null ? r.getProductCode() : "");
    }

    private static FormSummaryRowDTO rowFrom(SalesForecast r) {
        FormSummaryRowDTO dto = new FormSummaryRowDTO();
        dto.setWarehouseLocation(r.getWarehouseLocation());
        dto.setCategory(r.getCategory());
        dto.setSpec(r.getSpec());
        dto.setProductName(r.getProductName());
        dto.setProductCode(r.getProductCode());
        return dto;
    }

    private static int nullSafeCompare(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return a.compareTo(b);
    }
}
