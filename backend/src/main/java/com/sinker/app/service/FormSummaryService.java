package com.sinker.app.service;

import com.sinker.app.dto.forecast.*;
import com.sinker.app.entity.GiftSalesForecast;
import com.sinker.app.entity.SalesForecast;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.entity.SalesForecastFormVersion;
import com.sinker.app.entity.SalesForecastVersionReason;
import com.sinker.app.repository.GiftSalesForecastRepository;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastFormVersionRepository;
import com.sinker.app.repository.SalesForecastRepository;
import com.sinker.app.repository.SalesForecastVersionReasonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
public class FormSummaryService {

    private static final Logger log = LoggerFactory.getLogger(FormSummaryService.class);
    private static final List<String> CHANNEL_ORDER = List.of(
            "PX + 大全聯", "家樂福", "愛買", "7-11", "全家", "Ok+萊爾富",
            "好市多", "楓康", "美聯社", "康是美", "電商", "市面經銷");

    private final SalesForecastRepository forecastRepository;
    private final SalesForecastVersionReasonRepository versionReasonRepository;
    private final SalesForecastConfigRepository configRepository;
    private final SalesForecastFormVersionRepository formVersionRepository;
    private final GiftSalesForecastRepository giftForecastRepository;

    public FormSummaryService(SalesForecastRepository forecastRepository,
                              SalesForecastVersionReasonRepository versionReasonRepository,
                              SalesForecastConfigRepository configRepository,
                              SalesForecastFormVersionRepository formVersionRepository,
                              GiftSalesForecastRepository giftForecastRepository) {
        this.forecastRepository = forecastRepository;
        this.versionReasonRepository = versionReasonRepository;
        this.configRepository = configRepository;
        this.formVersionRepository = formVersionRepository;
        this.giftForecastRepository = giftForecastRepository;
    }

    /**
     * 若傳入 versionNo 則必須為已關帳月份，並回傳該表單版本的摘要（原小計=前一版總和；第 1 版無上一版時為第一版快照加總，更改後小計=此版總和，備註=此版修改原因）。
     * 若 versionNo 為 null 則維持原邏輯（各通路最新版彙總）。
     */
    @Transactional(readOnly = true)
    public FormSummaryResponse getFormSummary(String month, Integer versionNo) {
        if (versionNo != null) {
            return getFormSummaryByVersion(month, versionNo);
        }
        return getFormSummaryLegacy(month);
    }

    @Transactional
    public List<FormVersionListItemDTO> listFormVersions(String month) {
        if (month == null || month.length() != 6 || !month.matches("\\d{6}")) {
            throw new IllegalArgumentException("Invalid month format, expected YYYYMM");
        }
        Optional<SalesForecastConfig> configOpt = configRepository.findByMonth(month);
        if (configOpt.isEmpty() || !Boolean.TRUE.equals(configOpt.get().getIsClosed())) {
            return List.of();
        }
        ensureFormVersion1Exists(month, configOpt.get());
        List<SalesForecastFormVersion> list = formVersionRepository.findByMonthOrderByVersionNoDesc(month);
        List<FormVersionListItemDTO> result = new ArrayList<>();
        DateTimeFormatter displayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId utc = ZoneId.of("UTC");
        ZoneId taipei = ZoneId.of("Asia/Taipei");
        for (SalesForecastFormVersion v : list) {
            FormVersionListItemDTO dto = new FormVersionListItemDTO();
            dto.setVersionNo(v.getVersionNo());
            dto.setCreatedAt(v.getCreatedAt());
            dto.setChangeReason(v.getChangeReason());
            if (v.getCreatedAt() != null) {
                ZonedDateTime atUtc = v.getCreatedAt().atZone(utc);
                String display = atUtc.withZoneSameInstant(taipei).toLocalDateTime().format(displayFmt);
                dto.setCreatedAtDisplay(display);
            }
            result.add(dto);
        }
        return result;
    }

