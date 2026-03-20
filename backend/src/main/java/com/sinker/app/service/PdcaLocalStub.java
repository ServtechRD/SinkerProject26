package com.sinker.app.service;

import com.sinker.app.dto.pdca.PdcaRequest;
import com.sinker.app.dto.pdca.PdcaResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 本機假資料：當 PDCA HTTP 未啟用時供 {@link PdcaApiClientImpl} 使用（測試／開發）。
 */
public final class PdcaLocalStub {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String[][] MOCK_MATERIALS = {
            {"AA08C", "關華豆膠(LF20)/25kg/包", "KG"},
            {"BA12D", "玉米澱粉/20kg/包", "KG"},
            {"CC05A", "乳化劑E471/10kg/箱", "KG"},
            {"DD15B", "葡萄糖漿/25kg/桶", "KG"},
            {"EE08C", "食用色素黃5號/1kg/瓶", "KG"}
    };

    private PdcaLocalStub() {
    }

    /**
     * 當 PDCA URL 未設定時使用。若週排程為空，改回傳固定示範資料（仍依 weekStart 產生需求日）。
     */
    public static PdcaResponse generate(PdcaRequest request, LocalDate weekStart, String factory) {
        if (request.getSchedule() == null || request.getSchedule().isEmpty()) {
            return generateFixedDemo(weekStart, factory);
        }
        return generateFromSchedule(request);
    }

    private static PdcaResponse generateFromSchedule(PdcaRequest request) {
        List<PdcaResponse.MaterialItem> materials = new ArrayList<>();

        for (PdcaRequest.ScheduleItem scheduleItem : request.getSchedule()) {
            LocalDate demandDate = LocalDate.parse(scheduleItem.getDemandDate());
            LocalDate materialDemandDate = demandDate.minusDays(7);

            for (int i = 0; i < 3; i++) {
                String[] materialData = MOCK_MATERIALS[i % MOCK_MATERIALS.length];
                double baseQuantity = scheduleItem.getQuantity() * 0.15;
                double demandQty = baseQuantity * (1 + i * 0.2);
                double expectedDelivery = demandQty * 0.4;
                double estimatedInventory = demandQty * 0.2;

                materials.add(new PdcaResponse.MaterialItem(
                        materialData[0],
                        materialData[1],
                        materialData[2],
                        materialDemandDate.format(FORMATTER),
                        Math.round(expectedDelivery * 100.0) / 100.0,
                        Math.round(demandQty * 100.0) / 100.0,
                        Math.round(estimatedInventory * 100.0) / 100.0
                ));
            }
        }

        return new PdcaResponse(materials);
    }

    /** 無週排程或開發環境時的固定示範列 */
    private static PdcaResponse generateFixedDemo(LocalDate weekStart, String factory) {
        List<PdcaResponse.MaterialItem> materials = new ArrayList<>();
        LocalDate base = weekStart.plusDays(3);
        for (int i = 0; i < MOCK_MATERIALS.length; i++) {
            String[] materialData = MOCK_MATERIALS[i];
            double demandQty = 100 + i * 25.5;
            double expectedDelivery = demandQty * 0.4;
            double estimatedInventory = demandQty * 0.2;
            materials.add(new PdcaResponse.MaterialItem(
                    materialData[0],
                    materialData[1],
                    materialData[2],
                    base.plusDays(i).format(FORMATTER),
                    Math.round(expectedDelivery * 100.0) / 100.0,
                    Math.round(demandQty * 100.0) / 100.0,
                    Math.round(estimatedInventory * 100.0) / 100.0
            ));
        }
        return new PdcaResponse(materials);
    }
}
