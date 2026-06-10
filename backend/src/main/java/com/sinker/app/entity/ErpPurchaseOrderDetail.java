package com.sinker.app.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_purchase_order_detail")
public class ErpPurchaseOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(nullable = false, length = 50)
    private String factory;

    @Column(name = "po_no", nullable = false, length = 100)
    private String poNo;

    @Column(nullable = false)
    private Integer itm;

    @Column(name = "prd_no", nullable = false, length = 100)
    private String prdNo;

    @Column(name = "prd_name", nullable = false, length = 200)
    private String prdName;

    @Column(length = 50)
    private String wh;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal qty;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) { this.weekStart = weekStart; }

    public String getFactory() { return factory; }
    public void setFactory(String factory) { this.factory = factory; }

    public String getPoNo() { return poNo; }
    public void setPoNo(String poNo) { this.poNo = poNo; }

    public Integer getItm() { return itm; }
    public void setItm(Integer itm) { this.itm = itm; }

    public String getPrdNo() { return prdNo; }
    public void setPrdNo(String prdNo) { this.prdNo = prdNo; }

    public String getPrdName() { return prdName; }
    public void setPrdName(String prdName) { this.prdName = prdName; }

    public String getWh() { return wh; }
    public void setWh(String wh) { this.wh = wh; }

    public BigDecimal getQty() { return qty; }
    public void setQty(BigDecimal qty) { this.qty = qty; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