    /** 已關帳時確保存在第一版（結束時間為第一版），並回傳該版本的摘要。 */
    @Transactional(readOnly = true)
    public FormSummaryResponse getFormSummaryByVersion(String month, int versionNo) {
        validateMonth(month);
        SalesForecastConfig config = configRepository.findByMonth(month)
                .orElseThrow(() -> new IllegalArgumentException("Month config not found: " + month));
        if (!Boolean.TRUE.equals(config.getIsClosed())) {
            throw new IllegalArgumentException("Month is not closed, cannot query by version");
        }
        ensureFormVersion1Exists(month, config);

        List<SalesForecastFormVersion> allVers = formVersionRepository.findByMonthOrderByVersionNoDesc(month);
        if (allVers.isEmpty()) {
            throw new IllegalStateException("Form version 1 should exist for closed month");
        }
        Integer prevVersionNo = versionNo > 1 ? versionNo - 1 : null;
        String versionRemark = null;
        for (SalesForecastFormVersion v : allVers) {
            if (v.getVersionNo() == versionNo) {
                versionRemark = v.getChangeReason();
                break;
            }
        }

        List<SalesForecast> currentRows = forecastRepository.findByMonthAndFormVersionNoOrderByChannelCategorySpecProductCode(month, versionNo);
        Map<String, BigDecimal>[] qtyByChannel = new Map[CHANNEL_ORDER.size()];
        for (int i = 0; i < CHANNEL_ORDER.size(); i++) {
            qtyByChannel[i] = new LinkedHashMap<>();
        }
        Map<String, FormSummaryRowDTO> keyToRow = new TreeMap<>();
        for (SalesForecast r : currentRows) {
            String pk = productKey(r);
            keyToRow.putIfAbsent(pk, rowFrom(r));
            int chIdx = CHANNEL_ORDER.indexOf(r.getChannel());
            if (chIdx >= 0) qtyByChannel[chIdx].put(pk, r.getQuantity());
        }

        Map<String, BigDecimal>[] prevQtyByChannel = null;
        if (prevVersionNo != null) {
            List<SalesForecast> prevRows = forecastRepository.findByMonthAndFormVersionNoOrderByChannelCategorySpecProductCode(month, prevVersionNo);
            prevQtyByChannel = new Map[CHANNEL_ORDER.size()];
            for (int i = 0; i < CHANNEL_ORDER.size(); i++) {
                prevQtyByChannel[i] = new LinkedHashMap<>();
            }
            for (SalesForecast r : prevRows) {
                String pk = productKey(r);
                int chIdx = CHANNEL_ORDER.indexOf(r.getChannel());
                if (chIdx >= 0) prevQtyByChannel[chIdx].put(pk, r.getQuantity());
            }
        }

        // 匯整版（form_v*）已含銷售+禮品，只讀表單版本；舊版第一版（標記上傳版）則加禮品
        boolean isAggregatedVersion = !currentRows.isEmpty()
                && currentRows.get(0).getVersion() != null
                && currentRows.get(0).getVersion().startsWith("form_v");

        Map<String, BigDecimal>[] giftQtyByChannel = null;
        if (!isAggregatedVersion) {
            giftQtyByChannel = new Map[CHANNEL_ORDER.size()];
            for (int i = 0; i < CHANNEL_ORDER.size(); i++) {
                giftQtyByChannel[i] = new LinkedHashMap<>();
            }
            for (int chIdx = 0; chIdx < CHANNEL_ORDER.size(); chIdx++) {
                String channel = CHANNEL_ORDER.get(chIdx);
                List<GiftSalesForecast> giftRows = giftForecastRepository.findLatestByMonthAndChannel(month, channel);
                for (GiftSalesForecast g : giftRows) {
                    String pk = productKeyGift(g);
                    giftQtyByChannel[chIdx].put(pk, g.getQuantity());
                    keyToRow.putIfAbsent(pk, rowFromGift(g));
                }
            }
        }

        List<FormSummaryRowDTO> rows = new ArrayList<>();
        for (Map.Entry<String, FormSummaryRowDTO> e : keyToRow.entrySet()) {
            String pk = e.getKey();
            FormSummaryRowDTO row = e.getValue();
            List<ChannelCellDTO> cells = new ArrayList<>();
            for (int i = 0; i < CHANNEL_ORDER.size(); i++) {
                BigDecimal prev = prevQtyByChannel != null ? prevQtyByChannel[i].getOrDefault(pk, BigDecimal.ZERO) : BigDecimal.ZERO;
                BigDecimal curr = qtyByChannel[i].getOrDefault(pk, BigDecimal.ZERO);
                BigDecimal salesCurr = curr;
                if (!isAggregatedVersion && giftQtyByChannel != null) {
                    BigDecimal giftCurr = giftQtyByChannel[i].getOrDefault(pk, BigDecimal.ZERO);
                    curr = curr.add(giftCurr);
                }
                // 第 1 版沒有上一版：原小計基準 = 第一版建立時快照（未修改前），與當前此版數量相同
                if (versionNo == 1) {
                    prev = curr;
                }
                ChannelCellDTO cell = new ChannelCellDTO();
                cell.setPreviousQty(prev);
                cell.setCurrentQty(curr);
                cell.setCurrentSalesQty(salesCurr);
                cell.setDiff(prev.subtract(curr).setScale(2, RoundingMode.HALF_UP));
                cell.setRemark(versionRemark != null ? versionRemark : "");
                cells.add(cell);
            }
            row.setChannelCells(cells);
            rows.add(row);
        }
        rows.sort((a, b) -> {
            int c = nullSafeCompare(a.getCategory(), b.getCategory());
            if (c != 0) return c;
            c = nullSafeCompare(a.getSpec(), b.getSpec());
            if (c != 0) return c;
            return nullSafeCompare(a.getProductCode(), b.getProductCode());
        });

        FormSummaryResponse resp = new FormSummaryResponse();
        resp.setChannelVersions(List.of());
        resp.setChannelOrder(new ArrayList<>(CHANNEL_ORDER));
        resp.setRows(rows);
        resp.setVersionNo(versionNo);
        resp.setVersionRemark(versionRemark);
        return resp;
    }

