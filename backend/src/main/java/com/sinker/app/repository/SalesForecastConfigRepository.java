package com.sinker.app.repository;

import com.sinker.app.entity.SalesForecastConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalesForecastConfigRepository extends JpaRepository<SalesForecastConfig, Integer> {

    Optional<SalesForecastConfig> findByMonth(String month);

    boolean existsByMonth(String month);

    List<SalesForecastConfig> findAllByOrderByMonthDesc();

    List<SalesForecastConfig> findByIsClosedFalseAndAutoCloseDay(Integer autoCloseDay);
}
