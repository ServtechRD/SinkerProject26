package com.sinker.app.service;

import com.sinker.app.dto.pdca.PdcaRequest;
import com.sinker.app.dto.pdca.PdcaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Stub implementation of PDCA API client.
 * Returns realistic mock data for testing until real PDCA API integration is available.
 */
@Service
public class PdcaApiClientStub implements PdcaApiClient {

    private static final Logger log = LoggerFactory.getLogger(PdcaApiClientStub.class);

    private static final String[][] MOCK_MATERIALS = {
        {"AA08C", "關華豆膠(LF20)/25kg/包", "KG"},
        {"BA12D", "玉米澱粉/20kg/包", "KG"},
        {"CC05A", "乳化劑E471/10kg/箱", "KG"},
        {"DD15B", "葡萄糖漿/25kg/桶", "KG"},
        {"EE08C", "食用色素黃5號/1kg/瓶", "KG"}
    };

    @Override
    public PdcaResponse calculateMaterialRequirements(PdcaRequest request) {
        log.info("PDCA stub: calculating material requirements for {} schedule items",
                request.getSchedule().size());

        List<PdcaResponse.MaterialItem> materials = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // For each schedule item, generate 3 material requirements
        for (PdcaRequest.ScheduleItem scheduleItem : request.getSchedule()) {
            LocalDate demandDate = LocalDate.parse(scheduleItem.getDemandDate());
            LocalDate materialDemandDate = demandDate.minusDays(7); // Materials needed 1 week before production

            for (int i = 0; i < 3; i++) {
                String[] materialData = MOCK_MATERIALS[i % MOCK_MATERIALS.length];

                // Calculate mock quantities based on production quantity
                double baseQuantity = scheduleItem.getQuantity() * 0.15; // 15% of production quantity
                double demandQty = baseQuantity * (1 + i * 0.2); // Vary by material
                double expectedDelivery = demandQty * 0.4; // 40% incoming
                double estimatedInventory = demandQty * 0.2; // 20% in stock

                PdcaResponse.MaterialItem material = new PdcaResponse.MaterialItem(
                    materialData[0], // material_code
                    materialData[1], // material_name
                    materialData[2], // unit
                    materialDemandDate.format(formatter), // demand_date
                    Math.round(expectedDelivery * 100.0) / 100.0, // expected_delivery
                    Math.round(demandQty * 100.0) / 100.0, // demand_quantity
                    Math.round(estimatedInventory * 100.0) / 100.0 // estimated_inventory
                );

                materials.add(material);
            }
        }

        log.info("PDCA stub: generated {} material requirements", materials.size());
        return new PdcaResponse(materials);
    }
}
