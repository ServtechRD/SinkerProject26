package com.sinker.app.repository;

import com.sinker.app.entity.InventorySalesForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventorySalesForecastRepository extends JpaRepository<InventorySalesForecast, Integer> {

    @Query("SELECT isf FROM InventorySalesForecast isf WHERE isf.month = :month AND isf.version = :version ORDER BY isf.productCode ASC")
    List<InventorySalesForecast> findByMonthAndVersionOrderByProductCodeAsc(
            @Param("month") String month,
            @Param("version") String version);

    @Query("SELECT isf FROM InventorySalesForecast isf WHERE isf.month = :month ORDER BY isf.productCode ASC")
    List<InventorySalesForecast> findByMonthOrderByProductCodeAsc(@Param("month") String month);
}
