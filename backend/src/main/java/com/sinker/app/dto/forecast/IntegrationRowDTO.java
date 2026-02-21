package com.sinker.app.dto.forecast;

import java.math.BigDecimal;

public class IntegrationRowDTO {

    private String warehouseLocation;
    private String category;
    private String spec;
    private String productName;
    private String productCode;

    // Quantities for all 12 channels
    private BigDecimal qtyPx;                // PX/大全聯
    private BigDecimal qtyCarrefour;        // 家樂福
    private BigDecimal qtyAimall;           // 愛買
    private BigDecimal qty711;              // 711
    private BigDecimal qtyFamilymart;       // 全家
    private BigDecimal qtyOk;               // OK/萊爾富
    private BigDecimal qtyCostco;           // 好市多
    private BigDecimal qtyFkmart;           // 楓康
    private BigDecimal qtyWellsociety;      // 美聯社
    private BigDecimal qtyCosmed;           // 康是美
    private BigDecimal qtyEcommerce;        // 電商
    private BigDecimal qtyDistributor;      // 市面經銷

    private BigDecimal originalSubtotal;
    private BigDecimal difference;
    private String remarks;

    public IntegrationRowDTO() {
        // Initialize all quantities to 0
        this.qtyPx = BigDecimal.ZERO;
        this.qtyCarrefour = BigDecimal.ZERO;
        this.qtyAimall = BigDecimal.ZERO;
        this.qty711 = BigDecimal.ZERO;
        this.qtyFamilymart = BigDecimal.ZERO;
        this.qtyOk = BigDecimal.ZERO;
        this.qtyCostco = BigDecimal.ZERO;
        this.qtyFkmart = BigDecimal.ZERO;
        this.qtyWellsociety = BigDecimal.ZERO;
        this.qtyCosmed = BigDecimal.ZERO;
        this.qtyEcommerce = BigDecimal.ZERO;
        this.qtyDistributor = BigDecimal.ZERO;
        this.originalSubtotal = BigDecimal.ZERO;
        this.difference = BigDecimal.ZERO;
    }

    public String getWarehouseLocation() { return warehouseLocation; }
    public void setWarehouseLocation(String warehouseLocation) { this.warehouseLocation = warehouseLocation; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public BigDecimal getQtyPx() { return qtyPx; }
    public void setQtyPx(BigDecimal qtyPx) { this.qtyPx = qtyPx; }

    public BigDecimal getQtyCarrefour() { return qtyCarrefour; }
    public void setQtyCarrefour(BigDecimal qtyCarrefour) { this.qtyCarrefour = qtyCarrefour; }

    public BigDecimal getQtyAimall() { return qtyAimall; }
    public void setQtyAimall(BigDecimal qtyAimall) { this.qtyAimall = qtyAimall; }

    public BigDecimal getQty711() { return qty711; }
    public void setQty711(BigDecimal qty711) { this.qty711 = qty711; }

    public BigDecimal getQtyFamilymart() { return qtyFamilymart; }
    public void setQtyFamilymart(BigDecimal qtyFamilymart) { this.qtyFamilymart = qtyFamilymart; }

    public BigDecimal getQtyOk() { return qtyOk; }
    public void setQtyOk(BigDecimal qtyOk) { this.qtyOk = qtyOk; }

    public BigDecimal getQtyCostco() { return qtyCostco; }
    public void setQtyCostco(BigDecimal qtyCostco) { this.qtyCostco = qtyCostco; }

    public BigDecimal getQtyFkmart() { return qtyFkmart; }
    public void setQtyFkmart(BigDecimal qtyFkmart) { this.qtyFkmart = qtyFkmart; }

    public BigDecimal getQtyWellsociety() { return qtyWellsociety; }
    public void setQtyWellsociety(BigDecimal qtyWellsociety) { this.qtyWellsociety = qtyWellsociety; }

    public BigDecimal getQtyCosmed() { return qtyCosmed; }
    public void setQtyCosmed(BigDecimal qtyCosmed) { this.qtyCosmed = qtyCosmed; }

    public BigDecimal getQtyEcommerce() { return qtyEcommerce; }
    public void setQtyEcommerce(BigDecimal qtyEcommerce) { this.qtyEcommerce = qtyEcommerce; }

    public BigDecimal getQtyDistributor() { return qtyDistributor; }
    public void setQtyDistributor(BigDecimal qtyDistributor) { this.qtyDistributor = qtyDistributor; }

    public BigDecimal getOriginalSubtotal() { return originalSubtotal; }
    public void setOriginalSubtotal(BigDecimal originalSubtotal) { this.originalSubtotal = originalSubtotal; }

    public BigDecimal getDifference() { return difference; }
    public void setDifference(BigDecimal difference) { this.difference = difference; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
