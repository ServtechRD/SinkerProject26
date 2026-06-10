package com.sinker.app.repository;

import com.sinker.app.entity.ErpPurchaseOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ErpPurchaseOrderDetailRepository extends JpaRepository<ErpPurchaseOrderDetail, Long> {

    List<ErpPurchaseOrderDetail> findByWeekStartAndFactory(LocalDate weekStart, String factory);

    void deleteByWeekStartAndFactory(LocalDate weekStart, String factory);
}
