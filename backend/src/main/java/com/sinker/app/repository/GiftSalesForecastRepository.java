package com.sinker.app.repository;

import com.sinker.app.entity.GiftSalesForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GiftSalesForecastRepository extends JpaRepository<GiftSalesForecast, Integer> {

    List<GiftSalesForecast> findByMonthAndChannel(String month, String channel);

    @Modifying
    @Query("DELETE FROM GiftSalesForecast g WHERE g.month = :month AND g.channel = :channel")
    void deleteByMonthAndChannel(@Param("month") String month, @Param("channel") String channel);

    @Modifying
    @Query("DELETE FROM GiftSalesForecast g WHERE g.month = :month AND g.channel = :channel AND g.version = :version")
    void deleteByMonthAndChannelAndVersion(@Param("month") String month, @Param("channel") String channel, @Param("version") String version);

    int countByMonthAndChannel(String month, String channel);

    Optional<GiftSalesForecast> findByMonthAndChannelAndProductCode(String month, String channel, String productCode);

    List<GiftSalesForecast> findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(
            String month, String channel, String version);

    @Query("SELECT g FROM GiftSalesForecast g WHERE g.month = :month AND g.channel = :channel " +
           "AND g.version = (SELECT MAX(g2.version) FROM GiftSalesForecast g2 " +
           "WHERE g2.month = :month AND g2.channel = :channel) " +
           "ORDER BY g.category ASC, g.spec ASC, g.productCode ASC")
    List<GiftSalesForecast> findLatestByMonthAndChannel(@Param("month") String month, @Param("channel") String channel);

    @Query("SELECT DISTINCT g.version FROM GiftSalesForecast g " +
           "WHERE g.month = :month AND g.channel = :channel " +
           "ORDER BY g.version DESC")
    List<String> findDistinctVersionsByMonthAndChannel(@Param("month") String month, @Param("channel") String channel);

    @Query("SELECT COUNT(g) FROM GiftSalesForecast g WHERE g.month = :month AND g.channel = :channel AND g.version = :version")
    Integer countByMonthAndChannelAndVersion(@Param("month") String month, @Param("channel") String channel, @Param("version") String version);

    @Query("SELECT MAX(g.updatedAt) FROM GiftSalesForecast g WHERE g.month = :month AND g.channel = :channel AND g.version = :version")
    java.time.LocalDateTime findMaxUpdatedAtByMonthAndChannelAndVersion(@Param("month") String month, @Param("channel") String channel, @Param("version") String version);
}