    @Transactional
    public void ensureFormVersion1Exists(String month, SalesForecastConfig config) {
        if (formVersionRepository.countByMonth(month) > 0) return;
        createFormVersion1Snapshot(month, config);
    }

    /**
     * 結束新增設定時：匯整當下各通路銷售預估＋禮品銷售預估，寫入為表單第一版（form_v1）。
     * 僅在關帳時呼叫，或 ensureFormVersion1Exists 時若尚無任何表單版本則建立。
     */
    @Transactional
    public void createFormVersion1Snapshot(String month, SalesForecastConfig config) {
        validateMonth(month);
        if (!Boolean.TRUE.equals(config.getIsClosed())) {
            throw new IllegalArgumentException("Month is not closed, cannot create form version 1 snapshot");
        }
        if (formVersionRepository.countByMonth(month) > 0) {
            log.debug("Form version already exists for month {}, skip snapshot", month);
            return;
        }
        LocalDateTime now = config.getClosedAt() != null ? config.getClosedAt() : LocalDateTime.now();
        SalesForecastFormVersion v1 = new SalesForecastFormVersion();
        v1.setMonth(month);
        v1.setVersionNo(1);
        v1.setCreatedAt(now);
        v1.setChangeReason(null);
        formVersionRepository.save(v1);

        Map<String, FormSummaryRowDTO> keyToRow = new TreeMap<>();
        Map<String, BigDecimal>[] qtyByChannel = new Map[CHANNEL_ORDER.size()];
        for (int i = 0; i < CHANNEL_ORDER.size(); i++) {
            qtyByChannel[i] = new LinkedHashMap<>();
        }
        for (int chIdx = 0; chIdx < CHANNEL_ORDER.size(); chIdx++) {
            String channel = CHANNEL_ORDER.get(chIdx);
            List<String> versions = forecastRepository.findDistinctVersionsByMonthAndChannel(month, channel);
            if (!versions.isEmpty()) {
                String latest = versions.get(0);
                List<SalesForecast> salesRows = forecastRepository.findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(month, channel, latest);
                for (SalesForecast r : salesRows) {
                    String pk = productKey(r);
                    keyToRow.putIfAbsent(pk, rowFrom(r));
                    qtyByChannel[chIdx].put(pk, r.getQuantity());
                }
            }
            List<GiftSalesForecast> giftRows = giftForecastRepository.findLatestByMonthAndChannel(month, channel);
            for (GiftSalesForecast g : giftRows) {
                String pk = productKeyGift(g);
                keyToRow.putIfAbsent(pk, rowFromGift(g));
                BigDecimal existing = qtyByChannel[chIdx].getOrDefault(pk, BigDecimal.ZERO);
                qtyByChannel[chIdx].put(pk, existing.add(g.getQuantity()));
            }
        }

        String versionLabel = "form_v1";
        int inserted = 0;
        for (int chIdx = 0; chIdx < CHANNEL_ORDER.size(); chIdx++) {
            String channel = CHANNEL_ORDER.get(chIdx);
            for (Map.Entry<String, FormSummaryRowDTO> e : keyToRow.entrySet()) {
                String pk = e.getKey();
                FormSummaryRowDTO row = e.getValue();
                BigDecimal qty = qtyByChannel[chIdx].getOrDefault(pk, BigDecimal.ZERO);
                SalesForecast sf = new SalesForecast();
                sf.setMonth(month);
                sf.setChannel(channel);
                sf.setVersion(versionLabel);
                sf.setFormVersionNo(1);
                sf.setWarehouseLocation(row.getWarehouseLocation());
                sf.setCategory(row.getCategory());
                sf.setSpec(row.getSpec());
                sf.setProductName(row.getProductName());
                sf.setProductCode(row.getProductCode() != null ? row.getProductCode() : "");
                sf.setQuantity(qty);
                sf.setRemark(row.getRemark() != null ? row.getRemark() : "");
                sf.setIsModified(false);
                sf.setCreatedAt(now);
                sf.setUpdatedAt(now);
                forecastRepository.save(sf);
                inserted++;
            }
        }
        log.info("Created form version 1 snapshot for month {}, {} rows", month, inserted);
    }

