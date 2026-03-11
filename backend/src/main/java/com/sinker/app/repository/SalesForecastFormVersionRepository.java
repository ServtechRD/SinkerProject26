package com.sinker.app.repository;

import com.sinker.app.entity.SalesForecastFormVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalesForecastFormVersionRepository extends JpaRepository<SalesForecastFormVersion, Integer> {

    List<SalesForecastFormVersion> findByMonthOrderByVersionNoDesc(String month);

    long countByMonth(String month);
}
