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

    // Query methods for T017
    List<SalesForecast> findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(
            String month, String channel, String version);

    @Query("SELECT sf FROM SalesForecast sf WHERE sf.month = :month AND sf.channel = :channel " +
           "AND sf.version = (SELECT MAX(sf2.version) FROM SalesForecast sf2 " +
           "WHERE sf2.month = :month AND sf2.channel = :channel) " +
           "ORDER BY sf.category ASC, sf.spec ASC, sf.productCode ASC")
    List<SalesForecast> findLatestByMonthAndChannel(@Param("month") String month, @Param("channel") String channel);

    @Query("SELECT DISTINCT sf.version FROM SalesForecast sf " +
           "WHERE sf.month = :month AND sf.channel = :channel " +
           "ORDER BY sf.version DESC")
    List<String> findDistinctVersionsByMonthAndChannel(@Param("month") String month, @Param("channel") String channel);

    @Query("SELECT COUNT(sf) FROM SalesForecast sf WHERE sf.month = :month AND sf.channel = :channel AND sf.version = :version")
    Integer countByMonthAndChannelAndVersion(@Param("month") String month, @Param("channel") String channel, @Param("version") String version);

    @Query("SELECT MAX(sf.updatedAt) FROM SalesForecast sf WHERE sf.month = :month AND sf.channel = :channel AND sf.version = :version")
    java.time.LocalDateTime findMaxUpdatedAtByMonthAndChannelAndVersion(@Param("month") String month, @Param("channel") String channel, @Param("version") String version);
}
