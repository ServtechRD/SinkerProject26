package com.sinker.app.repository;

import com.sinker.app.entity.SalesForecastReopenWindow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SalesForecastReopenWindowRepository extends JpaRepository<SalesForecastReopenWindow, Integer> {

    /** 該月份＋通路目前有效（未結束且未到期）的補登 */
    Optional<SalesForecastReopenWindow> findFirstByMonthAndChannelAndEndedAtIsNullAndExpiresAtAfter(
            String month, String channel, LocalDateTime now);

    /** 未結束（含已到期但排程尚未處理）的補登，用於阻擋重複開放 */
    boolean existsByMonthAndChannelAndEndedAtIsNull(String month, String channel);

    List<SalesForecastReopenWindow> findByEndedAtIsNullAndExpiresAtLessThanEqual(LocalDateTime now);

    List<SalesForecastReopenWindow> findByMonthAndEndedAtIsNullAndExpiresAtLessThanEqual(String month, LocalDateTime now);

    List<SalesForecastReopenWindow> findByMonthAndEndedAtIsNull(String month);

    long countByMonthAndEndedAtIsNull(String month);
}
