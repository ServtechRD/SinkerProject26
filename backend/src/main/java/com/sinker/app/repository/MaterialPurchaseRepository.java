package com.sinker.app.repository;

import com.sinker.app.entity.MaterialPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MaterialPurchaseRepository extends JpaRepository<MaterialPurchase, Integer> {

    List<MaterialPurchase> findByWeekStartAndFactoryOrderByProductCodeAsc(LocalDate weekStart, String factory);
}
