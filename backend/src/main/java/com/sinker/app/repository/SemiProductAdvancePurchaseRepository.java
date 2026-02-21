package com.sinker.app.repository;

import com.sinker.app.entity.SemiProductAdvancePurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SemiProductAdvancePurchaseRepository extends JpaRepository<SemiProductAdvancePurchase, Integer> {

    @Modifying
    @Query(value = "TRUNCATE TABLE semi_product_advance_purchase", nativeQuery = true)
    void truncateTable();
}
