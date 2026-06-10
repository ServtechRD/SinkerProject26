package com.sinker.app.repository;

import com.sinker.app.entity.ErpPurchaseOrderRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ErpPurchaseOrderRecordRepository
        extends JpaRepository<ErpPurchaseOrderRecord, ErpPurchaseOrderRecord.PK> {

    Optional<ErpPurchaseOrderRecord> findByWeekStartAndFactory(LocalDate weekStart, String factory);

    void deleteByWeekStartAndFactory(LocalDate weekStart, String factory);
}
