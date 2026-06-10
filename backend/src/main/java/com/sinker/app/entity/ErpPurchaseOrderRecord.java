package com.sinker.app.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "erp_purchase_order")
@IdClass(ErpPurchaseOrderRecord.PK.class)
public class ErpPurchaseOrderRecord {

    @Id
    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Id
    @Column(nullable = false, length = 50)
    private String factory;

    @Column(name = "po_no", nullable = false, length = 100)
    private String poNo;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) { this.weekStart = weekStart; }

    public String getFactory() { return factory; }
    public void setFactory(String factory) { this.factory = factory; }

    public String getPoNo() { return poNo; }
    public void setPoNo(String poNo) { this.poNo = poNo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class PK implements Serializable {
        private LocalDate weekStart;
        private String factory;

        public PK() {}

        public PK(LocalDate weekStart, String factory) {
            this.weekStart = weekStart;
            this.factory = factory;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK)) return false;
            PK pk = (PK) o;
            return Objects.equals(weekStart, pk.weekStart) && Objects.equals(factory, pk.factory);
        }

        @Override
        public int hashCode() {
            return Objects.hash(weekStart, factory);
        }
    }
}