    @Transactional
    public int saveFormSummaryVersion(String month, SaveFormSummaryVersionRequest request) {
        validateMonth(month);
        SalesForecastConfig config = configRepository.findByMonth(month)
                .orElseThrow(() -> new IllegalArgumentException("Month config not found: " + month));
        if (!Boolean.TRUE.equals(config.getIsClosed())) {
            throw new IllegalArgumentException("Month is not closed");
        }
        ensureFormVersion1Exists(month, config);
        List<SalesForecastFormVersion> existing = formVersionRepository.findByMonthOrderByVersionNoDesc(month);
        int nextNo = existing.isEmpty() ? 2 : existing.get(0).getVersionNo() + 1;

        SalesForecastFormVersion newVer = new SalesForecastFormVersion();
        newVer.setMonth(month);
        newVer.setVersionNo(nextNo);
        newVer.setCreatedAt(LocalDateTime.now());
        newVer.setChangeReason(request.getChangeReason());
        formVersionRepository.save(newVer);

        String versionLabel = "form_v" + nextNo;
        LocalDateTime now = LocalDateTime.now();
        int inserted = 0;
        List<SaveFormSummaryVersionRequest.FormSummaryRowEditDTO> rows = request.getRows();
        if (rows == null) rows = List.of();
        for (int chIdx = 0; chIdx < CHANNEL_ORDER.size(); chIdx++) {
            String channel = CHANNEL_ORDER.get(chIdx);
            for (SaveFormSummaryVersionRequest.FormSummaryRowEditDTO row : rows) {
                BigDecimal qty = BigDecimal.ZERO;
                if (row.getChannelQuantities() != null && chIdx < row.getChannelQuantities().size()) {
                    BigDecimal v = row.getChannelQuantities().get(chIdx);
                    if (v != null) qty = v;
                }
                SalesForecast sf = new SalesForecast();
                sf.setMonth(month);
                sf.setChannel(channel);
                sf.setVersion(versionLabel);
                sf.setFormVersionNo(nextNo);
                sf.setWarehouseLocation(row.getWarehouseLocation());
                sf.setCategory(row.getCategory());
                sf.setSpec(row.getSpec());
                sf.setProductName(row.getProductName());
                sf.setProductCode(row.getProductCode() != null ? row.getProductCode() : "");
                sf.setQuantity(qty);
                sf.setRemark(row.getRemark() != null ? row.getRemark() : "");
                sf.setIsModified(true);
                sf.setCreatedAt(now);
                sf.setUpdatedAt(now);
                forecastRepository.save(sf);
                inserted++;
            }
        }
        log.info("Saved form summary version {} for month {}, {} rows", nextNo, month, inserted);
        return nextNo;
    }

