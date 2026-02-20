package com.sinker.app.repository;

import com.sinker.app.entity.SalesForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalesForecastRepository extends JpaRepository<SalesForecast, Integer> {

    List<SalesForecast> findByMonthAndChannel(String month, String channel);

    @Modifying
    @Query("DELETE FROM SalesForecast sf WHERE sf.month = :month AND sf.channel = :channel")
    void deleteByMonthAndChannel(@Param("month") String month, @Param("channel") String channel);

    int countByMonthAndChannel(String month, String channel);

    Optional<SalesForecast> findByMonthAndChannelAndProductCode(String month, String channel, String productCode);
}