    private static void validateMonth(String month) {
        if (month == null || month.length() != 6 || !month.matches("\\d{6}")) {
            throw new IllegalArgumentException("Invalid month format, expected YYYYMM");
        }
    }

    @Transactional(readOnly = true)
    public FormSummaryResponse getFormSummaryLegacy(String month) {
        log.info("Form summary: month={}", month);
        validateMonth(month);

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

        // 禮品銷售預估：各通路最新版數量（與銷售同通路加總呈現）
        Map<String, Map<String, BigDecimal>> giftLatestQty = new LinkedHashMap<>();
        for (String channel : CHANNEL_ORDER) {
            Map<String, BigDecimal> qtyMap = new LinkedHashMap<>();
            List<GiftSalesForecast> giftRows = giftForecastRepository.findLatestByMonthAndChannel(month, channel);
            for (GiftSalesForecast g : giftRows) {
                String pk = productKeyGift(g);
                qtyMap.put(pk, g.getQuantity());
                keyToRow.putIfAbsent(pk, rowFromGift(g));
            }
            giftLatestQty.put(channel, qtyMap);
        }

        for (Map.Entry<String, FormSummaryRowDTO> e : keyToRow.entrySet()) {
            String pk = e.getKey();
            FormSummaryRowDTO row = e.getValue();
            List<ChannelCellDTO> cells = new ArrayList<>();
            for (String ch : CHANNEL_ORDER) {
                BigDecimal prev = channelPreviousQty.get(ch).getOrDefault(pk, BigDecimal.ZERO);
                BigDecimal salesCurr = channelLatestQty.get(ch).getOrDefault(pk, BigDecimal.ZERO);
                BigDecimal giftCurr = giftLatestQty.get(ch).getOrDefault(pk, BigDecimal.ZERO);
                BigDecimal combinedCurr = salesCurr.add(giftCurr);
                ChannelCellDTO cell = new ChannelCellDTO();
                cell.setPreviousQty(prev);
                cell.setCurrentQty(combinedCurr);
                cell.setCurrentSalesQty(salesCurr);
                cell.setDiff(prev.subtract(combinedCurr).setScale(2, RoundingMode.HALF_UP));
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

    private static String productKeyGift(GiftSalesForecast g) {
        return (g.getWarehouseLocation() != null ? g.getWarehouseLocation() : "") + "|"
                + (g.getCategory() != null ? g.getCategory() : "") + "|"
                + (g.getSpec() != null ? g.getSpec() : "") + "|"
                + (g.getProductName() != null ? g.getProductName() : "") + "|"
                + (g.getProductCode() != null ? g.getProductCode() : "");
    }

    private static FormSummaryRowDTO rowFrom(SalesForecast r) {
        FormSummaryRowDTO dto = new FormSummaryRowDTO();
        dto.setWarehouseLocation(r.getWarehouseLocation());
        dto.setCategory(r.getCategory());
        dto.setSpec(r.getSpec());
        dto.setProductName(r.getProductName());
        dto.setProductCode(r.getProductCode());
        dto.setRemark(r.getRemark());
        return dto;
    }

    private static FormSummaryRowDTO rowFromGift(GiftSalesForecast g) {
        FormSummaryRowDTO dto = new FormSummaryRowDTO();
        dto.setWarehouseLocation(g.getWarehouseLocation());
        dto.setCategory(g.getCategory());
        dto.setSpec(g.getSpec());
        dto.setProductName(g.getProductName());
        dto.setProductCode(g.getProductCode());
        dto.setRemark(null);
        return dto;
    }

    private static int nullSafeCompare(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return a.compareTo(b);
    }
}
